package com.darkecage.dcpicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * @title: 必须具有某个角色
     * @author: darkecage
     * @date: 2025/5/3 0:24
     * @return: java.lang.String
     */
    String mustRole() default "";
}
