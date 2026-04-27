package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标签 Mapper 接口
 */
@Mapper
public interface TagMapper {
    
    /**
     * 插入标签
     * @param tag 标签对象
     * @return 影响行数
     */
    int insert(Tag tag);
    
    /**
     * 根据 ID 删除标签
     * @param id 标签 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 更新标签
     * @param tag 标签对象
     * @return 影响行数
     */
    int update(Tag tag);
    
    /**
     * 根据 ID 查询标签
     * @param id 标签 ID
     * @return 标签对象
     */
    Tag selectById(@Param("id") Integer id);
    
    /**
     * 查询所有标签
     * @return 标签列表
     */
    List<Tag> selectAll();
    
    /**
     * 根据分组查询标签
     * @param category 标签分组
     * @return 标签列表
     */
    List<Tag> selectByCategory(@Param("category") String category);
    
    /**
     * 根据名称查询标签（模糊查询）
     * @param name 标签名称
     * @return 标签列表
     */
    List<Tag> selectByName(@Param("name") String name);
    
    /**
     * 检查标签名称是否存在（排除自身）
     * @param name 标签名称
     * @param excludeId 排除的标签 ID
     * @return 存在的数量
     */
    int countByNameExcludeId(@Param("name") String name, @Param("excludeId") Integer excludeId);
    
    /**
     * 增加标签使用次数
     * @param id 标签 ID
     * @return 影响行数
     */
    int incrementUsageCount(@Param("id") Integer id);
    
    /**
     * 减少标签使用次数
     * @param id 标签 ID
     * @return 影响行数
     */
    int decrementUsageCount(@Param("id") Integer id);
    
    /**
     * 查询热门标签
     * @param limit 返回数量限制
     * @return 标签列表
     */
    List<Tag> selectHotTags(@Param("limit") Integer limit);
}
