package com.bjutzxq.server.service;

import com.bjutzxq.pojo.Project;
import com.bjutzxq.pojo.Tag;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 项目服务类
 */
@Slf4j
@Service
public class ProjectService {
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private ProjectTagService projectTagService;
    
    @Autowired
    private com.bjutzxq.server.mapper.UserMapper userMapper;
    
    @Autowired
    private com.bjutzxq.server.mapper.StarMapper starMapper;
    
    @Autowired
    private com.bjutzxq.server.mapper.WatchMapper watchMapper;
    
    @Autowired
    private com.bjutzxq.server.mapper.ProjectFileMapper projectFileMapper;
    
    @Autowired
    private com.bjutzxq.server.util.OssUtil ossUtil;
    
    @Autowired
    private ProjectFileService projectFileService;

    /**
     * 新增项目
     * @param project 项目信息
     * @param tagIds 标签 ID 列表
     * @return 创建后的项目信息（包含生成的 ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public Project createProject(Project project, List<Integer> tagIds) {
        log.info("开始创建项目，项目名：{}", project.getName());
        
        // 参数验证
        if (project == null) {
            log.warn("创建项目失败：项目信息为空");
            throw new IllegalArgumentException("项目信息不能为空");
        }
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            log.warn("创建项目失败：项目名称为空");
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (project.getOwnerId() == null) {
            log.warn("创建项目失败：项目所有者 ID 为空");
            throw new IllegalArgumentException("项目所有者 ID 不能为空");
        }
        
        // 检查同一用户名下是否有同名项目
        List<Project> existingProjects = projectMapper.selectByUserId(project.getOwnerId());
        if (existingProjects != null && !existingProjects.isEmpty()) {
            for (Project existing : existingProjects) {
                if (existing.getName().equals(project.getName())) {
                    log.warn("项目名已存在：{}, 用户 ID: {}", project.getName(), project.getOwnerId());
                    throw new RuntimeException("您已创建了同名项目");
                }
            }
        }
        
        // 插入项目
        projectMapper.insert(project);
        log.info("项目创建成功，ID：{}", project.getId());
        
        // 设置标签
        if (tagIds != null && !tagIds.isEmpty()) {
            projectTagService.setProjectTags(project.getId(), tagIds);
        }
        
        return project;
    }

    /**
     * 修改项目信息
     * @param project 项目信息（必须包含 id）
     * @return 修改后的项目信息
     * @throws RuntimeException 项目不存在或无权限修改时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Project updateProject(Project project, List<Integer> tagIds) {
        log.info("开始修改项目信息，项目 ID：{}", project != null ? project.getId() : "null");
            
        // 参数验证
        if (project == null) {
            log.warn("修改项目失败：项目信息为空");
            throw new IllegalArgumentException("项目信息不能为空");
        }
        if (project.getId() == null) {
            log.warn("修改项目失败：项目 ID 为空");
            throw new IllegalArgumentException("项目 ID 不能为空");
        }
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            log.warn("修改项目失败：项目名称为空");
            throw new IllegalArgumentException("项目名称不能为空");
        }
            
        // 查询原项目信息
        Project existingProject = projectMapper.selectById(project.getId());
        if (existingProject == null) {
            log.warn("修改项目失败：项目不存在，ID: {}", project.getId());
            throw new RuntimeException("项目不存在");
        }
            
        // 验证权限（只能修改自己的项目）
        if (!existingProject.getOwnerId().equals(project.getOwnerId())) {
            log.warn("修改项目失败：无权限修改，项目 ID: {}, 当前用户 ID: {}", 
                       project.getId(), project.getOwnerId());
            throw new RuntimeException("无权限修改该项目");
        }
            
        // 如果修改了项目名，检查是否与其他项目重名
        if (project.getName() != null && !project.getName().trim().isEmpty() 
            && !project.getName().equals(existingProject.getName())) {
            List<Project> projects = projectMapper.selectByUserId(project.getOwnerId());
            for (Project p : projects) {
                if (p.getName().equals(project.getName().trim()) && !p.getId().equals(project.getId())) {
                    log.warn("修改项目失败：项目名已存在，项目名：{}", project.getName());
                    throw new RuntimeException("您已创建了同名项目");
                }
            }
        }
            
        // 设置需要更新的字段（只更新非空字段）
        if (project.getName() != null && !project.getName().trim().isEmpty()) {
            existingProject.setName(project.getName().trim());
        }
        if (project.getDescription() != null) {
            existingProject.setDescription(project.getDescription());
        }
        if (project.getVisibility() != null) {
            existingProject.setVisibility(project.getVisibility());
        }
        if (project.getProjectType() != null) {
            existingProject.setProjectType(project.getProjectType());
        }
        if (project.getCourseName() != null) {
            existingProject.setCourseName(project.getCourseName());
        }
        if (project.getThesisType() != null) {
            existingProject.setThesisType(project.getThesisType());
        }
            
        // 执行更新
        int rows = projectMapper.updateById(existingProject);
        if (rows == 0) {
            log.error("修改项目失败：数据库更新失败，ID: {}", project.getId());
            throw new RuntimeException("修改项目失败");
        }
        
        // 更新标签
        if (tagIds != null) {
            projectTagService.setProjectTags(existingProject.getId(), tagIds);
            log.info("项目标签更新成功，项目 ID: {}, 标签数量: {}", existingProject.getId(), tagIds.size());
        }
            
        log.info("项目信息修改成功，ID: {}", project.getId());
        return existingProject;
    }

    /**
     * 删除项目
     * @param id 项目 ID
     * @param userId 当前用户 ID
     * @return 删除结果
     * @throws RuntimeException 项目不存在或无权限删除时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProject(Integer id, Integer userId) {
        log.info("开始删除项目，项目 ID：{}", id);
        
        // 1. 验证项目存在性和权限
        Project project = projectMapper.selectById(id);
        if (project == null) {
            log.warn("删除项目失败：项目不存在，ID: {}", id);
            throw new RuntimeException("项目不存在");
        }
        if (!project.getOwnerId().equals(userId)) {
            log.warn("删除项目失败：无权限删除，项目 ID: {}, 当前用户 ID: {}", id, userId);
            throw new RuntimeException("无权限删除该项目");
        }
        
        // 2. 先删除项目关联的所有文件（会同时清理 OSS）
        try {
            int deletedFileCount = projectFileService.deleteAllProjectFiles(id);
            log.info("项目文件清理完成，删除文件数量: {}", deletedFileCount);
        } catch (Exception e) {
            log.error("删除项目文件失败，项目 ID: {}, 错误: {}", id, e.getMessage(), e);
            // 不抛出异常，继续删除项目（避免因为 OSS 删除失败导致项目无法删除）
        }
        
        // 3. 删除项目相关的标签关联
        try {
            projectTagService.removeProjectTags(id);
            log.info("项目标签关联清理完成，项目 ID: {}", id);
        } catch (Exception e) {
            log.error("删除项目标签关联失败，项目 ID: {}, 错误: {}", id, e.getMessage(), e);
        }
        
        // 4. 删除项目相关的点赞和关注记录
        try {
            starMapper.deleteByProjectId(id);
            watchMapper.deleteByProjectId(id);
            log.info("项目点赞和关注记录清理完成，项目 ID: {}", id);
        } catch (Exception e) {
            log.error("删除项目点赞/关注记录失败，项目 ID: {}, 错误: {}", id, e.getMessage(), e);
        }
        
        // 5. 最后删除项目本身
        int rows = projectMapper.deleteById(id);
        if (rows == 0) {
            log.error("删除项目失败：数据库删除失败，ID: {}", id);
            throw new RuntimeException("删除项目失败");
        }
        
        log.info("项目删除成功，ID: {}", id);
        return true;
    }

    /**
     * 根据 ID 查询项目
     * @param id 项目 ID
     * @return 项目信息
     */
    public Project selectById(Integer id) {
        log.debug("查询项目详情，项目 ID: {}", id);
        
        // 增加项目浏览量
        try {
            projectMapper.incrementViewCount(id);
            log.debug("项目浏览量 +1，项目 ID: {}", id);
        } catch (Exception e) {
            // 浏览量增加失败不影响主流程，仅记录日志
            log.warn("增加项目浏览量失败，项目 ID: {}, 错误: {}", id, e.getMessage());
        }
        
        return projectMapper.selectById(id);
    }

    /**
     * 根据名称查询项目（支持分页）
     * @param name 项目名称
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 项目列表
     */
    public List<Project> selectByName(String name, Integer pageNum, Integer pageSize) {
        log.info("按名称搜索项目，名称：{}, 页码：{}, 每页数量：{}", name, pageNum, pageSize);
        // 使用 PageHelper 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        return projectMapper.selectByName(name);
    }

    /**
     * 根据用户 ID 查询项目（支持分页）
     * @param userId 用户 ID
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 项目列表
     */
    public List<Project> selectByUserId(Integer userId, Integer pageNum, Integer pageSize) {
        log.info("按用户 ID 查询项目，用户 ID: {}, 页码：{}, 每页数量：{}", userId, pageNum, pageSize);
        // 使用 PageHelper 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        return projectMapper.selectByUserId(userId);
    }
    
    /**
     * 根据标签 ID 查询项目（支持分页）
     * @param tagId 标签 ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 项目列表
     */
    public List<Project> selectByTagId(Integer tagId, Integer pageNum, Integer pageSize) {
        log.info("按标签 ID 查询项目，标签 ID: {}, 页码：{}, 每页数量：{}", tagId, pageNum, pageSize);
        PageHelper.startPage(pageNum, pageSize);
        return projectMapper.selectByTagId(tagId);
    }
    
    /**
     * 获取所有公开项目（支持分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 公开项目列表
     */
    public List<Project> getPublicProjects(Integer pageNum, Integer pageSize) {
        log.info("获取公开项目列表，页码：{}, 每页数量：{}", pageNum, pageSize);
        PageHelper.startPage(pageNum, pageSize);
        return projectMapper.selectPublicProjects();
    }

    /**
     * 丰富项目信息（添加标签、用户交互状态）
     * @param project 项目对象
     * @param userId 当前用户 ID（可为 null，表示未登录）
     * @return 丰富后的项目对象
     */
    public Project enrichProject(Project project, Integer userId) {
        if (project == null) {
            return null;
        }
        
        // 获取标签列表
        List<Tag> tags = projectTagService.getProjectTags(project.getId());
        project.setTags(tags);
        
        // 设置点赞和收藏的别名字段（用于前端显示）
        project.setLikes(project.getStarCount() != null ? project.getStarCount() : 0);
        project.setFavorites(project.getWatchCount() != null ? project.getWatchCount() : 0);
        
        // 设置作者名称
        if (project.getOwnerId() != null) {
            com.bjutzxq.pojo.User owner = userMapper.selectById(project.getOwnerId());
            if (owner != null) {
                project.setAuthor(owner.getUsername());
            } else {
                project.setAuthor("未知用户");
            }
        }
        
        // 如果提供了用户 ID，查询用户的交互状态
        if (userId != null) {
            // 检查是否已点赞
            com.bjutzxq.pojo.Star star = starMapper.selectByUserIdAndProjectId(userId, project.getId());
            project.setIsLiked(star != null);
            
            // 检查是否已收藏
            com.bjutzxq.pojo.Watch watch = watchMapper.selectByUserIdAndProjectId(userId, project.getId());
            project.setIsFavorited(watch != null);
        } else {
            // 未登录用户，默认为 false
            project.setIsLiked(false);
            project.setIsFavorited(false);
        }
        
        return project;
    }
    
    /**
     * 丰富项目列表信息
     * @param projects 项目列表
     * @param userId 当前用户 ID（可为 null）
     * @return 丰富后的项目列表
     */
    public List<Project> enrichProjects(List<Project> projects, Integer userId) {
        if (projects == null || projects.isEmpty()) {
            return projects;
        }
        
        for (Project project : projects) {
            enrichProject(project, userId);
        }
        
        return projects;
    }
    
    /**
     * 获取热门项目（按浏览量排序）
     * @param limit 限制数量
     * @return 热门项目列表
     */
    public List<Project> getTrendingProjects(Integer limit) {
        log.info("获取热门项目，限制数量：{}", limit);
        
        // 参数验证
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 50) {
            limit = 50; // 最多返回 50 个
        }
        
        List<Project> projects = projectMapper.selectTrendingProjects(limit);
        
        // 丰富项目信息（热门项目不需要用户交互状态，传 null）
        return enrichProjects(projects, null);
    }
    
    /**
     * 增加项目下载次数
     * @param id 项目 ID
     */
    public void incrementDownloadCount(Integer id) {
        log.debug("增加项目下载次数，项目 ID: {}", id);
        try {
            projectMapper.incrementDownloadCount(id);
            log.debug("项目下载次数 +1，项目 ID: {}", id);
        } catch (Exception e) {
            log.warn("增加项目下载次数失败，项目 ID: {}, 错误: {}", id, e.getMessage());
        }
    }
    
    /**
     * 将项目打包为 ZIP 文件
     * @param projectId 项目 ID
     * @param projectName 项目名称
     * @return ZIP 文件路径
     */
    public Path packageProjectToZip(Integer projectId, String projectName) {
        log.info("开始打包项目，项目 ID: {}, 项目名称: {}", projectId, projectName);
        
        try {
            // 1. 创建临时目录
            String tempDir = System.getProperty("java.io.tmpdir") + "/project_downloads/";
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 2. 生成 ZIP 文件路径
            String zipFileName = projectName + "_" + projectId + "_" + System.currentTimeMillis() + ".zip";
            Path zipPath = Paths.get(tempDir, zipFileName);
            
            // 3. 获取项目的所有文件
            List<com.bjutzxq.pojo.ProjectFile> projectFiles = projectFileMapper.selectByProjectId(projectId);
            
            if (projectFiles == null || projectFiles.isEmpty()) {
                log.warn("项目没有文件，创建空 ZIP");
                // 创建一个空的 ZIP 文件
                try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    // 空 ZIP
                }
                return zipPath;
            }
            
            log.info("找到 {} 个文件，开始打包...", projectFiles.size());
            
            // 统计目录和文件数量
            long dirCount = projectFiles.stream().filter(f -> f.getIsDir() != null && f.getIsDir() == 1).count();
            long fileCount = projectFiles.stream().filter(f -> f.getIsDir() == null || f.getIsDir() == 0).count();
            long withUrlCount = projectFiles.stream().filter(f -> f.getStorageUrl() != null && !f.getStorageUrl().trim().isEmpty()).count();
            
            log.info("统计: 总数={}, 目录={}, 文件={}, 有URL={}", 
                projectFiles.size(), dirCount, fileCount, withUrlCount);
            
            // 打印前5个非目录文件的详情
            int printed = 0;
            for (com.bjutzxq.pojo.ProjectFile f : projectFiles) {
                if ((f.getIsDir() == null || f.getIsDir() == 0) && printed < 5) {
                    log.info("示例文件 {}: name={}, storageUrl={}, filePath={}", 
                        printed+1, f.getFileName(), f.getStorageUrl(), f.getFilePath());
                    printed++;
                }
            }
            
            // 4. 创建 ZIP 文件
            try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                int successCount = 0;
                int failCount = 0;
                int skipDirCount = 0;
                int skipNoUrlCount = 0;
                
                // 收集需要打包的文件
                List<com.bjutzxq.pojo.ProjectFile> filesToPack = projectFiles.stream()
                    .filter(f -> f.getIsDir() == null || f.getIsDir() == 0)  // 只保留文件
                    .filter(f -> f.getStorageUrl() != null && !f.getStorageUrl().trim().isEmpty())  // 有 URL
                    .collect(java.util.stream.Collectors.toList());
                
                log.info("开始并行下载 {} 个文件...", filesToPack.size());
                
                // 并行下载所有文件（使用 CompletableFuture）
                ExecutorService executor = Executors.newFixedThreadPool(
                    Math.min(10, filesToPack.size())  // 最多10个线程
                );
                
                List<CompletableFuture<byte[]>> futures = new ArrayList<>();
                List<com.bjutzxq.pojo.ProjectFile> validFiles = new ArrayList<>();
                
                for (com.bjutzxq.pojo.ProjectFile file : filesToPack) {
                    CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            return ossUtil.download(file.getStorageUrl());
                        } catch (IOException e) {
                            log.error("下载文件失败: {}, 错误: {}", file.getFileName(), e.getMessage());
                            return null;
                        }
                    }, executor);
                    
                    futures.add(future);
                    validFiles.add(file);
                }
                
                // 等待所有下载完成并写入 ZIP
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        byte[] fileContent = futures.get(i).join();
                        com.bjutzxq.pojo.ProjectFile file = validFiles.get(i);
                        
                        if (fileContent == null) {
                            failCount++;
                            continue;
                        }
                        
                        // 构建文件在 ZIP 中的路径
                        String entryPath = file.getFilePath();
                        if (entryPath != null && !entryPath.trim().isEmpty()) {
                            entryPath = entryPath + "/" + file.getFileName();
                        } else {
                            entryPath = file.getFileName();
                        }
                        
                        // 添加到 ZIP
                        ZipEntry entry = new ZipEntry(entryPath);
                        zos.putNextEntry(entry);
                        zos.write(fileContent);
                        zos.closeEntry();
                        
                        successCount++;
                    } catch (Exception e) {
                        log.error("写入 ZIP 失败: {}, 错误: {}", validFiles.get(i).getFileName(), e.getMessage());
                        failCount++;
                    }
                }
                
                // 关闭线程池
                executor.shutdown();
                
                log.info("项目打包完成，成功: {}, 失败: {}, ZIP 文件: {}", 
                    successCount, failCount, zipPath);
                return zipPath;
            }
        } catch (IOException e) {
            log.error("项目打包失败：{}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 判断用户是否是项目所有者
     * @param projectId 项目 ID
     * @param userId 用户 ID
     * @return true-是所有者，false-不是所有者
     */
    public boolean isProjectOwner(Integer projectId, Integer userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        
        return project.getOwnerId().equals(userId);
    }
    
    /**
     * 上传项目文档（支持覆盖上传）
     * @param projectId 项目 ID
     * @param file 上传的文件
     * @return 文档 URL
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadProjectDocument(Integer projectId, org.springframework.web.multipart.MultipartFile file) {
        log.info("开始上传项目文档，项目 ID: {}, 文件名: {}", projectId, file.getOriginalFilename());
        
        // 1. 验证文件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 2. 验证文件类型（只允许 PDF、Word 等文档格式）
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedExtensions = List.of("pdf", "doc", "docx", "txt", "md", "ppt", "pptx");
        if (!allowedExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + fileExtension + "，仅支持 PDF、Word、TXT、Markdown、PPT 格式");
        }
        
        // 3. 获取原项目信息
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        
        // 4. 如果已有文档，先删除旧文档
        if (project.getDocumentUrl() != null && !project.getDocumentUrl().isEmpty()) {
            log.info("检测到旧文档，准备删除: {}", project.getDocumentUrl());
            try {
                ossUtil.delete(project.getDocumentUrl());
                log.info("旧文档删除成功");
            } catch (Exception e) {
                log.warn("删除旧文档失败: {}", e.getMessage());
                // 不抛出异常，继续上传新文档
            }
        }
        
        // 5. 上传新文档到 OSS
        String documentUrl;
        try {
            documentUrl = ossUtil.upload(file, "documents");
            log.info("文档上传成功: {}", documentUrl);
        } catch (IOException e) {
            log.error("文档上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档上传失败: " + e.getMessage());
        }
        
        // 6. 更新项目的 document_url 字段
        project.setDocumentUrl(documentUrl);
        int rows = projectMapper.updateById(project);
        if (rows == 0) {
            log.error("更新项目文档 URL 失败，项目 ID: {}", projectId);
            throw new RuntimeException("更新项目文档 URL 失败");
        }
        
        log.info("项目文档上传完成，项目 ID: {}, URL: {}", projectId, documentUrl);
        return documentUrl;
    }
    
    /**
     * 删除项目文档
     * @param projectId 项目 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectDocument(Integer projectId) {
        log.info("开始删除项目文档，项目 ID: {}", projectId);
        
        // 1. 获取项目信息
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        
        // 2. 如果没有文档，直接返回
        if (project.getDocumentUrl() == null || project.getDocumentUrl().isEmpty()) {
            log.info("项目暂无文档，无需删除");
            return;
        }
        
        // 3. 删除 OSS 上的文件
        String documentUrl = project.getDocumentUrl();
        try {
            ossUtil.delete(documentUrl);
            log.info("OSS 文档删除成功: {}", documentUrl);
        } catch (Exception e) {
            log.warn("删除 OSS 文档失败: {}", e.getMessage());
            // 不抛出异常，继续更新数据库
        }
        
        // 4. 更新数据库，清空 document_url
        project.setDocumentUrl(null);
        int rows = projectMapper.updateById(project);
        if (rows == 0) {
            log.error("更新项目文档 URL 失败，项目 ID: {}", projectId);
            throw new RuntimeException("更新项目文档 URL 失败");
        }
        
        log.info("项目文档删除完成，项目 ID: {}", projectId);
    }
    
    /**
     * 获取项目文档 URL
     * @param projectId 项目 ID
     * @return 文档 URL，如果没有则返回 null
     */
    public String getProjectDocumentUrl(Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        
        return project.getDocumentUrl();
    }
    
    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 扩展名（不含点号）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 根据用户 ID 获取用户信息
     * @param userId 用户 ID
     * @return 用户信息
     */
    public com.bjutzxq.pojo.User getUserById(Integer userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }
    
    /**
     * 批量打包多个学生项目为一个 ZIP 文件
     * @param projectIds 项目 ID 列表
     * @param className 班级名称
     * @param courseName 课程名称
     * @return ZIP 文件路径
     */
    public Path batchPackageProjects(List<Integer> projectIds, String className, String courseName) {
        log.info("开始批量打包项目，数量: {}, 班级: {}, 课程: {}", projectIds.size(), className, courseName);
        
        try {
            // 1. 创建临时目录
            String tempDir = System.getProperty("java.io.tmpdir") + "/batch_downloads/";
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 2. 生成 ZIP 文件路径
            String zipFileName = className + "_" + courseName + "_" + System.currentTimeMillis() + ".zip";
            Path zipPath = Paths.get(tempDir, zipFileName);
            
            // 3. 创建 ZIP 文件
            try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                int successCount = 0;
                int failCount = 0;
                
                // 4. 遍历每个项目
                for (Integer projectId : projectIds) {
                    try {
                        // 4.1 获取项目信息
                        Project project = projectMapper.selectById(projectId);
                        if (project == null) {
                            log.warn("项目不存在，跳过，ID: {}", projectId);
                            failCount++;
                            continue;
                        }
                        
                        // 4.2 获取项目所有者信息（学号和姓名）
                        com.bjutzxq.pojo.User owner = userMapper.selectById(project.getOwnerId());
                        if (owner == null) {
                            log.warn("项目所有者不存在，跳过，项目 ID: {}", projectId);
                            failCount++;
                            continue;
                        }
                        
                        // 4.3 构建文件夹名称：学号 真实姓名 项目名称
                        String folderName = owner.getEmployeeId() + " " + owner.getRealName() + " " + project.getName();
                        log.info("处理项目: {}", folderName);
                        
                        // 4.4 获取项目的所有文件
                        List<com.bjutzxq.pojo.ProjectFile> projectFiles = projectFileMapper.selectByProjectId(projectId);
                        
                        if (projectFiles == null || projectFiles.isEmpty()) {
                            log.warn("项目没有文件，创建空文件夹: {}", folderName);
                            // 创建一个空目录条目
                            ZipEntry dirEntry = new ZipEntry(folderName + "/");
                            zos.putNextEntry(dirEntry);
                            zos.closeEntry();
                            successCount++;
                            continue;
                        }
                        
                        // 4.5 收集需要打包的文件
                        List<com.bjutzxq.pojo.ProjectFile> filesToPack = projectFiles.stream()
                            .filter(f -> f.getIsDir() == null || f.getIsDir() == 0)  // 只保留文件
                            .filter(f -> f.getStorageUrl() != null && !f.getStorageUrl().trim().isEmpty())  // 有 URL
                            .collect(java.util.stream.Collectors.toList());
                        
                        if (filesToPack.isEmpty()) {
                            log.warn("项目没有可下载的文件，跳过: {}", folderName);
                            failCount++;
                            continue;
                        }
                        
                        // 4.6 并行下载所有文件
                        ExecutorService executor = Executors.newFixedThreadPool(
                            Math.min(10, filesToPack.size())
                        );
                        
                        List<CompletableFuture<byte[]>> futures = new ArrayList<>();
                        List<com.bjutzxq.pojo.ProjectFile> validFiles = new ArrayList<>();
                        
                        for (com.bjutzxq.pojo.ProjectFile file : filesToPack) {
                            CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                                try {
                                    return ossUtil.download(file.getStorageUrl());
                                } catch (IOException e) {
                                    log.error("下载文件失败: {}, 错误: {}", file.getFileName(), e.getMessage());
                                    return null;
                                }
                            }, executor);
                            
                            futures.add(future);
                            validFiles.add(file);
                        }
                        
                        // 4.7 等待所有下载完成并写入 ZIP
                        for (int i = 0; i < futures.size(); i++) {
                            try {
                                byte[] fileContent = futures.get(i).join();
                                com.bjutzxq.pojo.ProjectFile file = validFiles.get(i);
                                
                                if (fileContent == null) {
                                    continue;
                                }
                                
                                // 构建文件在 ZIP 中的路径（包含文件夹前缀）
                                String entryPath = folderName + "/";
                                if (file.getFilePath() != null && !file.getFilePath().trim().isEmpty()) {
                                    entryPath += file.getFilePath() + "/";
                                }
                                entryPath += file.getFileName();
                                
                                // 添加到 ZIP
                                ZipEntry entry = new ZipEntry(entryPath);
                                zos.putNextEntry(entry);
                                zos.write(fileContent);
                                zos.closeEntry();
                            } catch (Exception e) {
                                log.error("写入 ZIP 失败: {}, 错误: {}", validFiles.get(i).getFileName(), e.getMessage());
                            }
                        }
                        
                        // 关闭线程池
                        executor.shutdown();
                        
                        // 4.8 添加项目文档（如果有）
                        if (project.getDocumentUrl() != null && !project.getDocumentUrl().trim().isEmpty()) {
                            try {
                                byte[] docContent = ossUtil.download(project.getDocumentUrl());
                                String docExtension = getFileExtensionFromUrl(project.getDocumentUrl());
                                String docName = folderName + "/项目文档." + docExtension;
                                
                                ZipEntry docEntry = new ZipEntry(docName);
                                zos.putNextEntry(docEntry);
                                zos.write(docContent);
                                zos.closeEntry();
                                
                                log.info("项目文档已添加: {}", docName);
                            } catch (Exception e) {
                                log.warn("下载项目文档失败: {}", e.getMessage());
                            }
                        }
                        
                        successCount++;
                        log.info("项目打包成功: {}", folderName);
                        
                    } catch (Exception e) {
                        log.error("处理项目失败，项目 ID: {}, 错误: {}", projectId, e.getMessage(), e);
                        failCount++;
                    }
                }
                
                log.info("批量打包完成，成功: {}, 失败: {}, ZIP 文件: {}", successCount, failCount, zipPath);
                return zipPath;
            }
        } catch (IOException e) {
            log.error("批量打包失败：{}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从 URL 中提取文件扩展名
     * @param url 文件 URL
     * @return 扩展名
     */
    private String getFileExtensionFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "docx";
        }
        int lastDotIndex = url.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < url.length() - 1) {
            String ext = url.substring(lastDotIndex + 1);
            // 去除可能的查询参数
            int queryIndex = ext.indexOf('?');
            if (queryIndex > 0) {
                ext = ext.substring(0, queryIndex);
            }
            return ext;
        }
        return "docx";
    }
}
