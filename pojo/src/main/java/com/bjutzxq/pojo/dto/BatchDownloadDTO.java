package com.bjutzxq.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量下载请求 DTO
 */
@Data
public class BatchDownloadDTO {
    /**
     * 项目 ID 列表
     */
    @NotEmpty(message = "项目 ID 列表不能为空")
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
