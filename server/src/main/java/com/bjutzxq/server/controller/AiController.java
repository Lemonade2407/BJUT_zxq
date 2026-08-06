package com.bjutzxq.server.controller;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.AiChatRequest;
import com.bjutzxq.pojo.entity.AiConversation;
import com.bjutzxq.pojo.entity.AiMessage;
import com.bjutzxq.server.ai.AiAgentService;
import com.bjutzxq.server.ai.AiRateLimiter;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.mapper.AiConversationMapper;
import com.bjutzxq.server.mapper.AiMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * AI 选题与组队助手
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final ExecutorService agentExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AiAgentService aiAgentService;
    private final AiRateLimiter aiRateLimiter;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    public AiController(AiAgentService aiAgentService, AiRateLimiter aiRateLimiter,
                        AiConversationMapper conversationMapper, AiMessageMapper messageMapper) {
        this.aiAgentService = aiAgentService;
        this.aiRateLimiter = aiRateLimiter;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * AI 对话（SSE 流式）
     * POST /api/ai/chat  body: {"conversationId":1,"message":"帮我选个竞赛选题"}
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AiChatRequest req) {
        Integer userId = UserIdContext.getCurrentUserId();
        aiRateLimiter.check(userId);
        SseEmitter emitter = new SseEmitter(300_000L);
        agentExecutor.execute(() -> aiAgentService.streamChat(userId, req, emitter));
        return emitter;
    }

    /**
     * 当前用户的会话列表
     */
    @GetMapping("/conversations")
    public Result<List<AiConversation>> conversations() {
        Integer userId = UserIdContext.getCurrentUserId();
        return Result.success(conversationMapper.selectByUserId(userId));
    }

    /**
     * 会话历史（仅 user/assistant，用于前端展示）
     */
    @GetMapping("/conversations/{id}/messages")
    public Result<List<AiMessage>> messages(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        requireOwner(userId, id);
        return Result.success(messageMapper.selectByConversationId(id).stream()
                .filter(m -> "user".equals(m.getRole()) || "assistant".equals(m.getRole()))
                .collect(Collectors.toList()));
    }

    /**
     * 删除会话（级联删除消息）
     */
    @DeleteMapping("/conversations/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        requireOwner(userId, id);
        conversationMapper.deleteById(id);
        return Result.success("已删除", null);
    }

    private void requireOwner(Integer userId, Integer conversationId) {
        if (conversationMapper.countByUserAndId(userId, conversationId) == 0) {
            throw new BusinessException("会话不存在");
        }
    }
}
