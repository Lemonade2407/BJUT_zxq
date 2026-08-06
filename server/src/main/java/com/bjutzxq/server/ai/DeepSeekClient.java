package com.bjutzxq.server.ai;

import com.bjutzxq.server.ai.model.ChatCompletionRequest;
import com.bjutzxq.server.ai.model.ChatCompletionResponse;
import com.bjutzxq.server.ai.model.ChatMessage;
import com.bjutzxq.server.ai.model.Choice;
import com.bjutzxq.server.ai.model.Delta;
import com.bjutzxq.server.ai.model.StreamAccumulator;
import com.bjutzxq.server.ai.model.ToolCall;
import com.bjutzxq.server.ai.model.ToolDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek 薄客户端：OpenAI 兼容 chat/completions，支持流式与 function calling
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final String url;

    public DeepSeekClient(AiProperties properties) {
        this.properties = properties;
        this.url = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(properties.getTimeout());
        this.restClient = RestClient.builder().requestFactory(factory).build();
        // DeepSeek 分片含 model/system_fingerprint/logprobs 等额外字段，忽略未知字段避免整片解析失败
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 流式调用。contentSink 每收到一段文本立即回调（用于 SSE 实时转发），
     * 返回本轮累积结果（content + tool_calls）。
     */
    public StreamAccumulator chatStream(List<ChatMessage> messages, List<ToolDefinition> tools,
                                        Consumer<String> contentSink) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiException("未配置 DEEPSEEK_API_KEY，请在 .env 中填写");
        }
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setModel(properties.getModel());
        req.setMessages(messages);
        req.setTools(tools);
        req.setStream(true);
        req.setTemperature(0.7);
        req.setMaxTokens(2048);
        String body;
        try {
            body = objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new AiException("序列化请求失败", e);
        }
        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((request, response) -> parseStream(response, contentSink));
    }

    private StreamAccumulator parseStream(ClientHttpResponse response, Consumer<String> contentSink) throws IOException {
        if (response.getStatusCode().isError()) {
            String err = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            throw new AiException("DeepSeek 上游错误 " + response.getStatusCode().value() + ": " + err);
        }
        StreamAccumulator acc = new StreamAccumulator();
        Map<Integer, ToolCall> toolAcc = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String data = line.trim();
                if (!data.startsWith("data:")) {
                    continue;
                }
                String payload = data.substring(5).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                ChatCompletionResponse chunk;
                try {
                    chunk = objectMapper.readValue(payload, ChatCompletionResponse.class);
                } catch (JsonProcessingException e) {
                    log.warn("解析流式分片失败: {}", e.getMessage());
                    continue;
                }
                if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
                    continue;
                }
                Choice choice = chunk.getChoices().get(0);
                if (choice.getFinishReason() != null) {
                    acc.setFinishReason(choice.getFinishReason());
                }
                Delta delta = choice.getDelta();
                if (delta == null) {
                    continue;
                }
                if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                    acc.getContent().append(delta.getContent());
                    if (contentSink != null) {
                        contentSink.accept(delta.getContent());
                    }
                }
                if (delta.getToolCalls() != null) {
                    for (ToolCall tc : delta.getToolCalls()) {
                        ToolCall existing = toolAcc.get(tc.getIndex());
                        if (existing == null) {
                            toolAcc.put(tc.getIndex(), tc);
                        } else {
                            existing.appendArguments(tc.getArguments());
                        }
                    }
                }
            }
        }
        List<ToolCall> ordered = new ArrayList<>(toolAcc.values());
        ordered.sort(Comparator.comparingInt(ToolCall::getIndex));
        acc.setToolCalls(ordered);
        return acc;
    }
}
