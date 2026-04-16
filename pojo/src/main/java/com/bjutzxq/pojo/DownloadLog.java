package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 下载记录实体类
 */
@Data
public class DownloadLog {
    /**
     * 主键 ID
     */
    private Integer id;
    
    /**
     * 文件 ID
     */
    private Integer fileId;
    
    /**
     * 下载用户 ID
     */
    private Integer userId;
    
    /**
     * 下载 IP
     */
    private String downloadIp;
    
    /**
     * 下载时间
     */
    private LocalDateTime downloadedAt;
}
