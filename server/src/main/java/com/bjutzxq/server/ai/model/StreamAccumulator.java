package com.bjutzxq.server.ai.model;

import lombok.Data;

import java.util.List;

/**
 * 一轮流式调用的累积结果
 */
@Data
public class StreamAccumulator {
    private StringBuilder content = new StringBuilder();
    private List<ToolCall> toolCalls;
    private String finishReason;
}
