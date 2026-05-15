package com.demo.notification.domain.exception;
public class ChannelUnavailableException extends RuntimeException {
    public ChannelUnavailableException(String message) { super(message); }
    public ChannelUnavailableException(String message, Throwable cause) { super(message, cause); }
}
