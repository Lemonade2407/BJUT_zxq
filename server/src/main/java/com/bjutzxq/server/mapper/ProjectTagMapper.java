package com.bjutzxq.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目标签关联 Mapper 接口
 */
@Mapper
public interface ProjectTagMapper {
    
    /**
     * 为项目添加标签
     * @param projectId 项目 ID
     * @param tagId 标签 ID
     * @return 影响行数
     */
    int insert(@Param("projectId") Integer projectId, @Param("tagId") Integer tagId);
    
    /**
     * 批量为项目添加标签
     * @param projectId 项目 ID
     * @param tagIds 标签 ID 列表
     * @return 影响行数
     */
    int batchInsert(@Param("projectId") Integer projectId, @Param("tagIds") List<Integer> tagIds);
    
    /**
     * 删除项目的指定标签
     * @param projectId 项目 ID
     * @param tagId 标签 ID
     * @return 影响行数
     */
    int deleteByProjectIdAndTagId(@Param("projectId") Integer projectId, @Param("tagId") Integer tagId);
    
    /**
     * 删除项目的所有标签
     * @param projectId 项目 ID
     * @return 影响行数
     */
    int deleteByProjectId(@Param("projectId") Integer projectId);
    
    /**
     * 查询项目的标签 ID 列表
     * @param projectId 项目 ID
     * @return 标签 ID 列表
     */
    List<Integer> selectTagIdsByProjectId(@Param("projectId") Integer projectId);
    
    /**
     * 查询包含指定标签的项目 ID 列表
     * @param tagId 标签 ID
     * @return 项目 ID 列表
     */
    List<Integer> selectProjectIdsByTagId(@Param("tagId") Integer tagId);
}
