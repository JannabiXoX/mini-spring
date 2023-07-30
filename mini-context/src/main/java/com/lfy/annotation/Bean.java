package com.lfy.annotation;

import java.lang.annotation.*;

/**
 * @Author:feiyang
 * @Date:7/29/2023 11:06 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Bean {

    String value() default "";

    String initMethod() default "";

    String destroyMethod() default "";
}
