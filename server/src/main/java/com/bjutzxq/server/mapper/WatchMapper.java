package com.bjutzxq.server.mapper;


import com.bjutzxq.pojo.Watch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 关注 Mapper 接口
 */
@Mapper
public interface WatchMapper {
    
    /**
     * 新增关注记录
     * @param watch 关注信息
     * @return 影响行数
     */
    int insert(Watch watch);
    
    /**
     * 根据用户 ID 和项目 ID 查询关注记录
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 关注记录
     */
    Watch selectByUserIdAndProjectId(
            @Param("userId") Integer userId,
            @Param("projectId") Integer projectId);
    
    /**
     * 删除关注记录
     * @param id 关注记录 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 统计项目关注数
     * @param projectId 项目 ID
     * @return 关注数
     */
    int countByProjectId(@Param("projectId") Integer projectId);
    
    /**
     * 查询用户关注的所有项目 ID 列表
     * @param userId 用户 ID
     * @return 项目 ID 列表
     */
    java.util.List<Integer> selectProjectIdsByUserId(@Param("userId") Integer userId);
}
