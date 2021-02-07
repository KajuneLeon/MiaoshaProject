package com.jiajunliang.miaosha.service.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @project: MiaoshaProject
 * @program: UserModel
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-20 23:31
 **/
@Data
public class UserModel {

    private Integer id;
    @NotBlank(message = "用户名不能为空")
    private String name;
    @NotNull(message = "性别不能为空")
    private Byte gender;
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄必须大于0岁")
    @Max(value = 150, message = "年龄必须小于150岁")
    private Integer age;
    @NotBlank(message = "手机号不能为空")
    private String telphone;
    private String registerMode;
    private String thirdPartyId;
    @NotBlank(message = "密码不能为空")
    private String encryptPassword;

}
