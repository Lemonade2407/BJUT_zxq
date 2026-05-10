package com.bjutzxq.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 组队实体类
 */
@Data
public class Team {
    private Integer id;
    private Integer userId;
    private String title;
    private String description;
    private Integer currentMembers;
    private Integer neededMembers;
    private String tag;
    private String courseName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非数据库字段，用于VO填充
    private String creatorUsername;
    private String creatorAvatar;
}
