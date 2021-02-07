package com.jiajunliang.miaosha.error;

public enum EmBusinessError implements CommonError{
    // 1000X为通用错误类型
    PARAMETER_VALIDATION_ERROR(10001, "非法参数"),
    UNKNOWN_ERROR(10002, "未知错误"),

    // 2000X为用户信息相关错误
    USER_NOT_EXIST(20001, "用户不存在"),
    USER_LOGIN_FAIL(20002, "用户手机号码或密码不正确"),
    USER_NOT_LOGIN(20003,"用户未登录"),

    // 3000X为交易信息相关错误
    STOCK_NOT_ENOUGH(30001, "库存不足");

    private int errCode;
    private String errMsg;

    private EmBusinessError(int errCode, String errMsg){
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    // 用于通用错误码，修改其中的errMsg
    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
