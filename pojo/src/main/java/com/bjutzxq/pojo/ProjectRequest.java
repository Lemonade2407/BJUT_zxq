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
     * 项目类型: COURSE-课程设计, THESIS-毕业设计, COMPETITION-竞赛作品, PERSONAL-个人项目, OTHER-其他
     */
    private String projectType;
    
    /**
     * 课程名称（仅当项目类型为课程设计时使用）
     */
    private String courseName;
    
    /**
     * 毕设类型: UNDERGRADUATE-本科, MASTER-硕士, DOCTOR-博士（仅当项目类型为毕业设计时使用）
     */
    private String thesisType;
    
    /**
     * 标签 ID 列表
     */
    private List<Integer> tagIds;
}
