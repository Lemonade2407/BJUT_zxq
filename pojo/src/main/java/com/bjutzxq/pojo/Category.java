package com.bjutzxq.pojo;

import lombok.Data;

/**
 * 分类实体类
 */
@Data
public class Category {
    /**
     * 分类 ID
     */
    private Integer id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 父分类 ID
     */
    private Integer parentId;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
