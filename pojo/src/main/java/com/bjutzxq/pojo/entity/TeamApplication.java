package com.bjutzxq.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamApplication {
    private Integer id;
    private Integer teamId;
    private Integer applicantId;
    private String message;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 非DB字段
    private String applicantUsername;
    private String applicantAvatar;
    private String teamTitle;
}
