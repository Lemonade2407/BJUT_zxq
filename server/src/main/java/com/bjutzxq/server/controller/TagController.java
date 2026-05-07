package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Tag;
import com.bjutzxq.server.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@Slf4j
@RestController
@RequestMapping("/tags")
@Validated
public class TagController {
    
    @Autowired
    private TagService tagService;
    
    /**
     * 创建标签
     * POST /api/tags
     */
    @PostMapping
    public Result<Tag> createTag(
            @RequestParam String name,
            @RequestParam String category) {
        log.info("收到创建标签请求，名称：{}", name);
        
        // 构建 Entity
        Tag tag = new Tag();
        tag.setName(name);
        tag.setCategory(category);
        
        Tag createdTag = tagService.createTag(tag);
        log.info("标签创建成功，ID: {}", createdTag.getId());
        return Result.success("标签创建成功", createdTag);
    }
    
    /**
     * 更新标签
     * PUT /api/tags/{id}
     */
    @PutMapping("/{id}")
    public Result<Tag> updateTag(
            @PathVariable Integer id,
            @RequestParam String name,
            @RequestParam String category) {
        log.info("收到更新标签请求，ID: {}", id);
        
        // 构建 Entity
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        tag.setCategory(category);
        
        Tag updatedTag = tagService.updateTag(tag);
        log.info("标签更新成功，ID: {}", updatedTag.getId());
        return Result.success("标签更新成功", updatedTag);
    }
    
    /**
     * 删除标签
     * DELETE /api/tags/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTag(@PathVariable Integer id) {
        log.info("收到删除标签请求，ID: {}", id);
        
        boolean result = tagService.deleteTag(id);
        log.info("标签删除成功，ID: {}", id);
        return Result.success("标签删除成功", result);
    }
    
    /**
     * 根据 ID 查询标签
     * GET /api/tags/{id}
     */
    @GetMapping("/{id}")
    public Result<Tag> getTagById(@PathVariable Integer id) {
        log.debug("收到查询标签请求，ID: {}", id);
        Tag tag = tagService.getTagById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        log.debug("标签查询成功，ID: {}", id);
        return Result.success(tag);
    }
    
    /**
     * 查询所有标签
     * GET /api/tags
     */
    @GetMapping
    public Result<List<Tag>> getAllTags() {
        log.debug("收到查询所有标签请求");
        
        List<Tag> tags = tagService.getAllTags();
        log.debug("所有标签查询成功，数量：{}", tags.size());
        return Result.success(tags);
    }
    
    /**
     * 根据分组查询标签
     * GET /api/tags/category/{category}
     */
    @GetMapping("/category/{category}")
    public Result<List<Tag>> getTagsByCategory(@PathVariable String category) {
        log.debug("收到按分组查询标签请求，分组：{}", category);
        
        List<Tag> tags = tagService.getTagsByCategory(category);
        log.debug("分组标签查询成功，分组：{}, 数量：{}", category, tags.size());
        return Result.success(tags);
    }
    
    /**
     * 根据名称搜索标签
     * GET /api/tags/search/name?name=xxx
     */
    @GetMapping("/search/name")
    public Result<List<Tag>> searchTagsByName(
            @RequestParam() String name) {
        log.debug("收到按名称搜索标签请求，名称：{}", name);
        
        List<Tag> tags = tagService.searchTags(name);
        log.debug("标签搜索成功，名称：{}, 数量：{}", name, tags.size());
        return Result.success(tags);
    }
    
    /**
     * 查询热门标签
     * GET /api/tags/hot?limit=10
     */
    @GetMapping("/hot")
    public Result<List<Tag>> getHotTags(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.debug("收到查询热门标签请求，限制数量：{}", limit);
        
        List<Tag> tags = tagService.getHotTags(limit);
        log.debug("热门标签查询成功，数量：{}", tags.size());
        return Result.success(tags);
    }
}
