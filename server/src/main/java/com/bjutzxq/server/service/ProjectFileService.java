package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目文件服务类
 */
@Slf4j
@Service
public class ProjectFileService {

    @Autowired
    private ProjectFileMapper projectFileMapper;

    @Autowired
    private OssUtil ossUtil;

    /**
     * 获取项目的所有文件（用于构建完整的树形结构）
     */
    public List<ProjectFile> getAllFiles(Integer projectId) {
        log.debug("获取项目所有文件，项目 ID: {}", projectId);
        return projectFileMapper.selectByProjectId(projectId);
    }

    /**
     * 插入单条文件记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertSingle(ProjectFile file) {
        projectFileMapper.insert(file);
    }

    /**
     * 批量创建文件记录（OSS 直传确认后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "directoryCache", allEntries = true)
    public List<ProjectFile> batchCreate(List<ProjectFile> projectFiles) {
        for (ProjectFile file : projectFiles) {
            if (file.getIsDir() == null) {
                file.setIsDir(0);
            }
            projectFileMapper.insert(file);
        }
        log.info("批量创建 {} 条文件记录", projectFiles.size());
        return projectFiles;
    }

    /**
     * 删除项目的所有文件（包括 OSS 和数据库记录）
     * @param projectId 项目 ID
     * @return 删除的数据库记录数
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "directoryCache", allEntries = true)
    public int deleteAllProjectFiles(Integer projectId) {
        log.info("开始删除项目所有文件，项目 ID: {}", projectId);
        
        // 1. 获取项目的所有文件（用于删除 OSS 文件）
        List<ProjectFile> projectFiles = projectFileMapper.selectByProjectId(projectId);
        if (projectFiles == null || projectFiles.isEmpty()) {
            log.info("项目没有文件，无需删除");
            return 0;
        }
        
        log.info("找到 {} 个文件，开始删除...", projectFiles.size());
        
        // 2. 收集需要删除的 OSS 文件 URL（只删除实际文件，不包括目录）
        List<String> ossUrlsToDelete = projectFiles.stream()
            .filter(file -> file.getIsDir() == null || file.getIsDir() == 0)  // 只保留文件
            .map(ProjectFile::getStorageUrl)
            .filter(url -> url != null && !url.trim().isEmpty())
            .collect(java.util.stream.Collectors.toList());
        
        int ossSuccessCount = 0;
        int ossFailCount = 0;
        
        // 3. 并行删除 OSS 文件（使用 CompletableFuture）
        if (!ossUrlsToDelete.isEmpty()) {
            log.info("开始并行删除 {} 个 OSS 文件...", ossUrlsToDelete.size());
            
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(
                Math.min(10, ossUrlsToDelete.size())  // 最多 10 个线程
            );
            
            List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (String ossUrl : ossUrlsToDelete) {
                java.util.concurrent.CompletableFuture<Void> future = 
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            ossUtil.delete(ossUrl);
                            log.debug("OSS 文件删除成功: {}", ossUrl);
                        } catch (Exception e) {
                            log.warn("删除 OSS 文件失败: {}, 错误: {}", ossUrl, e.getMessage());
                        }
                    }, executor);
                
                futures.add(future);
            }
            
            // 等待所有删除完成并统计结果
            for (java.util.concurrent.CompletableFuture<Void> future : futures) {
                try {
                    future.join();  // 等待完成
                    ossSuccessCount++;
                } catch (Exception e) {
                    ossFailCount++;
                    log.error("OSS 删除任务异常: {}", e.getMessage());
                }
            }
            
            // 关闭线程池
            executor.shutdown();
            
            log.info("OSS 文件删除完成 - 成功: {}, 失败: {}", ossSuccessCount, ossFailCount);
        }
        
        // 3. 批量删除数据库记录（一次性删除，避免 N+1 问题）
        int dbDeletedCount = projectFileMapper.deleteByProjectId(projectId);
        
        log.info("项目文件删除完成 - OSS: 成功={}, 失败={}; 数据库: 删除={} 条记录", 
                ossSuccessCount, ossFailCount, dbDeletedCount);
        
        return dbDeletedCount;
    }

}

