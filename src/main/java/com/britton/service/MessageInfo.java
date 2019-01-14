package com.britton.service;

import lombok.Data;

@Data
public class MessageInfo {
    private String userId;

    private String senderType;

    private String requestId;

    private SendService sendService;

    public MessageInfo(String userId, String senderType, String requestId, SendService sendService) {
        this.userId = userId;
        this.senderType = senderType;
        this.requestId = requestId;
        this.sendService = sendService;
    }
    
    public String toString() {
        return "MessageInfo{userId='" + this.userId + '\'' + ", senderType='" + this.senderType + '\'' + ", requestId='" + this.requestId + '\'' + '}';
    }
}
