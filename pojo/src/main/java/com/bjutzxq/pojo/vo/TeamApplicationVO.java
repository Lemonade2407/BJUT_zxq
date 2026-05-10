package com.bjutzxq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamApplicationVO {
    private Integer id;
    private Integer teamId;
    private Integer applicantId;
    private String applicantUsername;
    private String applicantAvatar;
    private String teamTitle;
    private String message;
    private Integer status;
    private String statusText;
    private LocalDateTime createdAt;
}
