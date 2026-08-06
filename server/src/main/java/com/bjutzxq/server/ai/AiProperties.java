package com.bjutzxq.server.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 助手配置（ai.deepseek.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private Duration timeout = Duration.ofSeconds(120);
    private int maxToolRounds = 6;
    private int historyLimit = 40;
    private int toolResultLimit = 3000;
}
