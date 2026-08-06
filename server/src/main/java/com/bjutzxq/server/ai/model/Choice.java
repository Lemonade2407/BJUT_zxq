package com.bjutzxq.server.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 流式分片中的选择项
 */
@Data
public class Choice {
    private Integer index;
    private Delta delta;
    private ChatMessage message;
    @JsonProperty("finish_reason")
    private String finishReason;
}
