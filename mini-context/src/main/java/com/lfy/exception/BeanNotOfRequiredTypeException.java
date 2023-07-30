package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/30/2023 5:02 PM
 */
public class BeanNotOfRequiredTypeException extends BeansException{
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
