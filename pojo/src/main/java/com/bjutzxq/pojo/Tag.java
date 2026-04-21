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
     * 使用次数
     */
    private Integer usageCount;
}
