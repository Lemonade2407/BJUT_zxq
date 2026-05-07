package com.bjutzxq.server.service;

import com.bjutzxq.common.Constants;
import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目文件服务类
 */
@Slf4j
@Service
public class ProjectFileService {

    // 日志记录间隔常量
    private static final int PROGRESS_LOG_INTERVAL = 10;
    // 大文件阈值常量（字节）
    private static final long LARGE_FILE_THRESHOLD = 100 * 1024 * 1024; // 100MB

    @Autowired
    private ProjectFileMapper projectFileMapper;
    
    @Autowired
    private OssUtil ossUtil;
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * 批量上传文件（支持文件夹解析）
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "directoryCache", allEntries = true)
    public List<ProjectFile> uploadFiles(Integer projectId, MultipartFile[] files, Integer parentId, Integer uploaderId) {
        log.info("开始批量上传文件，项目 ID: {}, 文件数量：{}", projectId, files.length);
        
        List<ProjectFile> uploadedFiles = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file.isEmpty()) {
                    log.warn("跳过空文件：{}", file.getOriginalFilename());
                    continue;
                }
                
                if ((i + 1) % PROGRESS_LOG_INTERVAL == 0 || i == files.length - 1) {
                    log.info("上传进度: {}/{}", i + 1, files.length);
                }
                
                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null || originalFileName.trim().isEmpty()) {
                    continue;
                }
                
                originalFileName = originalFileName.replace("\\", "/");
                
                ProjectFile uploadedFile = originalFileName.contains("/") 
                    ? uploadFileWithPath(projectId, file, parentId, uploaderId)
                    : uploadFile(projectId, file, parentId, uploaderId);
                uploadedFiles.add(uploadedFile);
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("批量上传完成！成功上传 {} 个文件，总耗时: {}秒", uploadedFiles.size(), totalTime / 1000);
            return uploadedFiles;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("批量上传失败：已处理 {}/{} 个文件，耗时: {}秒，错误: {}", 
                uploadedFiles.size(), files.length, totalTime / 1000, e.getMessage(), e);
            throw new RuntimeException("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传单个文件
     */
    public ProjectFile uploadFile(Integer projectId, MultipartFile file, Integer parentId, Integer uploaderId) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("上传文件不能为空");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }
            
            return saveUploadedFile(projectId, file, fileName, parentId, uploaderId);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }
    
    /**
     * 构建文件路径字符串
     */
    private String buildFilePath(Integer parentId) {
        if (parentId == null) {
            return "/";
        }
        
        StringBuilder pathBuilder = new StringBuilder();
        buildPathRecursive(parentId, pathBuilder);
        return pathBuilder.toString();
    }
    
    /**
     * 递归构建路径
     */
    private void buildPathRecursive(Integer parentId, StringBuilder pathBuilder) {
        if (parentId == null) {
            return;
        }
        
        ProjectFile parent = projectFileMapper.selectById(parentId);
        if (parent != null && Constants.File.TYPE_DIRECTORY.equals(parent.getIsDir())) {
            buildPathRecursive(parent.getParentId(), pathBuilder);
            pathBuilder.append(parent.getFileName()).append("/");
        } else {
            pathBuilder.append("/");
        }
    }
    
    /**
     * 上传带路径的文件（用于文件夹上传）
     */
    private ProjectFile uploadFileWithPath(Integer projectId, MultipartFile file, Integer baseParentId, Integer uploaderId) throws IOException {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        originalFileName = originalFileName.replace("\\", "/");
        int lastSeparatorIndex = originalFileName.lastIndexOf("/");
        String relativePath = originalFileName.substring(0, lastSeparatorIndex);
        String fileName = originalFileName.substring(lastSeparatorIndex + 1);

        Integer targetParentId = createDirectoriesRecursively(projectId, relativePath, baseParentId, uploaderId);
        return saveUploadedFile(projectId, file, fileName, targetParentId, uploaderId);
    }

    /**
     * 递归创建目录结构（带缓存优化）
     */
    private Integer createDirectoriesRecursively(Integer projectId, String relativePath, Integer currentParentId, Integer uploaderId) {
        String[] pathParts = relativePath.split("/");
        Integer currentDirId = currentParentId;
        Cache directoryCache = cacheManager.getCache("directoryCache");

        for (String dirName : pathParts) {
            if (dirName == null || dirName.trim().isEmpty()) {
                continue;
            }

            String trimmedDirName = dirName.trim();
            String filePath = buildFilePath(currentDirId);
            String cacheKey = projectId + ":" + filePath + ":" + trimmedDirName;
            
            // 先查缓存
            Integer cachedDirId = getCachedDirectoryId(directoryCache, cacheKey);
            if (cachedDirId != null) {
                currentDirId = cachedDirId;
                continue;
            }

            // 检查目录是否已存在
            ProjectFile existing = projectFileMapper.selectByPath(projectId, filePath, trimmedDirName);
            if (existing != null && Constants.File.TYPE_DIRECTORY.equals(existing.getIsDir())) {
                currentDirId = existing.getId();
            } else {
                // 创建新目录
                currentDirId = createNewDirectory(projectId, trimmedDirName, currentDirId, uploaderId, filePath);
            }
            
            // 加入缓存
            putCache(directoryCache, cacheKey, currentDirId);
        }

        return currentDirId;
    }
    
    /**
     * 从缓存获取目录ID
     */
    private Integer getCachedDirectoryId(Cache cache, String key) {
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? (Integer) wrapper.get() : null;
    }
    
    /**
     * 写入缓存
     */
    private void putCache(Cache cache, String key, Integer value) {
        if (cache != null) {
            cache.put(key, value);
        }
    }
    
    /**
     * 创建新目录
     */
    private Integer createNewDirectory(Integer projectId, String dirName, Integer parentId, Integer uploaderId, String filePath) {
        ProjectFile existing = projectFileMapper.selectByPath(projectId, filePath, dirName);
        if (existing != null) {
            throw new RuntimeException("该目录下已存在同名目录");
        }
        
        ProjectFile directory = new ProjectFile();
        directory.setProjectId(projectId);
        directory.setFileName(dirName);
        directory.setFilePath(filePath);
        directory.setIsDir(Constants.File.TYPE_DIRECTORY);
        directory.setParentId(parentId);
        directory.setUploaderId(uploaderId);
        directory.setStorageUrl("");
        
        projectFileMapper.insert(directory);
        log.info("目录创建成功，目录 ID: {}", directory.getId());
        
        return directory.getId();
    }

    /**
     * 保存上传的文件到 OSS 和数据库
     */
    private ProjectFile saveUploadedFile(Integer projectId, MultipartFile file, String fileName, Integer parentId, Integer uploaderId) throws IOException {
        String fileExtension = getFileExtension(fileName);
        
        // 上传到 OSS
        String ossUrl = uploadToOss(file);
        
        // 创建数据库记录
        ProjectFile projectFile = new ProjectFile();
        projectFile.setProjectId(projectId);
        projectFile.setFileName(fileName);
        projectFile.setFilePath(buildFilePath(parentId));
        projectFile.setFileSize(file.getSize());
        projectFile.setFileType(fileExtension);
        projectFile.setStorageUrl(ossUrl);
        projectFile.setIsDir(Constants.File.TYPE_FILE);
        projectFile.setParentId(parentId);
        projectFile.setUploaderId(uploaderId);

        projectFileMapper.insert(projectFile);
        log.info("文件上传完成: {}", fileName);

        return projectFile;
    }
    
    /**
     * 上传文件到 OSS
     */
    private String uploadToOss(MultipartFile file) {
        long fileSize = file.getSize();
        try {
            String ossUrl = fileSize > LARGE_FILE_THRESHOLD 
                ? ossUtil.multipartUpload(file, null)
                : ossUtil.upload(file);
            
            if (ossUrl == null || ossUrl.trim().isEmpty()) {
                throw new RuntimeException("OSS 返回的 URL 为空");
            }
            return ossUrl;
        } catch (Exception e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("OSS 上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除项目的所有文件（包括数据库记录和 OSS 文件）
     * @param projectId 项目 ID
     * @return 删除的文件数量
     */
    /**
     * 获取项目的所有文件（用于构建完整的树形结构）
     * @param projectId 项目 ID
     * @return 文件列表
     */
    public List<ProjectFile> getAllFiles(Integer projectId) {
        log.debug("获取项目所有文件，项目 ID: {}", projectId);
        return projectFileMapper.selectByProjectId(projectId);
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

