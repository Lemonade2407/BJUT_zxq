package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程字典实体类
 */
@Data
public class Course {
    /**
     * 课程ID
     */
    private Integer id;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
