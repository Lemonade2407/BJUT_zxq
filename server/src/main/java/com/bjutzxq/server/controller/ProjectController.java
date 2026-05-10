package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.*;
import com.bjutzxq.pojo.vo.*;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.ProjectService;
import com.bjutzxq.server.util.DtoConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
             allowedHeaders = "*", 
             allowCredentials = "true",
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ProjectController {
    @Autowired
    private ProjectService projectService;
    

    /**
     * 创建项目
     * POST /api/projects/create
     */
    @PostMapping("/create")
    public Result<ProjectVO> createProject(
            @Valid @RequestBody ProjectDTO projectRequest) {
        log.info("收到项目创建请求，项目名：{}", projectRequest.getName());
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();

        // 2. 构建项目对象
        Project project = buildProjectFromRequest(projectRequest, userId);

        // 3. 创建项目
        Project newProject = projectService.createProject(project, projectRequest.getTagIds());
        
        // 4. 丰富项目信息
        newProject = projectService.enrichProject(newProject, userId);
        
        // 5. 使用工具类转换为 VO
        ProjectVO vo = DtoConverter.toProjectResponse(newProject);
            
        log.info("项目创建成功，ID：{}", newProject.getId());
        return Result.success("项目创建成功", vo);
    }
    
    /**
     * 修改项目信息
     * PUT /api/projects/{id}
     */
    @PutMapping("/{id}")
    public Result<ProjectVO> updateProject(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectDTO projectRequest) {
        log.info("收到修改项目信息请求，项目 ID：{}", id);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 构建项目对象
        Project project = buildProjectFromRequest(projectRequest, userId);
        project.setId(id);
        
        // 3. 更新项目
        Project updatedProject = projectService.updateProject(project, projectRequest.getTagIds());
        
        // 4. 丰富项目信息
        updatedProject = projectService.enrichProject(updatedProject, userId);
        
        // 5. 使用工具类转换为 VO
        ProjectVO vo = DtoConverter.toProjectResponse(updatedProject);
        
        log.info("项目信息修改成功，ID：{}", updatedProject.getId());
        return Result.success("项目信息修改成功", vo);
    }
    
    /**
     * 删除项目
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteProject(
            @PathVariable Integer id) {
        log.info("收到删除项目请求，项目 ID：{}", id);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 删除项目
        boolean result = projectService.deleteProject(id, userId);
        
        log.info("项目删除成功，ID：{}", id);
        return Result.success("项目删除成功", result);
    }

    /**
     * 获取热门项目（按浏览量排序）
     * GET /api/projects/trending?limit=10
     */
    @GetMapping("/trending")
    public Result<List<Project>> getTrendingProjects(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.info("获取热门项目，限制数量：{}", limit);
        
        // 参数验证
        if (limit < 1) {
            limit = 10;
        }
        if (limit > 50) {
            limit = 50;
        }
        
        List<Project> projects = projectService.getTrendingProjects(limit);
        
        log.info("热门项目获取成功，数量：{}", projects.size());
        return Result.success("获取成功", projects);
    }
    
    /**
     * 获取所有项目类型（字典）
     * GET /api/projects/types
     */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getProjectTypes() {
        log.info("获取项目类型列表");
        
        // 定义项目类型字典
        List<Map<String, Object>> types = List.of(
            Map.of("typeCode", "COURSE", "typeName", "课程设计"),
            Map.of("typeCode", "THESIS", "typeName", "毕业设计"),
            Map.of("typeCode", "COMPETITION", "typeName", "竞赛作品"),
            Map.of("typeCode", "PERSONAL", "typeName", "个人项目"),
            Map.of("typeCode", "OTHER", "typeName", "其他")
        );
        
        log.info("项目类型列表获取成功，数量：{}", types.size());
        return Result.success("获取成功", types);
    }
    
    /**
     * 通过 ID 获取项目信息（用户查看项目详情页）
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public Result<Project> selectById(
            @PathVariable Integer id) {
        log.info("收到获取项目信息请求，项目 ID：{}", id);
        
        Integer userId = UserIdContext.getCurrentUserId();

        Project project = projectService.selectById(id);
            
        // 检查项目是否存在
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        
        // 增加项目浏览量（仅当用户访问详情页时）
        try {
            projectService.incrementViewCount(id);
            log.debug("项目浏览量 +1，项目 ID: {}", id);
        } catch (Exception e) {
            log.warn("增加项目浏览量失败，项目 ID: {}, 错误: {}", id, e.getMessage());
            // 浏览量增加失败不影响主流程
        }
        
        // 丰富项目信息
        project = projectService.enrichProject(project, userId);

        log.info("项目信息获取成功，ID：{}", project.getId());
        return Result.success("项目信息获取成功", project);
    }
    
    /**
     * 搜索项目（支持分页）
     * GET /api/projects/search/name?name=xxx&pageNum=1&pageSize=10
     */
    @GetMapping("/search/name")
    public Result<PageResult<Project>> selectByName(
            @RequestParam(required = true) String name,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("收到搜索项目请求，项目名称：{}, 页码：{}, 每页数量：{}", name, pageNum, pageSize);
        
        // 参数验证
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        
        // 分页参数验证
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        
        // 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        List<Project> projects = projectService.selectByName(name.trim(), pageNum, pageSize);
        long total = projectService.countByName(name.trim());
        
        // 丰富项目信息
        projects = projectService.enrichProjects(projects, userId);
        
        // 构建分页响应
        PageResult<Project> response = new PageResult<>(projects, total, pageNum, pageSize);
        
        log.info("项目搜索成功，名称：{}, 返回数量：{}, 总数：{}", name, projects.size(), total);
        return Result.success("项目搜索成功", response);
    }
    
    /**
     * 获取当前登录用户的所有项目（支持分页）
     * GET /api/projects/my?pageNum=1&pageSize=10
     */
    @GetMapping("/my")
    public Result<PageResult<Project>> selectMyProjects(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("收到获取我的项目请求，页码：{}, 每页数量：{}", pageNum, pageSize);
        
        // 分页参数验证
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        
        // 获取当前用户 ID
        Integer currentUserId = UserIdContext.getCurrentUserId();
        
        // 获取用户的项目
        List<Project> projects = projectService.selectByUserId(currentUserId, pageNum, pageSize);
        long total = projectService.countByUserId(currentUserId);
        
        // 丰富项目信息
        projects = projectService.enrichProjects(projects, currentUserId);
        
        // 构建分页响应
        PageResult<Project> response = new PageResult<>(projects, total, pageNum, pageSize);
        
        log.info("获取我的项目成功，用户 ID: {}, 返回数量：{}, 总数：{}", currentUserId, projects.size(), total);
        return Result.success("获取我的项目成功", response);
    }
    
    /**
     * 根据标签查询项目（支持分页）
     * GET /api/projects/tag/{tagId}?pageNum=1&pageSize=10
     */
    @GetMapping("/tag/{tagId}")
    public Result<PageResult<Project>> getProjectsByTag(
            @PathVariable Integer tagId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("按标签查询项目，标签 ID: {}, 页码：{}, 每页数量：{}", tagId, pageNum, pageSize);
        
        // 分页参数验证
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        
        List<Project> projects = projectService.selectByTagId(tagId, pageNum, pageSize);
        long total = projectService.countByTagId(tagId);
        
        // 丰富项目信息
        Integer userId = UserIdContext.getCurrentUserId();
        projects = projectService.enrichProjects(projects, userId);
        
        // 构建分页响应
        PageResult<Project> response = new PageResult<>(projects, total, pageNum, pageSize);
        
        log.info("标签项目查询成功，数量：{}, 总数：{}", projects.size(), total);
        return Result.success("查询成功", response);
    }
    
    /**
     * 获取所有公开项目（支持分页）
     * GET /api/projects/list?pageNum=1&pageSize=10
     */
    @GetMapping("/list")
    public Result<PageResult<Project>> getPublicProjects(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("获取公开项目列表，页码：{}, 每页数量：{}", pageNum, pageSize);
        
        // 分页参数验证
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        
        List<Project> projects = projectService.getPublicProjects(pageNum, pageSize);
        long total = projectService.countPublicProjects();
        
        // 丰富项目信息
        Integer userId = UserIdContext.getCurrentUserId();
        projects = projectService.enrichProjects(projects, userId);
        
        // 构建分页响应
        PageResult<Project> response = new PageResult<>(projects, total, pageNum, pageSize);
        
        log.info("公开项目列表获取成功，数量：{}, 总数：{}", projects.size(), total);
        return Result.success("获取成功", response);
    }
    
    /**
     * 按班级和课程筛选项目（教学管理专用，支持分页）
     * GET /api/projects/filter?className=xxx&courseName=xxx&projectType=COURSE&pageNum=1&pageSize=10
     */
    @GetMapping("/filter")
    public Result<PageResult<ProjectVO>> filterProjects(
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "projectType", required = false) String projectType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("筛选项目，班级：{}, 课程：{}, 类型：{}, 页码：{}, 每页数量：{}",
                className, courseName, projectType, pageNum, pageSize);
        
        // 分页参数验证
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        
        // 调用 Service 层筛选
        List<Project> projects = projectService.selectByClassAndCourse(
            className, courseName, projectType, pageNum, pageSize
        );
        
        // 获取总数
        long total = projectService.countByClassAndCourse(className, courseName, projectType);
        
        // 丰富项目信息
        Integer userId = UserIdContext.getCurrentUserId();
        projects = projectService.enrichProjects(projects, userId);
        
        // 转换为 VO
        List<ProjectVO> voList = DtoConverter.toProjectResponseList(projects);
        
        // 构建分页响应
        PageResult<ProjectVO> response = new PageResult<>(voList, total, pageNum, pageSize);
        
        log.info("项目筛选成功，数量：{}, 总数：{}", projects.size(), total);
        return Result.success("筛选成功", response);
    }
    
    /**
     * 获取筛选条件下的所有项目ID（用于批量下载）
     * GET /api/projects/filter/ids?className=xxx&courseName=xxx&projectType=COURSE
     */
    @GetMapping("/filter/ids")
    public Result<List<Integer>> getFilteredProjectIds(
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "projectType", required = false) String projectType) {
        log.info("获取筛选项目ID，班级：{}, 课程：{}, 类型：{}", className, courseName, projectType);
        
        List<Integer> projectIds = projectService.getProjectIdsByClassAndCourse(
            className, courseName, projectType
        );
        
        log.info("获取到 {} 个项目ID", projectIds.size());
        return Result.success("获取成功", projectIds);
    }
    
    /**
     * 下载项目（打包为 ZIP）
     * GET /api/projects/{id}/download
     */
    @GetMapping("/{id}/download")
    @CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
                 allowedHeaders = "*", 
                 allowCredentials = "true",
                 methods = {RequestMethod.GET, RequestMethod.OPTIONS})
    public ResponseEntity<Resource> downloadProject(
            @PathVariable Integer id) {
        log.info("收到项目下载请求，项目 ID: {}", id);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 获取项目信息
        Project project = projectService.selectById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        
        // 3. 检查权限
        if (project.getVisibility() != null && project.getVisibility() == 0) {
            if (!project.getOwnerId().equals(userId)) {
                throw new RuntimeException("无权限下载私有项目");
            }
        }
        
        // 4. 打包项目为 ZIP
        Path zipPath = projectService.packageProjectToZip(id, project.getName());
        
        if (zipPath == null || !Files.exists(zipPath)) {
            throw new RuntimeException("ZIP 文件生成失败");
        }
        
        // 5. 创建响应
        Resource resource = new FileSystemResource(zipPath.toFile());
        String fileName = project.getName() + "_" + id + ".zip";
        
        log.info("项目下载成功，项目 ID: {}, 文件名: {}", id, fileName);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
    
    /**
     * 批量下载学生项目（教师专用）
     * POST /api/projects/batch-download
     */
    @PostMapping("/batch-download")
    @CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
                 allowedHeaders = "*", 
                 allowCredentials = "true",
                 methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Resource> batchDownloadProjects(
            @Valid @RequestBody BatchDownloadDTO batchRequest) {
        log.info("收到批量下载请求，项目数量: {}, 班级: {}, 课程: {}", 
                batchRequest.getProjectIds().size(), 
                batchRequest.getClassName(), 
                batchRequest.getCourseName());
        
        // 1. 获取当前用户 ID 并验证是否为教师
        Integer userId = UserIdContext.getCurrentUserId();
        
        com.bjutzxq.pojo.entity.User user = projectService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (user.getRole() != com.bjutzxq.common.Role.TEACHER) {
            throw new RuntimeException("只有教师才能批量下载项目");
        }
        
        // 2. 批量打包项目（验证已由 @Valid 处理）
        String className = batchRequest.getClassName() != null ? batchRequest.getClassName() : "未知班级";
        String courseName = batchRequest.getCourseName() != null ? batchRequest.getCourseName() : "未知课程";
        Path zipPath = projectService.batchPackageProjects(
            batchRequest.getProjectIds(), 
            className, 
            courseName
        );
        
        if (zipPath == null || !Files.exists(zipPath)) {
            throw new RuntimeException("ZIP 文件生成失败");
        }
        
        // 4. 创建响应
        Resource resource = new FileSystemResource(zipPath.toFile());
        String fileName = className + "_" + courseName + ".zip";
        
        log.info("批量下载成功，文件名: {}", fileName);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .body(resource);
    }
    
    /**
     * 从 ProjectDTO 构建 Project 对象
     */
    private Project buildProjectFromRequest(ProjectDTO request, Integer userId) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setVisibility(request.getVisibility());
        project.setProjectType(request.getProjectType());
        project.setCourseName(request.getCourseName());
        project.setThesisType(request.getThesisType());
        project.setOwnerId(userId);
        return project;
    }
}
