package com.bjutzxq.common;

/**
 * 用户角色枚举
 */
public enum Role {
    /**
     * 普通用户（学生）
     */
    USER(1, "普通用户"),
    
    /**
     * 教师（高级用户）
     */
    TEACHER(2, "教师"),
    
    /**
     * 管理员（超级用户）
     */
    ADMIN(3, "管理员");
    
    private final int code;
    private final String description;
    
    Role(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据 code 获取角色
     * @param code 角色代码
     * @return 角色枚举
     */
    public static Role valueOf(int code) {
        for (Role role : values()) {
            if (role.getCode() == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的角色代码：" + code);
    }
}
