package com.bjutzxq.pojo;

import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class RegisterRequest {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 确认密码
     */
    private String confirmPassword;
    
    /**
     * 身份标识号（学生为学号，教师为职工号）
     */
    private String employeeId;
    
    /**
     * 用户角色（USER-学生, TEACHER-教师）
     */
    private String role;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 班级
     */
    private String className;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号（可选）
     */
    private String phone;
    
    /**
     * 性别（可选）
     */
    private String sex;
    
    /**
     * 个人简介（可选）
     */
    private String bio;
    
    /**
     * 验证码会话 ID
     */
    private String captchaSessionId;
    
    /**
     * 用户输入的验证码
     */
    private String captchaCode;
}
