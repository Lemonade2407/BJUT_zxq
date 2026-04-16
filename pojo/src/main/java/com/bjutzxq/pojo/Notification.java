package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体类
 */
@Data
public class Notification {
    /**
     * 主键 ID
     */
    private Integer id;
    
    /**
     * 接收用户 ID
     */
    private Integer userId;
    
    /**
     * 发送用户 ID
     */
    private Integer senderId;
    
    /**
     * 相关项目 ID
     */
    private Integer projectId;
    
    /**
     * 通知类型:1-点赞，2-评论，3-关注等
     */
    private Integer type;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 是否已读:0-未读，1-已读
     */
    private Integer isRead;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
