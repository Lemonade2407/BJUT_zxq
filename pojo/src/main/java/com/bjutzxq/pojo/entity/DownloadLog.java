package com.bjutzxq.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 下载日志实体类
 */
@Data
public class DownloadLog {
    /**
     * 日志 ID
     */
    private Integer id;
    
    /**
     * 用户 ID
     */
    private Integer userId;
    
    /**
     * 项目 ID
     */
    private Integer projectId;
    
    /**
     * 文件 ID
     */
    private Integer fileId;
    
    /**
     * IP 地址
     */
    private String ipAddress;
    
    /**
     * 下载时间
     */
    private LocalDateTime createdAt;
}
