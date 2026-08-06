package com.bjutzxq.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 会话消息实体类
 */
@Data
public class AiMessage {
    private Integer id;
    private Integer conversationId;
    /** 角色: system / user / assistant / tool */
    private String role;
    private String content;
    /** assistant 消息的工具调用 JSON 数组字符串 */
    private String toolCalls;
    /** tool 消息对应的调用 ID */
    private String toolCallId;
    private LocalDateTime createdAt;
}
