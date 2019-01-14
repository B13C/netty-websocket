package com.britton.util;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class Response {
    private String requestId;
    private int eventId;
    private boolean status;
    private String message;

    public String toJson() {
        return JSONObject.toJSONString(this);
    }

    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
