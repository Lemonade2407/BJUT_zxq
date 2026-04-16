package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目标签关联实体类
 */
@Data
public class ProjectTag {
    /**
     * 项目 ID
     */
    private Integer projectId;
    
    /**
     * 标签 ID
     */
    private Integer tagId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
