package com.britton.service;

import com.britton.util.Request;

public interface SendService {
    void send(Request paramRequest) throws Exception;

    void kill() throws Exception;
}
