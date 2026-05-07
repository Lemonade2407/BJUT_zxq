package com.bjutzxq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户视图对象 VO（用于展示层，排除敏感字段）
 */
@Data
public class UserVO {
    
    /**
     * 用户 ID
     */
    private Integer id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 身份标识号（学号/职工号）
     */
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
    private String email;
    
    /**
     * 头像 URL
     */
    private String avatar;
    
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
     * 状态
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // 注意：不包含 password 字段，保证安全性
}
