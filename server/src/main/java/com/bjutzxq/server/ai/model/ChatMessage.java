package com.bjutzxq.server.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek/OpenAI 兼容的对话消息
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    /** system / user / assistant / tool */
    private String role;
    private String content;
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    private String name;

    public static ChatMessage system(String content) {
        ChatMessage m = new ChatMessage();
        m.role = "system";
        m.content = content;
        return m;
    }

    public static ChatMessage user(String content) {
        ChatMessage m = new ChatMessage();
        m.role = "user";
        m.content = content;
        return m;
    }

    public static ChatMessage assistant(String content, List<ToolCall> toolCalls) {
        ChatMessage m = new ChatMessage();
        m.role = "assistant";
        m.content = content;
        m.toolCalls = toolCalls;
        return m;
    }

    public static ChatMessage tool(String toolCallId, String name, String content) {
        ChatMessage m = new ChatMessage();
        m.role = "tool";
        m.toolCallId = toolCallId;
        m.name = name;
        m.content = content;
        return m;
    }
}
