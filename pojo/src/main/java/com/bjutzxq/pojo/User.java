package com.bjutzxq.pojo;

import com.bjutzxq.common.Role;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {
    /**
     * 用户 ID
     */
    private Integer id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码 (加密)
     */
    private String password;
    
    /**
     * 学号
     */
    private String studentId;
    
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
     * 注册时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 状态:0-禁用，1-正常
     */
    private Integer status;
    
    /**
     * 用户角色
     */
    private Role role;
}
