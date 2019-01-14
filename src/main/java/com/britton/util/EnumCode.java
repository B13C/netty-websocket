package com.britton.util;

public enum EnumCode {
    SUCCESS(200, "成功"),
    ON_LINE(1001, "客户端上线请求"),
    SEND_MESSAGE(1002, "客户端发送'发送消息'请求"),
    RECEIVE_MESSAGE(1003, "服务端发送'接收消息'请求"),
    DOWN_LINE(1004, "客户端下线请求"),
    HEART_SEND(1005, "心跳包"),
    API_SEND(1006, "API");

    private String msg;
    private Integer code;

    EnumCode(Integer code, String msg) {
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getCode() {
        return code;
    }
}
