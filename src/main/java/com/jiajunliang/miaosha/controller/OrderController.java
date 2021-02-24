package com.jiajunliang.miaosha.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.error.EmBusinessError;
import com.jiajunliang.miaosha.mq.MqProducer;
import com.jiajunliang.miaosha.response.CommonReturnType;
import com.jiajunliang.miaosha.service.ItemService;
import com.jiajunliang.miaosha.service.OrderService;
import com.jiajunliang.miaosha.service.PromoService;
import com.jiajunliang.miaosha.service.model.OrderModel;
import com.jiajunliang.miaosha.service.model.UserModel;
import com.jiajunliang.miaosha.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @project: MiaoshaProject
 * @program: OrderController
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-06 18:04
 **/
@Controller
@RequestMapping("/order")
//@CrossOrigin(origins = {"*"},allowCredentials = "true")
@CrossOrigin(allowCredentials="true", allowedHeaders="*", originPatterns="*")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(300);
    }

    //生成验证码
    @RequestMapping(value = "/generateverifycode", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能生成验证码");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能生成验证码");
        }
        //生成验证码
        Map<String,Object> map = CodeUtil.generateCodeAndPic();
        //将验证码code与用户做绑定存入redis中
        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"));
        redisTemplate.expire("verify_code_" + userModel.getId(), 10, TimeUnit.MINUTES);
        //将生成的验证码通过HttpResponse的输出流返回前端
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());
    }


    //封装下单请求
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId", required = false) Integer promoId,
                                        @RequestParam(name = "promoToken", required = false) String promoToken) throws BusinessException {

        //限流：尝试获取一个Guava RateLimiter的令牌，若获取失败就返回false
        if(!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EmBusinessError.RATE_LIMIT);
        }

        //使用token判断登录态,获取用户登录信息(token附于请求url的?参数后)
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }

        //校验秒杀令牌是否正确
        if(promoId != null) {
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userid_" + userModel.getId() + "_itemid_" + itemId);
            if(inRedisPromoToken == null || !StringUtils.equals(promoToken, inRedisPromoToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        //判断库存是否已售罄，若对应的售罄key存在，则直接放回下单失败
        if(redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //队列泄洪：同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用于队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入一条init状态的库存流水，用于追踪异步扣减库存的消息
                String stockLogId = itemService.initStockLog(itemId, amount);
                //发送事务型消息，由事务型消息驱动创建订单，根据回调状态确定消息发送状态
                if(!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)){
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"下单失败");
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);
    }

    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name = "itemId") Integer itemId,
                                          @RequestParam(name = "promoId") Integer promoId,
                                          @RequestParam(name = "verifyCode") String verifyCode
                                          ) throws BusinessException {
        //判断登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }
        //判断用户信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }

        //通过verifyCode确定验证码的有效性
        String redisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if(StringUtils.isEmpty(redisVerifyCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请求非法");
        }
        if(!redisVerifyCode.equalsIgnoreCase(verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请求非法，验证码错误");
        }

        //获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if(result < 0) {
            return null;
        }

        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());

        if(promoToken == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败");
        }

        return CommonReturnType.create(promoToken);
    }
}
