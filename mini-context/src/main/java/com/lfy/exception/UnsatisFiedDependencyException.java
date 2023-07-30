package com.lfy.exception;

/**
 * @Author:feiyang
 * @Date:7/30/2023 4:01 PM
 */
public class UnsatisfiedDependencyException extends BeanCreationException{

    public UnsatisfiedDependencyException() {
    }

    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsatisfiedDependencyException(Throwable cause) {
        super(cause);
    }
}
