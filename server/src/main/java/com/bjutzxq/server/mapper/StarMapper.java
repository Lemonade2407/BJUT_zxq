package com.bjutzxq.server.mapper;


import com.bjutzxq.pojo.Star;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 点赞 Mapper 接口
 */
@Mapper
public interface StarMapper {
    
    /**
     * 新增点赞记录
     * @param star 点赞信息
     * @return 影响行数
     */
    int insert(Star star);
    
    /**
     * 根据用户 ID 和项目 ID 查询点赞记录
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 点赞记录
     */
    Star selectByUserIdAndProjectId(
            @Param("userId") Integer userId,
            @Param("projectId") Integer projectId);
    
    /**
     * 删除点赞记录
     * @param id 点赞记录 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 统计项目点赞数
     * @param projectId 项目 ID
     * @return 点赞数
     */
    int countByProjectId(@Param("projectId") Integer projectId);
}
