package com.bjutzxq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件视图对象（包含上传者信息）
 */
@Data
public class FileVO {
    
    /**
     * 文件 ID
     */
    private Integer id;
    
    /**
     * 项目 ID
     */
    private Integer projectId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件路径（用于目录结构展示）
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件大小（格式化后，如 "1.5 MB"）
     */
    private String formattedSize;
    
    /**
     * 文件类型（如：java, pdf, zip 等）
     */
    private String fileType;
    
    /**
     * 存储 URL（实际存储地址）
     */
    private String storageUrl;
    
    /**
     * 是否目录:0-文件，1-目录
     */
    private Integer isDir;
    
    /**
     * 父目录 ID
     */
    private Integer parentId;
    
    /**
     * 上传者 ID
     */
    private Integer uploaderId;
    
    /**
     * 上传者用户名
     */
    private String uploaderUsername;
    
    /**
     * 上传时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
