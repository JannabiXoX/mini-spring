package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/29/2023 8:55 PM
 */
public class NestedRuntimeException extends RuntimeException{
    public NestedRuntimeException() {
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
