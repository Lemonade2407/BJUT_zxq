package com.bjutzxq.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 会话实体类
 */
@Data
public class AiConversation {
    private Integer id;
    private Integer userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
