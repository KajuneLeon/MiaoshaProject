package com.jiajunliang.miaosha.config;

import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;

/**
 * @project: MiaoshaProject
 * @program: RedisConfig
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-11 22:15
 **/
@Component
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {

}
