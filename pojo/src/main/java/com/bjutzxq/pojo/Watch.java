package com.bjutzxq.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 关注记录实体类
 */
@Data
public class Watch {
    /**
     * 主键 ID
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
     * 通知类型：1-全部，2-仅重要更新
     */
    private Integer notificationType;
    
    /**
     * 关注时间
     */
    private LocalDateTime createdAt;
}
