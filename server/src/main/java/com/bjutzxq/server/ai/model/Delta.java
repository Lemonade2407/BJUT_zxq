package com.bjutzxq.server.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 流式增量
 */
@Data
public class Delta {
    private String role;
    private String content;
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
}
