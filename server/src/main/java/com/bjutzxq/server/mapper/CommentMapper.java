package com.bjutzxq.server.mapper;


import com.bjutzxq.pojo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 评论 Mapper 接口
 */
@Mapper
public interface CommentMapper {
    
    /**
     * 新增评论
     * @param comment 评论信息
     * @return 影响行数
     */
    int insert(Comment comment);
    
    /**
     * 根据 ID 查询评论
     * @param id 评论 ID
     * @return 评论信息
     */
    Comment selectById(@Param("id") Integer id);
    
    /**
     * 根据项目 ID 查询评论列表（分页）
     * @param projectId 项目 ID
     * @param status 评论状态
     * @return 评论列表
     */
    List<Comment> selectByProjectId(
            @Param("projectId") Integer projectId,
            @Param("status") Integer status);
    
    /**
     * 更新评论信息
     * @param comment 评论信息
     * @return 影响行数
     */
    int updateById(Comment comment);
    
    /**
     * 删除评论
     * @param id 评论 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 统计项目评论数
     * @param projectId 项目 ID
     * @return 评论数
     */
    int countByProjectId(@Param("projectId") Integer projectId);
    
    /**
     * 统计评论总数
     * @return 评论总数
     */
    int countAll();
    
    /**
     * 根据用户 ID 删除所有评论
     * @param userId 用户 ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Integer userId);
    
    /**
     * 批量查询评论的用户信息（优化 N+1 问题）
     * @param userIds 用户 ID 列表
     * @return 用户信息列表（id, username, avatar）
     */
    List<Map<String, Object>> selectUserBatch(@Param("userIds") List<Integer> userIds);
    
    /**
     * 获取项目所有者ID
     * @param projectId 项目 ID
     * @return 所有者 ID
     */
    Integer getProjectOwnerId(@Param("projectId") Integer projectId);
    
    /**
     * 获取项目名称
     * @param projectId 项目 ID
     * @return 项目名称
     */
    String getProjectName(@Param("projectId") Integer projectId);

    /**
     * 批量获取项目名称
     * @param projectIds 项目 ID 列表
     * @return 项目名称映射
     */
    List<Map<String, Object>> getProjectNamesBatch(@Param("projectIds") List<Integer> projectIds);

    /**
     * 分页查询所有评论（管理员）
     * @param status 评论状态（null查全部）
     * @return 评论列表
     */
    List<Comment> selectAllWithPage(@Param("status") Integer status);

    /**
     * 按关键词搜索评论（管理员）
     * @param keyword 关键词
     * @return 评论列表
     */
    List<Comment> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 统计关键词匹配的评论数
     * @param keyword 关键词
     * @return 评论数
     */
    int countByKeyword(@Param("keyword") String keyword);

    /**
     * 统计按状态的评论数（管理员）
     * @param status 评论状态（null查全部）
     * @return 评论数
     */
    int countByStatus(@Param("status") Integer status);

    /**
     * 物理删除评论
     * @param id 评论 ID
     * @return 影响行数
     */
    int forceDeleteById(@Param("id") Integer id);
    int countByUserId(@Param("userId") Integer userId);
    int countByMonth(@Param("month") String month);
    int countByUserIdAndMonth(@Param("userId") Integer userId, @Param("month") String month);
}