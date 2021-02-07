package com.jiajunliang.miaosha.controller;

import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.error.EmBusinessError;
import com.jiajunliang.miaosha.response.CommonReturnType;
import com.jiajunliang.miaosha.service.OrderService;
import com.jiajunliang.miaosha.service.model.OrderModel;
import com.jiajunliang.miaosha.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
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

    //封装下单请求
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId", required = false) Integer promoId) throws BusinessException {

        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(isLogin == null || !isLogin.booleanValue()) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，不能下单");
        }
        //获取用户登录信息
        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        //获取用户的登录信息
        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);

        return CommonReturnType.create(null);
    }
}
