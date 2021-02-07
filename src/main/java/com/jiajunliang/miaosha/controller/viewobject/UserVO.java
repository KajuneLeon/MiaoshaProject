package com.jiajunliang.miaosha.controller.viewobject;

import lombok.Data;

/**
 * @project: MiaoshaProject
 * @program: UserVO
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-21 00:01
 **/
@Data
public class UserVO {

    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;

}
