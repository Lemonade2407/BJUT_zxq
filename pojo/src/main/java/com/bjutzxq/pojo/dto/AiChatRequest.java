package com.bjutzxq.pojo.dto;

import lombok.Data;

/**
 * AI 对话请求 DTO
 */
@Data
public class AiChatRequest {
    /** 会话 ID，为空表示新建会话 */
    private Integer conversationId;
    /** 用户消息，最长 2000 字符 */
    private String message;
}
