package com.lfy.annotation;

import java.lang.annotation.*;

/**
 * @Author:feiyang
 * @Date:7/29/2023 10:41 PM
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
}
