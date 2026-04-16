package com.bjutzxq.server.mapper;


import com.bjutzxq.pojo.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

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
     * 根据父评论 ID 查询回复列表
     * @param parentId 父评论 ID
     * @return 回复列表
     */
    List<Comment> selectReplies(@Param("parentId") Integer parentId);
    
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
}
