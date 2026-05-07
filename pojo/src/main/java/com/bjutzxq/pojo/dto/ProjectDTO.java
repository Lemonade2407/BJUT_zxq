package com.bjutzxq.pojo.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

/**
 * 项目请求 DTO（创建和更新共用）
 */
@Data
public class ProjectDTO {
    
    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    @Size(min = 2, max = 100, message = "项目名称长度应为 2-100 位")
    private String name;
    
    /**
     * 项目描述
     */
    @Size(max = 2000, message = "项目描述不能超过 2000 字")
    private String description;
    
    /**
     * 可见性：0-私有，1-公开
     */
    @NotNull(message = "可见性不能为空")
    @Min(value = 0, message = "可见性参数错误")
    @Max(value = 1, message = "可见性参数错误")
    private Integer visibility;
    
    /**
     * 项目类型: COURSE-课程设计, THESIS-毕业设计, COMPETITION-竞赛作品, PERSONAL-个人项目, OTHER-其他
     */
    private String projectType;
    
    /**
     * 课程名称（仅当 project_type=COURSE 时有效）
     */
    private String courseName;
    
    /**
     * 毕设类型: UNDERGRADUATE-本科, MASTER-硕士, DOCTOR-博士（仅当 project_type=THESIS 时有效）
     */
    private String thesisType;
    
    /**
     * 标签 ID 列表
     */
    private List<Integer> tagIds;
}
