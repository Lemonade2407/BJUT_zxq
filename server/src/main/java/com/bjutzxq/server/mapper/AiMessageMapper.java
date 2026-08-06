package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 会话消息 Mapper 接口
 */
@Mapper
public interface AiMessageMapper {

    int insert(AiMessage message);

    List<AiMessage> selectByConversationId(@Param("conversationId") Integer conversationId);

    int deleteByConversationId(@Param("conversationId") Integer conversationId);
}
