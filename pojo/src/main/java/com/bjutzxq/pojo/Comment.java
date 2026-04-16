package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
public class Comment {
    /**
     * 评论 ID
     */
    private Integer id;
    
    /**
     * 评论者 ID
     */
    private Integer userId;
    
    /**
     * 项目 ID
     */
    private Integer projectId;
    
    /**
     * 父评论 ID（用于回复）
     */
    private Integer parentId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 回复数
     */
    private Integer replyCount;
    
    /**
     * 评论时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 状态：0-删除，1-显示
     */
    private Integer status;
}
