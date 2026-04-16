package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.Category;
import com.bjutzxq.server.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    /**
     * 创建分类
     * POST /api/categories
     */
    @PostMapping
    public Result<Category> createCategory(@RequestBody Category category) {
        log.info("收到创建分类请求");
        
        try {
            Category createdCategory = categoryService.createCategory(category);
            log.info("分类创建成功，ID: {}", createdCategory.getId());
            return Result.success("分类创建成功", createdCategory);
        } catch (IllegalArgumentException e) {
            log.warn("创建分类失败：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建分类失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 更新分类
     * PUT /api/categories/{id}
     */
    @PutMapping("/{id}")
    public Result<Category> updateCategory(
            @PathVariable Integer id,
            @RequestBody Category category) {
        log.info("收到更新分类请求，ID: {}", id);
        
        try {
            category.setId(id);
            Category updatedCategory = categoryService.updateCategory(category);
            log.info("分类更新成功，ID: {}", updatedCategory.getId());
            return Result.success("分类更新成功", updatedCategory);
        } catch (IllegalArgumentException e) {
            log.warn("更新分类失败：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新分类失败：{}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新分类失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 删除分类
     * DELETE /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCategory(@PathVariable Integer id) {
        log.info("收到删除分类请求，ID: {}", id);
        
        try {
            boolean result = categoryService.deleteCategory(id);
            log.info("分类删除成功，ID: {}", id);
            return Result.success("分类删除成功", result);
        } catch (RuntimeException e) {
            log.warn("删除分类失败：{}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("删除分类失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 根据 ID 查询分类
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public Result<Category> getCategoryById(@PathVariable Integer id) {
        log.debug("收到查询分类请求，ID: {}", id);
        
        try {
            Category category = categoryService.getCategoryById(id);
            if (category == null) {
                log.warn("分类不存在，ID: {}", id);
                return Result.error(404, "分类不存在");
            }
            log.debug("分类查询成功，ID: {}", id);
            return Result.success(category);
        } catch (Exception e) {
            log.error("查询分类失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 查询分类列表（支持按父分类筛选）
     * GET /api/categories?parentId=xxx
     */
    @GetMapping
    public Result<List<Category>> getCategories(
            @RequestParam(value = "parentId", required = false) Integer parentId) {
        log.debug("收到查询分类列表请求，父分类 ID: {}", parentId);
        
        try {
            List<Category> categories = categoryService.getCategories(parentId);
            log.debug("分类列表查询成功，数量：{}", categories.size());
            return Result.success(categories);
        } catch (Exception e) {
            log.error("查询分类列表失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 查询所有分类（不分层级）
     * GET /api/categories/all
     */
    @GetMapping("/all")
    public Result<List<Category>> getAllCategories() {
        log.debug("收到查询所有分类请求");
        
        try {
            List<Category> categories = categoryService.getAllCategories();
            log.debug("所有分类查询成功，数量：{}", categories.size());
            return Result.success(categories);
        } catch (Exception e) {
            log.error("查询所有分类失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 根据名称搜索分类
     * GET /api/categories/search/name?name=xxx
     */
    @GetMapping("/search/name")
    public Result<List<Category>> searchCategoriesByName(
            @RequestParam(required = true) String name) {
        log.debug("收到按名称搜索分类请求，名称：{}", name);
        
        try {
            List<Category> categories = categoryService.searchCategories(name);
            log.debug("分类搜索成功，名称：{}, 数量：{}", name, categories.size());
            return Result.success(categories);
        } catch (IllegalArgumentException e) {
            log.warn("搜索分类失败：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("搜索分类失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
}
