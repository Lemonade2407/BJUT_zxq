package com.bjutzxq.server.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 工具调用（流式响应/请求消息均复用）
 */
@Data
public class ToolCall {
    /** 仅流式增量中按 index 拼装；发送给上游的 assistant.tool_calls 中不序列化 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer index;
    private String id;
    private String type;
    private ToolCallFunction function;

    @Data
    public static class ToolCallFunction {
        private String name;
        private String arguments;
    }

    public String getName() {
        return function != null ? function.getName() : null;
    }

    public String getArguments() {
        return function != null ? function.getArguments() : null;
    }

    /** 流式下 arguments 分片到达，按 index 累加 */
    public void appendArguments(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        if (function == null) {
            function = new ToolCallFunction();
        }
        String cur = function.getArguments();
        function.setArguments(cur == null ? chunk : cur + chunk);
    }
}
