package com.bjutzxq.server.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 聊天补全请求
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private List<ToolDefinition> tools;
    private boolean stream;
    private Double temperature;
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    /** 可选：当前用户 ID，用于 DeepSeek 侧日志/鉴权上下文 */
    @JsonProperty("user")
    private String user;
}
