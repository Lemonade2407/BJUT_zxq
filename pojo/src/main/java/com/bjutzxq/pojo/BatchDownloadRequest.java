package com.bjutzxq.pojo;

import lombok.Data;
import java.util.List;

/**
 * 批量下载请求 DTO
 */
@Data
public class BatchDownloadRequest {
    /**
     * 项目 ID 列表
     */
    private List<Integer> projectIds;
    
    /**
     * 班级名称
     */
    private String className;
    
    /**
     * 课程名称
     */
    private String courseName;
}
