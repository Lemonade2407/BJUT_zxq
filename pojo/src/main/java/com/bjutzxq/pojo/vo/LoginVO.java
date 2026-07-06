package com.bjutzxq.pojo.vo;

import lombok.Data;

/**
 * 登录视图对象 VO（用于展示层，封装认证信息）
 */
@Data
public class LoginVO {
    
    /**
     * 用户 ID
     */
    private Integer id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 身份标识号（学号/职工号）
     */
    private String employeeId;
    
    /**
     * 角色
     */
    private String role;
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * Token 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 是否需要强制修改密码（密码为弱密码或旧数据）
     */
    private Boolean mustChangePassword;
}
