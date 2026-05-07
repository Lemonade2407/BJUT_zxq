package com.bjutzxq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论视图对象（包含用户信息）
 */
@Data
public class CommentVO {
    
    /**
     * 评论 ID
     */
    private Integer id;
    
    /**
     * 用户 ID
     */
    private Integer userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 项目 ID
     */
    private Integer projectId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 状态（0-已删除, 1-正常）
     */
    private Integer status;
}
