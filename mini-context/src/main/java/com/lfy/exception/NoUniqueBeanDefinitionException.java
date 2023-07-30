package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/29/2023 8:53 PM
 */
public class NoUniqueBeanDefinitionException extends BeanDefinitionException{

    public NoUniqueBeanDefinitionException() {
    }

    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}
