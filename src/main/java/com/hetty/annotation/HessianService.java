package com.hetty.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

@Target({ java.lang.annotation.ElementType.TYPE })  
@Retention(RetentionPolicy.RUNTIME)  
@Documented  
public @interface HessianService {
    public abstract String value() default "";
    
    public abstract String version() default "1";
}