package com.bjutzxq.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterDTO {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度应为 2-20 位")
    private String username;
    
    /**
     * 密码（至少 8 位，含大小写字母和数字）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度应为 8-32 位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码必须包含大小写字母和数字")
    private String password;
    
    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    /**
     * 身份标识号（学号/职工号）
     */
    @NotBlank(message = "身份标识号不能为空")
    private String employeeId;
    
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
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 性别
     */
    private String sex;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 角色
     */
    private String role;
    
    /**
     * 验证码会话 ID
     */
    @NotBlank(message = "验证码会话 ID 不能为空")
    private String captchaSessionId;
    
    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
