package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类 Mapper 接口
 */
@Mapper
public interface CategoryMapper {
    
    /**
     * 插入分类
     * @param category 分类对象
     * @return 影响行数
     */
    int insert(Category category);
    
    /**
     * 根据 ID 删除分类
     * @param id 分类 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 更新分类
     * @param category 分类对象
     * @return 影响行数
     */
    int update(Category category);
    
    /**
     * 根据 ID 查询分类
     * @param id 分类 ID
     * @return 分类对象
     */
    Category selectById(@Param("id") Integer id);
    
    /**
     * 查询所有分类（支持按父分类筛选）
     * @param parentId 父分类 ID，为 null 时查询所有一级分类
     * @return 分类列表
     */
    List<Category> selectByParentId(@Param("parentId") Integer parentId);
    
    /**
     * 查询所有分类
     * @return 分类列表
     */
    List<Category> selectAll();
    
    /**
     * 根据名称查询分类
     * @param name 分类名称
     * @return 分类列表
     */
    List<Category> selectByName(@Param("name") String name);
    
    /**
     * 检查分类名称是否存在（排除自身）
     * @param name 分类名称
     * @param excludeId 排除的分类 ID
     * @return 存在的数量
     */
    int countByNameExcludeId(@Param("name") String name, @Param("excludeId") Integer excludeId);
}
