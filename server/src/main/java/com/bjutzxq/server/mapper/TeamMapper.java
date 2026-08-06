package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeamMapper {

    int insert(Team team);

    Team selectById(@Param("id") Integer id);

    List<Team> selectAll(@Param("tag") String tag, @Param("status") Integer status, @Param("courseName") String courseName);

    List<Team> selectByUserId(@Param("userId") Integer userId);

    int updateById(Team team);

    int deleteById(@Param("id") Integer id);

    int countAll(@Param("tag") String tag, @Param("status") Integer status, @Param("courseName") String courseName);

    List<Map<String, Object>> selectUserBatch(@Param("userIds") List<Integer> userIds);
    int countByUserId(@Param("userId") Integer userId);

    /**
     * AI 助手检索组队（动态条件 + 时间倒序 + 限量）
     */
    List<Team> searchForAi(
        @Param("keyword") String keyword,
        @Param("tag") String tag,
        @Param("courseName") String courseName,
        @Param("status") Integer status,
        @Param("limit") Integer limit
    );
}