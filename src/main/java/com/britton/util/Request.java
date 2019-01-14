package com.britton.util;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class Request {
    @NotNull
    @NotEmpty
    @Min(1001)
    @Max(1006)
    private int eventId;
    @NotNull
    @NotEmpty
    private String requestId;
    @NotNull
    @NotEmpty
    private String senderType;
    @NotNull
    @NotEmpty
    private String receiverId;
    @NotNull
    private String receiverType;
    @NotNull
    @NotEmpty
    private String userId;
    @NotNull
    @NotEmpty
    private String object;

    public static Request create(String json) {
        if (!StringUtils.isEmpty(json)) {
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
