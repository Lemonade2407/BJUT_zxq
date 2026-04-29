package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目实体类
 */
@Data
public class Project {
    /**
     * 项目 ID
     */
    private Integer id;
    
    /**
     * 项目名称
     */
    private String name;
    
    /**
     * 项目描述
     */
    private String description;
    
    /**
     * 项目所有者 ID
     */
    private Integer ownerId;
    
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
     * 可见性:0-私有，1-公开
     */
    private Integer visibility;
    
    /**
     * 点赞数
     */
    private Integer starCount;
    
    /**
     * 关注数
     */
    private Integer watchCount;
    
    /**
     * 文件数量
     */
    private Integer fileCount;
    
    /**
     * 下载数
     */
    private Integer downloadCount;
    
    /**
     * 浏览数
     */
    private Integer viewCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 标签列表（非数据库字段）
     */
    private List<Tag> tags;
    
    /**
     * 当前用户是否已点赞（非数据库字段）
     */
    private Boolean isLiked;
    
    /**
     * 当前用户是否已收藏（非数据库字段）
     */
    private Boolean isFavorited;
    
    /**
     * 点赞数别名（用于前端显示，对应 starCount）
     */
    private Integer likes;
    
    /**
     * 收藏数别名（用于前端显示，对应 watchCount）
     */
    private Integer favorites;
    
    /**
     * 作者名称（非数据库字段，用于前端显示）
     */
    private String author;
}
