package com.bjutzxq.server.service;

import com.bjutzxq.pojo.Category;
import com.bjutzxq.server.mapper.CategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类服务类
 */
@Slf4j
@Service
public class CategoryService {
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    /**
     * 创建分类
     * @param category 分类对象
     * @return 创建后的分类
     */
    @Transactional(rollbackFor = Exception.class)
    public Category createCategory(Category category) {
        log.info("开始创建分类，名称：{}", category.getName());
        
        // 参数验证
        if (category == null) {
            log.warn("创建分类失败：分类信息为空");
            throw new IllegalArgumentException("分类信息不能为空");
        }
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            log.warn("创建分类失败：分类名称为空");
            throw new IllegalArgumentException("分类名称不能为空");
        }
        
        // 检查分类名称是否存在
        int count = categoryMapper.countByNameExcludeId(category.getName().trim(), null);
        if (count > 0) {
            log.warn("分类名称已存在：{}", category.getName());
            throw new RuntimeException("分类名称已存在");
        }
        
        // 设置默认排序值（如果未指定）
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        // 插入分类
        categoryMapper.insert(category);
        
        log.info("分类创建成功，ID：{}", category.getId());
        return category;
    }
    
    /**
     * 更新分类
     * @param category 分类对象（必须包含 id）
     * @return 更新后的分类
     */
    @Transactional(rollbackFor = Exception.class)
    public Category updateCategory(Category category) {
        log.info("开始更新分类，ID：{}", category.getId());
        
        // 参数验证
        if (category == null || category.getId() == null) {
            log.warn("更新分类失败：分类 ID 为空");
            throw new IllegalArgumentException("分类 ID 不能为空");
        }
        if (category.getName() != null && category.getName().trim().isEmpty()) {
            log.warn("更新分类失败：分类名称为空");
            throw new IllegalArgumentException("分类名称不能为空");
        }
        
        // 查询原分类
        Category existingCategory = categoryMapper.selectById(category.getId());
        if (existingCategory == null) {
            log.warn("更新分类失败：分类不存在，ID: {}", category.getId());
            throw new RuntimeException("分类不存在");
        }
        
        // 如果修改了名称，检查是否与其他分类重名
        if (category.getName() != null && !category.getName().trim().isEmpty() 
            && !category.getName().trim().equals(existingCategory.getName())) {
            int count = categoryMapper.countByNameExcludeId(category.getName().trim(), category.getId());
            if (count > 0) {
                log.warn("分类名称已存在：{}", category.getName());
                throw new RuntimeException("分类名称已存在");
            }
            existingCategory.setName(category.getName().trim());
        }
        
        // 更新其他字段
        if (category.getParentId() != null) {
            existingCategory.setParentId(category.getParentId());
        }
        if (category.getSortOrder() != null) {
            existingCategory.setSortOrder(category.getSortOrder());
        }
        
        // 执行更新
        int rows = categoryMapper.update(existingCategory);
        if (rows == 0) {
            log.error("更新分类失败：数据库更新失败，ID: {}", category.getId());
            throw new RuntimeException("更新分类失败");
        }
        
        log.info("分类更新成功，ID：{}", category.getId());
        return existingCategory;
    }
    
    /**
     * 删除分类
     * @param id 分类 ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Integer id) {
        log.info("开始删除分类，ID：{}", id);
        
        // 检查分类是否存在
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            log.warn("删除分类失败：分类不存在，ID: {}", id);
            throw new RuntimeException("分类不存在");
        }
        
        // 删除分类
        int rows = categoryMapper.deleteById(id);
        if (rows == 0) {
            log.error("删除分类失败：数据库删除失败，ID: {}", id);
            throw new RuntimeException("删除分类失败");
        }
        
        log.info("分类删除成功，ID：{}", id);
        return true;
    }
    
    /**
     * 根据 ID 查询分类
     * @param id 分类 ID
     * @return 分类对象
     */
    public Category getCategoryById(Integer id) {
        log.debug("查询分类，ID：{}", id);
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            log.debug("分类不存在，ID：{}", id);
        }
        return category;
    }
    
    /**
     * 查询所有分类
     * @param parentId 父分类 ID，为 null 时查询所有一级分类
     * @return 分类列表
     */
    public List<Category> getCategories(Integer parentId) {
        log.debug("查询分类列表，父分类 ID: {}", parentId);
        return categoryMapper.selectByParentId(parentId);
    }
    
    /**
     * 查询所有分类（不分层级）
     * @return 分类列表
     */
    public List<Category> getAllCategories() {
        log.debug("查询所有分类");
        return categoryMapper.selectAll();
    }
    
    /**
     * 根据名称搜索分类
     * @param name 分类名称
     * @return 分类列表
     */
    public List<Category> searchCategories(String name) {
        log.debug("按名称搜索分类，名称：{}", name);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        return categoryMapper.selectByName(name.trim());
    }
}
