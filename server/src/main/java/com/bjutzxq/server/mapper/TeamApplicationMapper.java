package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.entity.TeamApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TeamApplicationMapper {

    int insert(TeamApplication app);

    TeamApplication selectById(@Param("id") Integer id);

    TeamApplication selectByTeamAndUser(@Param("teamId") Integer teamId, @Param("applicantId") Integer applicantId);

    List<TeamApplication> selectByTeamId(@Param("teamId") Integer teamId);

    List<TeamApplication> selectByApplicantId(@Param("applicantId") Integer applicantId);

    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    int countByTeamIdAndStatus(@Param("teamId") Integer teamId, @Param("status") Integer status);

    Integer getTeamCreatorId(@Param("teamId") Integer teamId);

    List<Map<String, Object>> selectUserBatch(@Param("userIds") List<Integer> userIds);
}
