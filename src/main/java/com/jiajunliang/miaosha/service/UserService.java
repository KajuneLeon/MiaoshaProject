package com.jiajunliang.miaosha.service;

import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.service.model.UserModel;

/**
 * @project: MiaoshaProject
 * @program: UserService
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-20 23:19
 **/
public interface UserService {
    //通过用户id获取用户对象
    UserModel getUserById(Integer id);
    //用户注册
    void register(UserModel userModel) throws BusinessException;

    /**
     * @param telphone:用户注册手机
     * @param encryptPassword:用户加密后的密码
     * @throws BusinessException
     */
    UserModel validateLogin(String telphone, String encryptPassword) throws BusinessException;

    //通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);
}
