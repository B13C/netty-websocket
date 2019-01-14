package com.britton.util;

import com.alibaba.fastjson.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.Data;

@Data
public class Request {
    private int eventId;
    private String requestId;
    private String senderType;
    private String receiverId;
    private String receiverType;
    private String userId;
    private String message;

    public static Request create(String json) {
        if (!StringUtil.isNullOrEmpty(json)) {
            return JSONObject.parseObject(json, Request.class);
        }
        return null;
    }

    public String toJson() {
        return JSONObject.toJSONString(this);
    }

    public String toString() {
        return toJson();
    }
}
