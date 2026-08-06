package com.bjutzxq.server.ai;

import com.bjutzxq.pojo.dto.AiChatRequest;
import com.bjutzxq.pojo.entity.AiConversation;
import com.bjutzxq.pojo.entity.AiMessage;
import com.bjutzxq.server.ai.model.ChatMessage;
import com.bjutzxq.server.ai.model.StreamAccumulator;
import com.bjutzxq.server.ai.model.ToolCall;
import com.bjutzxq.server.ai.model.ToolDefinition;
import com.bjutzxq.server.mapper.AiConversationMapper;
import com.bjutzxq.server.mapper.AiMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 编排循环：组装消息 → 流式调用 DeepSeek → 执行工具回填 → 循环直至最终回答 → SSE 事件发射
 */
@Slf4j
@Service
public class AiAgentService {

    private final DeepSeekClient deepSeekClient;
    private final AgentTools agentTools;
    private final AiProperties properties;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAgentService(DeepSeekClient deepSeekClient, AgentTools agentTools, AiProperties properties,
                          AiConversationMapper conversationMapper, AiMessageMapper messageMapper) {
        this.deepSeekClient = deepSeekClient;
        this.agentTools = agentTools;
        this.properties = properties;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 在独立线程中执行，结果通过 SseEmitter 流式推送。
     */
    public void streamChat(Integer userId, AiChatRequest req, SseEmitter emitter) {
        try {
            if (req.getMessage() == null || req.getMessage().isBlank()) {
                sendError(emitter, "消息不能为空");
                return;
            }
            if (req.getMessage().length() > 2000) {
                sendError(emitter, "消息过长（最多 2000 字）");
                return;
            }

            // 1. 会话归属校验/新建
            Integer conversationId = req.getConversationId();
            if (conversationId == null) {
                AiConversation conv = new AiConversation();
                conv.setUserId(userId);
                conv.setTitle(truncate(req.getMessage(), 20));
                conversationMapper.insert(conv);
                conversationId = conv.getId();
            } else if (conversationMapper.countByUserAndId(userId, conversationId) == 0) {
                sendError(emitter, "会话不存在");
                return;
            }

            // 2. 先加载历史，再落库本次用户消息（避免本次消息进入历史造成重复）
            List<AiMessage> history = messageMapper.selectByConversationId(conversationId);
            saveMessage(conversationId, "user", req.getMessage(), null, null);

            // 3. 组装：system(用户画像) + 历史 + 本次
            String profileJson = agentTools.execute("get_user_profile", "{}", userId);
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system(AiPrompts.systemPrompt(profileJson)));
            int start = Math.max(0, history.size() - properties.getHistoryLimit());
            for (int i = start; i < history.size(); i++) {
                ChatMessage m = toChatMessage(history.get(i));
                if (m != null) {
                    messages.add(m);
                }
            }
            messages.add(ChatMessage.user(req.getMessage()));
            List<ToolDefinition> tools = AiPrompts.toolDefinitions();

            // 4. Agent 循环
            for (int round = 0; round < properties.getMaxToolRounds(); round++) {
                StreamAccumulator acc = deepSeekClient.chatStream(messages, tools,
                        delta -> {
                            if (Thread.currentThread().isInterrupted()) {
                                throw new ChatCancelledException();
                            }
                            sendDelta(emitter, delta);
                        });
                if (acc.getToolCalls() == null || acc.getToolCalls().isEmpty()) {
                    // 最终回答
                    saveMessage(conversationId, "assistant", acc.getContent().toString(), null, null);
                    break;
                }
                // 工具轮：落库 assistant(tool_calls) 并执行工具
                String toolCallsJson = toJson(acc.getToolCalls());
                saveMessage(conversationId, "assistant", acc.getContent().toString(), toolCallsJson, null);
                messages.add(ChatMessage.assistant(acc.getContent().toString(), acc.getToolCalls()));
                if (round == properties.getMaxToolRounds() - 1) {
                    sendError(emitter, "工具调用次数过多，已停止。请换个问法试试。");
                    return;
                }
                for (ToolCall tc : acc.getToolCalls()) {
                    String result;
                    try {
                        result = agentTools.execute(tc.getName(), tc.getArguments(), userId);
                    } catch (Exception e) {
                        log.warn("工具执行失败 {}: {}", tc.getName(), e.getMessage());
                        result = "{\"error\":\"工具执行失败: " + e.getMessage() + "\"}";
                    }
                    result = truncate(result, properties.getToolResultLimit());
                    saveMessage(conversationId, "tool", result, null, tc.getId());
                    messages.add(ChatMessage.tool(tc.getId(), tc.getName(), result));
                }
            }

            sendDone(emitter, conversationId);
        } catch (ChatCancelledException e) {
            // 客户端已断开，静默结束
            log.debug("AI 对话被客户端取消");
        } catch (AiException e) {
            log.warn("AI 请求失败: {}", e.getMessage());
            sendError(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("AI 对话异常", e);
            sendError(emitter, "服务异常，请稍后再试");
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== SSE 事件 ====================

    private void sendDelta(SseEmitter emitter, String delta) {
        send(emitter, Map.of("type", "delta", "content", delta));
    }

    private void sendDone(SseEmitter emitter, Integer conversationId) {
        send(emitter, Map.of("type", "done", "conversationId", conversationId));
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(toJson(Map.of("type", "error", "message", message))));
        } catch (IOException ignored) {
        }
    }

    private void send(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().data(toJson(payload)));
        } catch (IOException e) {
            throw new ChatCancelledException();
        }
    }

    // ==================== 消息持久化/转换 ====================

    private void saveMessage(Integer conversationId, String role, String content,
                             String toolCalls, String toolCallId) {
        AiMessage m = new AiMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setToolCalls(toolCalls);
        m.setToolCallId(toolCallId);
        messageMapper.insert(m);
    }

    private ChatMessage toChatMessage(AiMessage m) {
        switch (m.getRole() == null ? "" : m.getRole()) {
            case "user":
                return ChatMessage.user(m.getContent());
            case "assistant": {
                List<ToolCall> tcs = null;
                if (m.getToolCalls() != null && !m.getToolCalls().isBlank()) {
                    try {
                        tcs = objectMapper.readValue(m.getToolCalls(), new TypeReference<List<ToolCall>>() {
                        });
                    } catch (JsonProcessingException e) {
                        log.warn("解析历史 tool_calls 失败: {}", e.getMessage());
                    }
                }
                return ChatMessage.assistant(m.getContent(), tcs);
            }
            case "tool":
                return ChatMessage.tool(m.getToolCallId(), null, m.getContent());
            default:
                return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new AiException("JSON 序列化失败", e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * 客户端断开时由 sendDelta 抛出，用于中止上游流式读取
     */
    private static class ChatCancelledException extends RuntimeException {
    }
}
