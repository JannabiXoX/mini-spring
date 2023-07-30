package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/30/2023 6:02 PM
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException{
    public NoSuchBeanDefinitionException(){

    }

    public NoSuchBeanDefinitionException(String message){
        super(message);
    }
}
