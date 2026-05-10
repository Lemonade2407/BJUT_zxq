package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.ProjectTag;
import com.bjutzxq.pojo.entity.Tag;
import com.bjutzxq.server.mapper.ProjectTagMapper;
import com.bjutzxq.server.mapper.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
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
            // 批量验证标签是否存在
            List<Tag> existingTags = tagMapper.selectByIds(tagIds);
            if (existingTags.size() != tagIds.size()) {
                java.util.Set<Integer> existingIds = existingTags.stream().map(Tag::getId).collect(Collectors.toSet());
                for (Integer tagId : tagIds) {
                    if (!existingIds.contains(tagId)) throw new RuntimeException("标签不存在，ID: " + tagId);
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
        List<Tag> tags;
        if (tagIds != null && !tagIds.isEmpty()) {
            tags = new ArrayList<>(tagMapper.selectByIds(tagIds));
        } else {
            tags = new ArrayList<>();
        }
        
        log.debug("获取到 {} 个标签", tags.size());
        return tags;
    }

    /**
     * 批量获取多个项目的标签
     * @param projectIds 项目 ID 列表
     * @return 项目ID → 标签列表的映射
     */
    public Map<Integer, List<Tag>> getProjectTagsBatch(List<Integer> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Map.of();
        List<ProjectTag> mappings = projectTagMapper.selectByProjectIds(projectIds);
        if (mappings.isEmpty()) {
            return projectIds.stream().collect(Collectors.toMap(id -> id, id -> List.of()));
        }
        List<Integer> allTagIds = mappings.stream().map(ProjectTag::getTagId).distinct().collect(Collectors.toList());
        Map<Integer, Tag> tagMap = tagMapper.selectByIds(allTagIds).stream()
            .collect(Collectors.toMap(Tag::getId, t -> t));
        Map<Integer, List<Tag>> result = new java.util.HashMap<>();
        for (Integer pid : projectIds) result.put(pid, new ArrayList<>());
        for (ProjectTag pt : mappings) {
            Tag tag = tagMap.get(pt.getTagId());
            if (tag != null) result.get(pt.getProjectId()).add(tag);
        }
        return result;
    }

    /**
     * 移除项目的所有标签关联（用于删除项目时）
     * @param projectId 项目 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeProjectTags(Integer projectId) {
        log.info("移除项目的所有标签关联，项目 ID: {}", projectId);
        
        if (projectId == null) {
            throw new IllegalArgumentException("项目 ID 不能为空");
        }
        
        // 获取项目的标签 ID 列表（用于更新使用次数）
        List<Integer> tagIds = projectTagMapper.selectTagIdsByProjectId(projectId);
        
        // 删除项目的所有标签关联
        int deletedCount = projectTagMapper.deleteByProjectId(projectId);
        log.info("删除标签关联数量: {}", deletedCount);
        
        // 更新标签使用次数（减少）
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Integer tagId : tagIds) {
                tagMapper.decrementUsageCount(tagId);
            }
            log.info("更新标签使用次数，标签数量: {}", tagIds.size());
        }
        
        log.info("项目标签关联清理完成，项目 ID: {}", projectId);
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
