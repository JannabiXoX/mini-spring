package com.lfy.annotation;
import java.lang.annotation.*;
/**
 * @Author:feiyang
 * @Date:7/30/2023 3:11 PM
 */

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    /**
     * Is required.
     */
    boolean value() default true;

    /**
     * Bean name if set.
     */
    String name() default "";
}
