package com.jiajunliang.miaosha.controller;

import com.alibaba.druid.util.StringUtils;
import com.jiajunliang.miaosha.controller.viewobject.UserVO;
import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.error.EmBusinessError;
import com.jiajunliang.miaosha.response.CommonReturnType;
import com.jiajunliang.miaosha.service.UserService;
import com.jiajunliang.miaosha.service.model.UserModel;
import com.jiajunliang.miaosha.validator.ValidatorImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Base64.Encoder;
import java.util.concurrent.TimeUnit;

/**
 * @project: MiaoshaProject
 * @program: UserController
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-20 23:14
 **/
@Slf4j
@Controller
@RequestMapping("/user")
//坑：如果没有添加originPatterns，会报错：
//When allowCredentials is true, allowedOrigins cannot contain the special value "*"since that
//cannot be set on the "Access-Control-Allow-Origin" response header. To allow credentials to
//a set of origins, list them explicitly or consider using "allowedOriginPatterns" instead.
//allowCredentials=true需要配合前端设置xhrFields授信，使得跨域session共享
@CrossOrigin(allowCredentials="true", allowedHeaders="*", originPatterns="*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    //这个HttpServletRequest本质是一个proxy增强类，底层有一个关于ThreadLocal的map，用于多线程处理
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;


    //用户登录接口
    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name="telphone")String telphone,
                                  @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if(StringUtils.isEmpty(telphone)||StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务，校验登录是否合法
        UserModel userModel = userService.validateLogin(telphone,encodeByMd5(password));

        //将登录凭证加入到用户登陆成功的session内 - Cookie方式
        //httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        //httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);

        //从cookie方式修改为token方式：用户登录验证成功后，将对应的登录信息和登录凭证一起存入redis中
        //生成登录凭证token - UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken.replace("-","");
        //建立token与用户登录态之间的联系
        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);
        //下发token
        return CommonReturnType.create(uuidToken);
    }


    //用户注册接口
    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name="telphone")String telphone,
                                     @RequestParam("otpCode")String otpCode,
                                     @RequestParam("name")String name,
                                     @RequestParam("gender")Integer gender,
                                     @RequestParam("age")Integer age,
                                     @RequestParam("password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //短信验证码匹配
        String inSessionOtpCode = (String) httpServletRequest.getSession().getAttribute(telphone);
        if(!StringUtils.equals(otpCode,inSessionOtpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不正确");
        }
        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(Byte.valueOf(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncryptPassword(encodeByMd5(password));

        userService.register(userModel);

        return CommonReturnType.create(null);
    }

    public String encodeByMd5(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        Encoder encoder = Base64.getEncoder();
        String newStr = encoder.encodeToString(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    //用户获取opt短信接口
    @RequestMapping(value = "/getotp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone")String telphone){
        //1. 按照一定的规则生成otp验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999) + 10000;
        String otpCode = String.valueOf(randomInt);

        //2. 将otp验证码同对应用户的手机号管理
        //使用HttpSession的方式绑定手机号与otp验证码
        httpServletRequest.getSession().setAttribute(telphone, otpCode);

        //3. 将otp验证码通过短信通道发送给用户，在此省略
        log.info("telphone = " + telphone + " & otpCode = " + otpCode);

        return CommonReturnType.create(null);
    }

    /**
     * 返回UserVO的原因：Model是一个全字段的模型，包含了相应角色的所有数据库映射信息，
     * 若直接返回一个Model到前端用户，则会透传给很多不必要的信息，如加密的密码...
     * 所以一般在controller包下新建一个viewobject包，在这里面将所需信息封装为一个VO类，如UserVO
     */
    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name="id")Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回前端
        UserModel userModel = userService.getUserById(id);

        //若获取的对应用户信息不存在
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域模型用户对象转化为可供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);
        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }



}
