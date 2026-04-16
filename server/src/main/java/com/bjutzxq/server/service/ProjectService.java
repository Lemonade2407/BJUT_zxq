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
import java.util.List;
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
        
        // TODO: 删除项目时应同时清理 OSS 上的文件存储
        // TODO: 删除项目时应清理 Git 仓库目录
        // TODO: 考虑软删除而非硬删除，保留历史记录
        
        Project project = projectMapper.selectById(id);
        if (project == null) {
            log.warn("删除项目失败：项目不存在，ID: {}", id);
            throw new RuntimeException("项目不存在");
        }
        if (!project.getOwnerId().equals(userId)) {
            log.warn("删除项目失败：无权限删除，项目 ID: {}, 当前用户 ID: {}", id, userId);
            throw new RuntimeException("无权限删除该项目");
        }
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
        
        // TODO: 优化 ZIP 打包性能，支持大项目异步处理
        // TODO: 添加 ZIP 文件大小限制
        // TODO: 清理过期的临时 ZIP 文件
        
        try {
            // 创建临时目录
            String tempDir = System.getProperty("java.io.tmpdir") + "/project_downloads/";
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成 ZIP 文件路径
            String zipFileName = projectName + "_" + projectId + "_" + System.currentTimeMillis() + ".zip";
            Path zipPath = Paths.get(tempDir, zipFileName);
            
            // 创建 ZIP 文件
            try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                // TODO: 从数据库或 Git 仓库获取项目文件列表
                // 目前先创建一个简单的 README 文件作为示例
                String readmeContent = "# " + projectName + "\n\n" +
                                      "这是一个从 BJUT-ZXQ 平台下载的项目。\n\n" +
                                      "项目 ID: " + projectId + "\n" +
                                      "下载时间: " + java.time.LocalDateTime.now() + "\n";
                
                ZipEntry entry = new ZipEntry("README.md");
                zos.putNextEntry(entry);
                zos.write(readmeContent.getBytes("UTF-8"));
                zos.closeEntry();
                
                log.info("项目打包成功，ZIP 文件路径: {}", zipPath);
                return zipPath;
            }
        } catch (IOException e) {
            log.error("项目打包失败：{}", e.getMessage(), e);
            return null;
        }
    }
}
