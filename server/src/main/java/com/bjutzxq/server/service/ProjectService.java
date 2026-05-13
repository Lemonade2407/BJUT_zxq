package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.*;
import com.bjutzxq.server.mapper.*;
import com.bjutzxq.server.util.OssUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
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
    private UserMapper userMapper;
    
    @Autowired
    private StarMapper starMapper;
    
    @Autowired
    private WatchMapper watchMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProjectFileMapper projectFileMapper;
    
    @Autowired
    private OssUtil ossUtil;
    
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
        if (tagIds != null) {
            projectTagService.setProjectTags(project.getId(), tagIds);
            log.info("项目标签设置成功，项目 ID: {}, 标签数量: {}", project.getId(), tagIds.size());
        }
        
        return project;
    }

    /**
     * 修改项目信息
     * @param project 项目信息（必须包含 id）
     * @return 修改后的项目信息
     * @throws RuntimeException 项目不存在或无权限修改时抛出
     */
    @CacheEvict(value = "projectInfo", key = "#project.id")
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
        
        // 5. 清理 Redis 缓存
        try {
            redisTemplate.delete("pv:project:" + id);
            redisTemplate.delete("cache:trending:projects");
        } catch (Exception e) {
            log.warn("清理 Redis 缓存失败: {}", e.getMessage());
        }

        // 6. 最后删除项目本身
        int rows = projectMapper.deleteById(id);
        if (rows == 0) {
            Project check = projectMapper.selectById(id);
            if (check == null) {
                log.info("项目已被级联删除，ID: {}", id);
                return true;
            }
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
    @Cacheable(value = "projectInfo", key = "#id")
    public Project selectById(Integer id) {
        log.debug("查询项目详情，项目 ID: {}", id);
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
        
        // 获取当前用户 ID
        Integer userId = com.bjutzxq.server.context.UserIdContext.getCurrentUserId();
        
        // 尝试从缓存获取（只缓存静态数据：标签、作者信息）
        String cacheKey = "cache:public:projects:p" + pageNum + ":s" + pageSize;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("命中项目广场缓存: {}", cacheKey);
                List<Project> cachedProjects = objectMapper.readValue(cached, new TypeReference<List<Project>>() {});
                // 补充用户交互状态
                return enrichUserInteraction(cachedProjects, userId);
            }
        } catch (Exception e) {
            log.warn("项目广场缓存异常: {}", e.getMessage());
        }
        
        // 缓存未命中，查询数据库
        PageHelper.startPage(pageNum, pageSize);
        List<Project> projects = projectMapper.selectPublicProjects();
        
        // 丰富静态数据（标签、作者）
        projects = enrichStaticData(projects);
        
        // 写入缓存（5分钟过期）
        try {
            String json = objectMapper.writeValueAsString(projects);
            redisTemplate.opsForValue().set(cacheKey, json, 5, java.util.concurrent.TimeUnit.MINUTES);
            log.debug("项目广场缓存已设置: {}", cacheKey);
        } catch (Exception e) {
            log.warn("缓存项目广场失败: {}", e.getMessage());
        }
        
        // 补充用户交互状态
        return enrichUserInteraction(projects, userId);
    }
    
    /**
     * 只丰富静态数据（标签、作者信息），不包含用户交互状态
     * @param projects 项目列表
     * @return 丰富后的项目列表
     */
    private List<Project> enrichStaticData(List<Project> projects) {
        if (projects == null || projects.isEmpty()) return projects;

        // 批量查询项目标签
        List<Integer> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        Map<Integer, List<Tag>> tagMap = projectTagService.getProjectTagsBatch(projectIds);
        Map<Integer, String> ownerNameMap = new java.util.HashMap<>();
        Map<Integer, String> ownerClassNameMap = new java.util.HashMap<>();

        // 批量查询所有者
        List<Integer> ownerIds = projects.stream().map(Project::getOwnerId).distinct().collect(Collectors.toList());
        if (!ownerIds.isEmpty()) {
            List<User> owners = userMapper.selectBatchIds(ownerIds);
            for (User o : owners) {
                ownerNameMap.put(o.getId(), o.getUsername());
                ownerClassNameMap.put(o.getId(), o.getClassName());
            }
        }

        for (Project project : projects) {
            project.setTags(tagMap.getOrDefault(project.getId(), List.of()));
            String ownerName = ownerNameMap.get(project.getOwnerId());
            project.setOwnerUsername(ownerName != null ? ownerName : "未知用户");
            project.setOwnerClassName(ownerClassNameMap.get(project.getOwnerId()));
            project.setAuthor(ownerName != null ? ownerName : "未知用户");
            // 不设置 isStarred 和 isWatched，留给 enrichUserInteraction 处理
        }
        return projects;
    }
    
    /**
     * 补充用户交互状态（点赞/关注）
     * @param projects 项目列表
     * @param userId 当前用户 ID
     * @return 补充后的项目列表
     */
    private List<Project> enrichUserInteraction(List<Project> projects, Integer userId) {
        if (projects == null || projects.isEmpty()) return projects;
        
        if (userId != null) {
            List<Integer> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
            java.util.Set<Integer> starredIds = new java.util.HashSet<>(
                starMapper.selectStarredProjectIds(userId, projectIds)
            );
            java.util.Set<Integer> watchedIds = new java.util.HashSet<>(
                watchMapper.selectWatchedProjectIds(userId, projectIds)
            );
            
            for (Project project : projects) {
                project.setIsStarred(starredIds.contains(project.getId()));
                project.setIsWatched(watchedIds.contains(project.getId()));
            }
        } else {
            // 未登录用户，默认为 false
            for (Project project : projects) {
                project.setIsStarred(false);
                project.setIsWatched(false);
            }
        }
        
        return projects;
    }
    
    /**
     * 按班级和课程筛选项目（用于教学管理，支持分页）
     * @param className 班级名称（可选，支持模糊查询）
     * @param courseName 课程名称（可选）
     * @param projectType 项目类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 项目列表
     */
    public List<Project> selectByClassAndCourse(
            String className, 
            String courseName, 
            String projectType,
            Integer pageNum, 
            Integer pageSize) {
        log.info("按班级和课程筛选项目，班级：{}, 课程：{}, 类型：{}, 页码：{}, 每页数量：{}",
                className, courseName, projectType, pageNum, pageSize);
        
        // 使用 PageHelper 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        
        // 调用 Mapper 查询
        List<Project> projects = projectMapper.selectByClassAndCourse(
            className != null ? className.trim() : null,
            courseName != null ? courseName.trim() : null,
            projectType
        );
        
        log.info("筛选结果数量：{}", projects.size());
        return projects;
    }
    
    /**
     * 获取按班级和课程筛选的项目总数
     * @param className 班级名称（可选，支持模糊查询）
     * @param courseName 课程名称（可选）
     * @param projectType 项目类型（可选）
     * @return 项目总数
     */
    public long countByClassAndCourse(String className, String courseName, String projectType) {
        log.debug("统计筛选项目总数，班级：{}, 课程：{}, 类型：{}", className, courseName, projectType);
        return projectMapper.countByClassAndCourse(
            className != null ? className.trim() : null,
            courseName != null ? courseName.trim() : null,
            projectType
        );
    }
    
    /**
     * 统计符合条件的项目总数（按名称）
     * @param name 项目名称
     * @return 项目总数
     */
    public long countByName(String name) {
        log.debug("统计项目名称总数，名称：{}", name);
        return projectMapper.countByName(name);
    }
    
    /**
     * 统计用户的项目总数
     * @param userId 用户 ID
     * @return 项目总数
     */
    public long countByUserId(Integer userId) {
        log.debug("统计用户项目总数，用户 ID：{}", userId);
        return projectMapper.countByUserId(userId);
    }
    
    /**
     * 统计标签下的项目总数
     * @param tagId 标签 ID
     * @return 项目总数
     */
    public long countByTagId(Integer tagId) {
        log.debug("统计标签项目总数，标签 ID：{}", tagId);
        return projectMapper.countByTagId(tagId);
    }
    
    /**
     * 统计公开项目总数
     * @return 项目总数
     */
    public long countPublicProjects() {
        log.debug("统计公开项目总数");
        return projectMapper.countPublicProjects();
    }
    
    /**
     * 获取按班级和课程筛选的所有项目ID（用于批量下载）
     * @param className 班级名称（可选，支持模糊查询）
     * @param courseName 课程名称（可选）
     * @param projectType 项目类型（可选）
     * @return 项目ID列表
     */
    public List<Integer> getProjectIdsByClassAndCourse(String className, String courseName, String projectType) {
        log.info("获取筛选项目ID列表，班级：{}, 课程：{}, 类型：{}", className, courseName, projectType);
        return projectMapper.selectIdsByClassAndCourse(
            className != null ? className.trim() : null,
            courseName != null ? courseName.trim() : null,
            projectType
        );
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
        
        // 设置作者名称
        if (project.getOwnerId() != null) {
            User owner = userMapper.selectById(project.getOwnerId());
            if (owner != null) {
                project.setAuthor(owner.getUsername());
            } else {
                project.setAuthor("未知用户");
            }
        }
        
        // 如果提供了用户 ID，查询用户的交互状态
        if (userId != null) {
            // 检查是否已点赞
            Star star = starMapper.selectByUserIdAndProjectId(userId, project.getId());
            project.setIsStarred(star != null);
            
            // 检查是否已关注
            Watch watch = watchMapper.selectByUserIdAndProjectId(userId, project.getId());
            project.setIsWatched(watch != null);
        } else {
            // 未登录用户，默认为 false
            project.setIsStarred(false);
            project.setIsWatched(false);
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
        if (projects == null || projects.isEmpty()) return projects;

        // 批量查询项目标签
        List<Integer> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        Map<Integer, List<Tag>> tagMap = projectTagService.getProjectTagsBatch(projectIds);
        Map<Integer, String> ownerNameMap = new java.util.HashMap<>();
        Map<Integer, String> ownerClassNameMap = new java.util.HashMap<>();

        // 批量查询所有者
        List<Integer> ownerIds = projects.stream().map(Project::getOwnerId).distinct().collect(Collectors.toList());
        if (!ownerIds.isEmpty()) {
            List<User> owners = userMapper.selectBatchIds(ownerIds);
            for (User o : owners) {
                ownerNameMap.put(o.getId(), o.getUsername());
                ownerClassNameMap.put(o.getId(), o.getClassName());
            }
        }

        // 批量查询用户的点赞/关注状态
        java.util.Set<Integer> starredIds = java.util.Collections.emptySet();
        java.util.Set<Integer> watchedIds = java.util.Collections.emptySet();
        if (userId != null) {
            starredIds = new java.util.HashSet<>(starMapper.selectStarredProjectIds(userId, projectIds));
            watchedIds = new java.util.HashSet<>(watchMapper.selectWatchedProjectIds(userId, projectIds));
        }

        for (Project project : projects) {
            project.setTags(tagMap.getOrDefault(project.getId(), List.of()));
            String ownerName = ownerNameMap.get(project.getOwnerId());
            project.setOwnerUsername(ownerName != null ? ownerName : "未知用户");
            project.setOwnerClassName(ownerClassNameMap.get(project.getOwnerId()));
            project.setAuthor(ownerName != null ? ownerName : "未知用户");
            project.setIsStarred(starredIds.contains(project.getId()));
            project.setIsWatched(watchedIds.contains(project.getId()));
        }
        return projects;
    }
    
    /**
     * 获取热门项目（按浏览量排序）
     * @param limit 限制数量
     * @return 热门项目列表
     */
    public List<Project> getTrendingProjects(Integer limit) {
        if (limit == null || limit < 1) limit = 10;
        if (limit > 50) limit = 50;

        String cacheKey = "cache:trending:projects";
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Project>>() {});
            }
        } catch (Exception e) {
            log.warn("热门项目缓存异常: {}", e.getMessage());
        }

        List<Project> projects = projectMapper.selectTrendingProjects(limit);
        projects = enrichProjects(projects, null);

        try {
            String json = objectMapper.writeValueAsString(projects);
            redisTemplate.opsForValue().set(cacheKey, json, 3, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存热门项目失败: {}", e.getMessage());
        }
        return projects;
    }
    
    /**
     * 增加项目浏览次数
     * @param id 项目 ID
     */
    public void incrementViewCount(Integer id) {
        redisTemplate.opsForValue().increment("pv:project:" + id);
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
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("创建临时目录失败: {}", tempDir);
                throw new IOException("创建临时目录失败");
            }
            
            // 2. 生成 ZIP 文件路径
            String zipFileName = projectName + "_" + projectId + "_" + System.currentTimeMillis() + ".zip";
            Path zipPath = Paths.get(tempDir, zipFileName);
            
            // 3. 获取项目的所有文件
            List<com.bjutzxq.pojo.entity.ProjectFile> projectFiles = projectFileMapper.selectByProjectId(projectId);
            
            if (projectFiles == null || projectFiles.isEmpty()) {
                log.warn("项目没有文件，创建空 ZIP");
                createEmptyZip(zipPath);
                return zipPath;
            }
            
            // 4. 过滤出需要打包的文件（非目录且有 URL）
            List<com.bjutzxq.pojo.entity.ProjectFile> filesToPack = projectFiles.stream()
                .filter(f -> f.getIsDir() == null || f.getIsDir() == 0)
                .filter(f -> f.getStorageUrl() != null && !f.getStorageUrl().trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
            
            if (filesToPack.isEmpty()) {
                log.warn("项目没有可下载的文件，创建空 ZIP");
                createEmptyZip(zipPath);
                return zipPath;
            }
            
            log.info("找到 {} 个文件，开始并行打包...", filesToPack.size());
            
            // 5. 并行下载并打包
            int successCount = downloadAndPackFiles(filesToPack, zipPath);
            
            log.info("项目打包完成，成功: {}, ZIP 文件: {}", successCount, zipPath);
            return zipPath;
            
        } catch (IOException e) {
            log.error("项目打包失败：{}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 创建空 ZIP 文件
     */
    private void createEmptyZip(Path zipPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            // 空 ZIP
        }
    }
    
    /**
     * 并行下载文件并打包到 ZIP
     * @return 成功打包的文件数量
     */
    private int downloadAndPackFiles(List<com.bjutzxq.pojo.entity.ProjectFile> filesToPack, Path zipPath) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, filesToPack.size()));
        
        try {
            // 1. 并行下载所有文件
            List<CompletableFuture<FileContent>> futures = filesToPack.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] content = ossUtil.download(file.getStorageUrl());
                        return new FileContent(file, content);
                    } catch (IOException e) {
                        log.error("下载文件失败: {}, 错误: {}", file.getFileName(), e.getMessage());
                        return new FileContent(file, null);
                    }
                }, executor))
                .toList();
            
            // 2. 等待所有下载完成并写入 ZIP
            int successCount = 0;
            try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                for (CompletableFuture<FileContent> future : futures) {
                    FileContent fileContent = future.join();
                    
                    if (fileContent.content != null) {
                        String entryPath = buildZipEntryPath(fileContent.file);
                        ZipEntry entry = new ZipEntry(entryPath);
                        zos.putNextEntry(entry);
                        zos.write(fileContent.content);
                        zos.closeEntry();
                        successCount++;
                    }
                }
            }
            
            return successCount;
            
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * 构建 ZIP 条目路径
     */
    private String buildZipEntryPath(com.bjutzxq.pojo.entity.ProjectFile file) {
        String entryPath = file.getFilePath();
        if (entryPath != null && !entryPath.trim().isEmpty()) {
            entryPath = entryPath + "/" + file.getFileName();
        } else {
            entryPath = file.getFileName();
        }
        return entryPath;
    }

    /**
         * 文件内容封装类
         */
        private record FileContent(ProjectFile file, byte[] content) {
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
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 2. 验证文件类型（只允许 PDF、Word 等文档格式）
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedExtensions = List.of("pdf", "doc", "docx", "txt", "md");
        if (!allowedExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + fileExtension + "，仅支持 PDF、Word、TXT、Markdown格式");
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
    public User getUserById(Integer userId) {
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
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("创建临时目录失败: {}", tempDir);
                throw new IOException("创建临时目录失败");
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
                        com.bjutzxq.pojo.entity.User owner = userMapper.selectById(project.getOwnerId());
                        if (owner == null) {
                            log.warn("项目所有者不存在，跳过，项目 ID: {}", projectId);
                            failCount++;
                            continue;
                        }
                        
                        // 4.3 构建文件夹名称：学号 真实姓名 项目名称
                        String folderName = owner.getEmployeeId() + " " + owner.getRealName() + " " + project.getName();
                        log.info("处理项目: {}", folderName);
                        
                        // 4.4 获取项目的所有文件
                        List<com.bjutzxq.pojo.entity.ProjectFile> projectFiles = projectFileMapper.selectByProjectId(projectId);
                        
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
                        List<com.bjutzxq.pojo.entity.ProjectFile> filesToPack = projectFiles.stream()
                            .filter(f -> f.getIsDir() == null || f.getIsDir() == 0)  // 只保留文件
                            .filter(f -> f.getStorageUrl() != null && !f.getStorageUrl().trim().isEmpty())  // 有 URL
                            .toList();
                        
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
                        List<com.bjutzxq.pojo.entity.ProjectFile> validFiles = new ArrayList<>();
                        
                        for (com.bjutzxq.pojo.entity.ProjectFile file : filesToPack) {
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
                                com.bjutzxq.pojo.entity.ProjectFile file = validFiles.get(i);
                                
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
