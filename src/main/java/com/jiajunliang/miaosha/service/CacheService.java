package com.jiajunliang.miaosha.service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @project: MiaoshaProject
 * @program: CacheService
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-16 21:35
 **/
//封装本地缓存操作类
public interface CacheService {
    //存方法
    void setCommonCache(String key, Object value);

    //取方法
    Object getFromCommonCache(String key);
}
