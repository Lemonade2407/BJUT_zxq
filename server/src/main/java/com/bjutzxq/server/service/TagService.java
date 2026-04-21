package com.bjutzxq.server.service;

import com.bjutzxq.pojo.Tag;
import com.bjutzxq.server.mapper.ProjectTagMapper;
import com.bjutzxq.server.mapper.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签服务类
 */
@Slf4j
@Service
public class TagService {
    @Autowired
    private TagMapper tagMapper;
    
    @Autowired
    private ProjectTagMapper projectTagMapper;
    
    /**
     * 创建标签
     * @param tag 标签对象
     * @return 创建后的标签
     */
    @Transactional(rollbackFor = Exception.class)
    public Tag createTag(Tag tag) {
        log.info("开始创建标签，名称：{}", tag.getName());
        
        // 参数验证
        if (tag == null) {
            log.warn("创建标签失败：标签信息为空");
            throw new IllegalArgumentException("标签信息不能为空");
        }
        if (tag.getName() == null || tag.getName().trim().isEmpty()) {
            log.warn("创建标签失败：标签名称为空");
            throw new IllegalArgumentException("标签名称不能为空");
        }
        
        // 检查标签名称是否存在
        int count = tagMapper.countByNameExcludeId(tag.getName().trim(), null);
        if (count > 0) {
            log.warn("标签名称已存在：{}", tag.getName());
            throw new RuntimeException("标签名称已存在");
        }
        
        // 设置初始使用次数
        if (tag.getUsageCount() == null) {
            tag.setUsageCount(0);
        }
        
        // 插入标签
        tagMapper.insert(tag);
        
        log.info("标签创建成功，ID：{}", tag.getId());
        return tag;
    }
    
    /**
     * 更新标签
     * @param tag 标签对象（必须包含 id）
     * @return 更新后的标签
     */
    @Transactional(rollbackFor = Exception.class)
    public Tag updateTag(Tag tag) {
        log.info("开始更新标签，ID：{}", tag.getId());
        
        // 参数验证
        if (tag == null || tag.getId() == null) {
            log.warn("更新标签失败：标签 ID 为空");
            throw new IllegalArgumentException("标签 ID 不能为空");
        }
        if (tag.getName() != null && tag.getName().trim().isEmpty()) {
            log.warn("更新标签失败：标签名称为空");
            throw new IllegalArgumentException("标签名称不能为空");
        }
        
        // 查询原标签
        Tag existingTag = tagMapper.selectById(tag.getId());
        if (existingTag == null) {
            log.warn("更新标签失败：标签不存在，ID: {}", tag.getId());
            throw new RuntimeException("标签不存在");
        }
        
        // 如果修改了名称，检查是否与其他标签重名
        if (tag.getName() != null && !tag.getName().trim().isEmpty() 
            && !tag.getName().trim().equals(existingTag.getName())) {
            int count = tagMapper.countByNameExcludeId(tag.getName().trim(), tag.getId());
            if (count > 0) {
                log.warn("标签名称已存在：{}", tag.getName());
                throw new RuntimeException("标签名称已存在");
            }
            existingTag.setName(tag.getName().trim());
        }
        
        // 执行更新
        int rows = tagMapper.update(existingTag);
        if (rows == 0) {
            log.error("更新标签失败：数据库更新失败，ID: {}", tag.getId());
            throw new RuntimeException("更新标签失败");
        }
        
        log.info("标签更新成功，ID：{}", tag.getId());
        return existingTag;
    }
    
    /**
     * 删除标签
     * @param id 标签 ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Integer id) {
        log.info("开始删除标签，ID：{}", id);
        
        // 检查标签是否存在
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            log.warn("删除标签失败：标签不存在，ID: {}", id);
            throw new RuntimeException("标签不存在");
        }
        
        // 检查是否有项目使用该标签
        List<Integer> projectIds = projectTagMapper.selectProjectIdsByTagId(id);
        if (projectIds != null && !projectIds.isEmpty()) {
            log.warn("删除标签失败：该标签正在被 {} 个项目使用，ID: {}", projectIds.size(), id);
            throw new RuntimeException(String.format(
                "无法删除标签 '%s'，该标签正在被 %d 个项目使用。请先从相关项目中移除该标签。",
                tag.getName(),
                projectIds.size()
            ));
        }
        
        // 删除标签
        int rows = tagMapper.deleteById(id);
        if (rows == 0) {
            log.error("删除标签失败：数据库删除失败，ID: {}", id);
            throw new RuntimeException("删除标签失败");
        }
        
        log.info("标签删除成功，ID：{}", id);
        return true;
    }
    
    /**
     * 根据 ID 查询标签
     * @param id 标签 ID
     * @return 标签对象
     */
    public Tag getTagById(Integer id) {
        log.debug("查询标签，ID：{}", id);
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            log.debug("标签不存在，ID：{}", id);
        }
        return tag;
    }
    
    /**
     * 查询所有标签
     * @return 标签列表
     */
    public List<Tag> getAllTags() {
        log.debug("查询所有标签");
        return tagMapper.selectAll();
    }
    
    /**
     * 根据名称搜索标签
     * @param name 标签名称
     * @return 标签列表
     */
    public List<Tag> searchTags(String name) {
        log.debug("按名称搜索标签，名称：{}", name);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        return tagMapper.selectByName(name.trim());
    }
    
    /**
     * 查询热门标签
     * @param limit 返回数量限制
     * @return 标签列表
     */
    public List<Tag> getHotTags(Integer limit) {
        log.debug("查询热门标签，限制数量：{}", limit);
        if (limit == null || limit <= 0) {
            limit = 10; // 默认返回 10 个
        }
        if (limit > 50) {
            limit = 50; // 最多返回 50 个
        }
        return tagMapper.selectHotTags(limit);
    }
    
    /**
     * 增加标签使用次数
     * @param tagId 标签 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementTagUsage(Integer tagId) {
        log.debug("增加标签使用次数，标签 ID: {}", tagId);
        Tag tag = tagMapper.selectById(tagId);
        if (tag != null) {
            tagMapper.incrementUsageCount(tagId);
        } else {
            log.warn("标签不存在，无法增加使用次数，ID: {}", tagId);
        }
    }
    
    /**
     * 减少标签使用次数
     * @param tagId 标签 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void decrementTagUsage(Integer tagId) {
        log.debug("减少标签使用次数，标签 ID: {}", tagId);
        Tag tag = tagMapper.selectById(tagId);
        if (tag != null) {
            tagMapper.decrementUsageCount(tagId);
        } else {
            log.warn("标签不存在，无法减少使用次数，ID: {}", tagId);
        }
    }
}
