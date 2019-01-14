package com.britton.exceptions;

/**
 * 握手失败异常
 */
public class HandshakeException extends Exception {
    public HandshakeException(String msg) {
        super(msg);
    }
}
