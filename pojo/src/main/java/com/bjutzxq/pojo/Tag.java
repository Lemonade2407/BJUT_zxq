package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 标签实体类
 */
@Data
public class Tag {
    /**
     * 标签 ID
     */
    private Integer id;
    
    /**
     * 标签名称
     */
    private String name;
    
    /**
     * 标签分组：技术栈、领域、其他
     */
    private String category;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
}
