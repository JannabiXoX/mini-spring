package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/29/2023 8:54 PM
 */
public class BeanDefinitionException extends BeansException{

    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }
}
