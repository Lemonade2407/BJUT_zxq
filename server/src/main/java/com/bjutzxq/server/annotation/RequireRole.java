package com.bjutzxq.server.annotation;

import com.bjutzxq.common.Role;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要特定角色才能访问的注解
 */
//用于在方法上的注释
@Target(ElementType.METHOD)
//保留到运行时
@Retention(RetentionPolicy.RUNTIME)
//生成文档
@Documented
//@interface用来定义注解；interface用来定义接口
public @interface RequireRole {
    
    /**
     * 需要的角色（支持多个）
     * @return 角色数组
     */
    Role[] value();
    
    /**
     * 错误提示信息
     * @return 提示消息
     */
    String message() default "权限不足";
}
