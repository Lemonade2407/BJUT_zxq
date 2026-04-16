package com.bjutzxq.pojo;

import lombok.Data;
import java.util.List;

/**
 * 项目创建/更新请求 DTO
 */
@Data
public class ProjectRequest {
    /**
     * 项目名称
     */
    private String name;
    
    /**
     * 项目描述
     */
    private String description;
    
    /**
     * 可见性:0-私有，1-公开
     */
    private Integer visibility;
    
    /**
     * 标签 ID 列表
     */
    private List<Integer> tagIds;
}
