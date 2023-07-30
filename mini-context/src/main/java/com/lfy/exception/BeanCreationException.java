package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/29/2023 10:02 PM
 */
public class BeanCreationException extends BeansException{

    public BeanCreationException() {
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}
