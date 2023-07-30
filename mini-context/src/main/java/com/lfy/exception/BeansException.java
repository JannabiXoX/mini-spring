package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/29/2023 8:54 PM
 */
public class BeansException extends NestedRuntimeException{

    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
