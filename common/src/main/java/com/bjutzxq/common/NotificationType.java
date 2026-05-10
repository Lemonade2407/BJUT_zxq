package com.bjutzxq.common;

import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
public enum NotificationType {
    /**
     * 点赞通知
     */
    LIKE(1, "点赞"),
    
    /**
     * 评论通知
     */
    COMMENT(2, "评论"),
    
    /**
     * 关注通知
     */
    WATCH(3, "关注"),
    
    /**
     * 系统通知
     */
    SYSTEM(4, "系统通知"),

    /**
     * 组队申请通知
     */
    TEAM_APPLICATION(5, "组队申请");
    
    private final int code;
    private final String description;
    
    NotificationType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 获取通知类型
     * @param code 类型代码
     * @return 通知类型枚举
     */
    public static NotificationType valueOf(int code) {
        for (NotificationType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的通知类型代码：" + code);
    }
}
