package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 会话 Mapper 接口
 */
@Mapper
public interface AiConversationMapper {

    int insert(AiConversation conversation);

    AiConversation selectById(@Param("id") Integer id);

    List<AiConversation> selectByUserId(@Param("userId") Integer userId);

    int updateTitle(@Param("id") Integer id, @Param("title") String title);

    int deleteById(@Param("id") Integer id);

    int countByUserAndId(@Param("userId") Integer userId, @Param("id") Integer id);
}
