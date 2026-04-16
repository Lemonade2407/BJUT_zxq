package com.bjutzxq.server.service;

import com.bjutzxq.pojo.Tag;
import com.bjutzxq.server.mapper.ProjectTagMapper;
import com.bjutzxq.server.mapper.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目标签关联服务类
 */
@Slf4j
@Service
public class ProjectTagService {
    
    @Autowired
    private ProjectTagMapper projectTagMapper;
    
    @Autowired
    private TagMapper tagMapper;
    
    /**
     * 为项目设置标签（先删除旧标签，再添加新标签）
     * @param projectId 项目 ID
     * @param tagIds 标签 ID 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void setProjectTags(Integer projectId, List<Integer> tagIds) {
        log.info("为项目设置标签，项目 ID: {}, 标签数量: {}", projectId, tagIds != null ? tagIds.size() : 0);
        
        if (projectId == null) {
            throw new IllegalArgumentException("项目 ID 不能为空");
        }
        
        // 1. 删除项目的旧标签
        projectTagMapper.deleteByProjectId(projectId);
        
        // 2. 如果有新标签，批量插入
        if (tagIds != null && !tagIds.isEmpty()) {
            // 验证标签是否存在
            for (Integer tagId : tagIds) {
                Tag tag = tagMapper.selectById(tagId);
                if (tag == null) {
                    log.warn("标签不存在，ID: {}", tagId);
                    throw new RuntimeException("标签不存在，ID: " + tagId);
                }
            }
            
            // 批量插入
            projectTagMapper.batchInsert(projectId, tagIds);
            log.info("批量插入标签成功，数量: {}", tagIds.size());
            
            // 更新标签使用次数
            updateTagUsageCount(tagIds);
        }
        
        log.info("项目标签设置成功，项目 ID: {}", projectId);
    }
    
    /**
     * 为项目添加标签
     * @param projectId 项目 ID
     * @param tagId 标签 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTagToProject(Integer projectId, Integer tagId) {
        log.info("为项目添加标签，项目 ID: {}, 标签 ID: {}", projectId, tagId);
        
        if (projectId == null || tagId == null) {
            throw new IllegalArgumentException("项目 ID 和标签 ID 不能为空");
        }
        
        // 检查是否已经存在
        List<Integer> existingTags = projectTagMapper.selectTagIdsByProjectId(projectId);
        if (existingTags.contains(tagId)) {
            log.warn("标签已存在，项目 ID: {}, 标签 ID: {}", projectId, tagId);
            return;
        }
        
        // 验证标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new RuntimeException("标签不存在，ID: " + tagId);
        }
        
        // 插入关联
        projectTagMapper.insert(projectId, tagId);
        
        // 更新标签使用次数
        tagMapper.incrementUsageCount(tagId);
        
        log.info("标签添加成功");
    }
    
    /**
     * 从项目中移除标签
     * @param projectId 项目 ID
     * @param tagId 标签 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeTagFromProject(Integer projectId, Integer tagId) {
        log.info("从项目中移除标签，项目 ID: {}, 标签 ID: {}", projectId, tagId);
        
        if (projectId == null || tagId == null) {
            throw new IllegalArgumentException("项目 ID 和标签 ID 不能为空");
        }
        
        // 删除关联
        int rows = projectTagMapper.deleteByProjectIdAndTagId(projectId, tagId);
        
        if (rows > 0) {
            // 更新标签使用次数
            tagMapper.decrementUsageCount(tagId);
            log.info("标签移除成功");
        } else {
            log.warn("标签关联不存在，项目 ID: {}, 标签 ID: {}", projectId, tagId);
        }
    }
    
    /**
     * 获取项目的标签列表
     * @param projectId 项目 ID
     * @return 标签列表
     */
    public List<Tag> getProjectTags(Integer projectId) {
        log.debug("获取项目标签，项目 ID: {}", projectId);
        
        if (projectId == null) {
            throw new IllegalArgumentException("项目 ID 不能为空");
        }
        
        List<Integer> tagIds = projectTagMapper.selectTagIdsByProjectId(projectId);
        List<Tag> tags = new ArrayList<>();
        
        for (Integer tagId : tagIds) {
            Tag tag = tagMapper.selectById(tagId);
            if (tag != null) {
                tags.add(tag);
            }
        }
        
        log.debug("获取到 {} 个标签", tags.size());
        return tags;
    }
    
    /**
     * 根据标签查询项目 ID 列表
     * @param tagId 标签 ID
     * @return 项目 ID 列表
     */
    public List<Integer> getProjectIdsByTagId(Integer tagId) {
        log.debug("根据标签查询项目，标签 ID: {}", tagId);
        
        if (tagId == null) {
            throw new IllegalArgumentException("标签 ID 不能为空");
        }
        
        return projectTagMapper.selectProjectIdsByTagId(tagId);
    }
    
    /**
     * 更新标签使用次数
     * @param tagIds 标签 ID 列表
     */
    private void updateTagUsageCount(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        
        for (Integer tagId : tagIds) {
            tagMapper.incrementUsageCount(tagId);
        }
    }
}
