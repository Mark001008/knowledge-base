package com.ma.kb.core.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 标注在Controller方法上，表示需要特定权限才能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 需要的权限编码
     */
    String value();

    /**
     * 是否需要所有权限（默认只需要任意一个）
     */
    boolean requireAll() default false;
}
