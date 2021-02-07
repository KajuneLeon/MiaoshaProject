package com.jiajunliang.miaosha.controller;

import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.error.EmBusinessError;
import com.jiajunliang.miaosha.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @project: MiaoshaProject
 * @program: BaseController
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-21 23:16
 **/
public class BaseController {

    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

//    //定义ExceptionHandler解决未被controller层吸收的exception
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.OK)
//    @ResponseBody
//    public Object handlerException(HttpServletRequest request, Exception exception){
//        Map<String, Object> responseData = new HashMap<>();
//        if(exception instanceof BusinessException){
//            BusinessException businessException = (BusinessException) exception;
//            responseData.put("errCode", businessException.getErrCode());
//            responseData.put("errMsg", businessException.getErrMsg());
//        } else {
//            responseData.put("errCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
//            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
//        }
//        return CommonReturnType.create(responseData, "fail");
//    }
}
