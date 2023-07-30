package com.lfy.annotation;

import java.lang.annotation.*;

/**
 * @Author:feiyang
 * @Date:7/30/2023 3:13 PM
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Value {
    String value();
}
