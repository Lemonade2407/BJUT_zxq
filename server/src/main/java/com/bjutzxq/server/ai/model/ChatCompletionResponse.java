package com.bjutzxq.server.ai.model;

import lombok.Data;

import java.util.List;

/**
 * 聊天补全响应（流式分片与完整响应共用结构）
 */
@Data
public class ChatCompletionResponse {
    private String id;
    private List<Choice> choices;
    private String object;
    private Long created;
}
