package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞记录实体类
 */
@Data
public class Star {
    /**
     * 主键 ID
     */
    private Integer id;
    
    /**
     * 用户 ID
     */
    private Integer userId;
    
    /**
     * 项目 ID
     */
    private Integer projectId;
    
    /**
     * 点赞时间
     */
    private LocalDateTime createdAt;
}
