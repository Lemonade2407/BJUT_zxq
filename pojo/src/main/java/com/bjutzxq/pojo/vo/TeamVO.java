package com.bjutzxq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 组队视图对象
 */
@Data
public class TeamVO {
    private Integer id;
    private Integer userId;
    private String creatorUsername;
    private String creatorAvatar;
    private String title;
    private String description;
    private Integer currentMembers;
    private Integer neededMembers;
    private String tag;
    private String courseName;
    private Integer status;
    private String statusText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
