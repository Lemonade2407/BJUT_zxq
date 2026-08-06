package com.bjutzxq.server.ai.model;

import lombok.Data;

import java.util.Map;

/**
 * 工具定义（function calling schema）
 * OpenAI/DeepSeek 兼容格式：type + 嵌套的 function{name, description, parameters}
 */
@Data
public class ToolDefinition {
    private String type = "function";
    private ToolFunction function;

    @Data
    public static class ToolFunction {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}
