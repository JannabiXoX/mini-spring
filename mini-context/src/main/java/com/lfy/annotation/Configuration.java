package com.lfy.annotation;

import java.lang.annotation.*;

/**
 * @Author:feiyang
 * @Date:7/29/2023 11:01 PM
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configuration {
    String value() default "";
}
