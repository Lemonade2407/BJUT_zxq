package com.bjutzxq.pojo.dto;

import lombok.Data;
import java.util.List;

/**
 * 通用分页响应 DTO
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> {
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页数量
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 构造函数
     * @param records 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     */
    public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }
}
