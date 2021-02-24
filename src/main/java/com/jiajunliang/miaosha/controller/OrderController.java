package com.jiajunliang.miaosha.controller;

import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.error.EmBusinessError;
import com.jiajunliang.miaosha.mq.MqProducer;
import com.jiajunliang.miaosha.response.CommonReturnType;
import com.jiajunliang.miaosha.service.ItemService;
import com.jiajunliang.miaosha.service.OrderService;
import com.jiajunliang.miaosha.service.PromoService;
import com.jiajunliang.miaosha.service.model.OrderModel;
import com.jiajunliang.miaosha.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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


    //封装下单请求
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId", required = false) Integer promoId,
                                        @RequestParam(name = "promoToken", required = false) String promoToken) throws BusinessException {


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

        //加入一条init状态的库存流水，用于追踪异步扣减库存的消息
        String stockLogId = itemService.initStockLog(itemId, amount);

        //发送事务型消息，由事务型消息驱动创建订单，根据回调状态确定消息发送状态
        if(!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"下单失败");
        }
        return CommonReturnType.create(null);
    }

    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name = "itemId") Integer itemId,
                                          @RequestParam(name = "promoId") Integer promoId) throws BusinessException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }
        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());

        if(promoToken == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败");
        }

        return CommonReturnType.create(promoToken);
    }
}
