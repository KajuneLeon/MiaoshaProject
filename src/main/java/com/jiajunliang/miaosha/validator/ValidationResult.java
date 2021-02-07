package com.jiajunliang.miaosha.validator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @project: MiaoshaProject
 * @program: ValidationResult
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-30 22:41
 **/

/**
 * 在应用程序与validator逻辑校验之间，提供对接功能
 */
@Data
public class ValidationResult {

    //校验结果是否有错
    private boolean hasErrors = false;
    //存放错误信息的map
    private Map<String, String> errorMsgMap = new HashMap<>();

    public boolean isError(){
        return hasErrors;
    }

    //实现通用的通过格式化字符串信息获取错误结果的msg方法
    public String getErrMsg(){
        return StringUtils.join(errorMsgMap.values().toArray(), ",");
    }
}
