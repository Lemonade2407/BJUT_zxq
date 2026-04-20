package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.Project;
import com.bjutzxq.pojo.ProjectRequest;
import com.bjutzxq.server.service.ProjectService;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
     * 从 Authorization header 中解析 Token 获取当前登录用户 ID
     * @param request HTTP 请求对象
     * @return 当前登录用户 ID
     * @throws RuntimeException 如果未登录或 Token 无效
     */
    // TODO: 抽取为公共的认证拦截器或工具类，避免代码重复
    private Integer getCurrentUserId(HttpServletRequest request) {
        String authorization = request.getHeader(Constants.JWT.TOKEN_HEADER);
        
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new RuntimeException("请先登录");
        }
        
        // 提取 Token（去掉 "Bearer " 前缀）
        String token = authorization;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证并解析 Token
        if (!JwtUtil.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }
        
        return JwtUtil.getUserIdFromToken(token);
    }
    
    /**
    * 创建项目
    * POST /api/projects/create
    */
    @PostMapping("/create")
    public Result<Project> createProject(
            HttpServletRequest request,
            @RequestBody ProjectRequest projectRequest) {
        log.info("收到项目创建请求，项目名：{}", projectRequest.getName());
        
        // TODO: 添加用户创建项目数量限制
        // TODO: 添加项目名称敏感词过滤
            
        try {
            // 1. 从 request 属性中获取当前用户 ID（由拦截器设置）
            Integer userId = getCurrentUserId(request);

            // 2. 构建项目对象
            Project project = new Project();
            project.setName(projectRequest.getName());
            project.setDescription(projectRequest.getDescription());
            project.setVisibility(projectRequest.getVisibility());
            project.setOwnerId(userId);

            // 3. 创建项目
            Project newProject = projectService.createProject(project, projectRequest.getTagIds());
            
            // 4. 丰富项目信息（添加标签、用户交互状态）
            newProject = projectService.enrichProject(newProject, userId);
                
            log.info("项目创建成功，ID：{}", newProject.getId());
            return Result.success("项目创建成功", newProject);
                
        } catch (Exception e) {
            log.error("创建项目失败：{}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }
    /**
    * 修改项目信息
    * PUT /api/projects/{id}
    */
    @PutMapping("/{id}")
    public Result<Project> updateProject(
            HttpServletRequest request,
            @PathVariable Integer id,
            @RequestBody ProjectRequest projectRequest) {
        log.info("收到修改项目信息请求，项目 ID：{}", id);
        
        try {
            // 1. 从 request 属性中获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 构建项目对象
            Project project = new Project();
            project.setId(id);
            project.setName(projectRequest.getName());
            project.setDescription(projectRequest.getDescription());
            project.setVisibility(projectRequest.getVisibility());
            project.setOwnerId(userId);
            
            // 3. Service 层会验证是否为项目所有者
            Project updatedProject = projectService.updateProject(project, projectRequest.getTagIds());
            
            // 4. 丰富项目信息
            updatedProject = projectService.enrichProject(updatedProject, userId);
            
            log.info("项目信息修改成功，ID：{}", updatedProject.getId());
            return Result.success("项目信息修改成功", updatedProject);
            
        } catch (Exception e) {
            log.error("修改项目失败：{}", e.getMessage());
            return Result.error(401, "未授权访问");
        }
    }
    /**
    * 删除项目
    * DELETE /api/projects/{id}
    */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteProject(
            HttpServletRequest request,
            @PathVariable Integer id) {
        log.info("收到删除项目请求，项目 ID：{}", id);
        
        try {
            // 1. 从 request 属性中获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. Service 层会验证是否为项目所有者
            boolean result = projectService.deleteProject(id, userId);
            
            log.info("项目删除成功，ID：{}", id);
            return Result.success("项目删除成功", result);
            
        } catch (Exception e) {
            log.error("删除项目失败：{}", e.getMessage());
            return Result.error(401, "未授权访问");
        }
    }

    /**
     * 获取热门项目（按浏览量排序）
     * GET /api/projects/trending?limit=10
     */
    @GetMapping("/trending")
    public Result<List<Project>> getTrendingProjects(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.info("获取热门项目，限制数量：{}", limit);
        
        try {
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
        } catch (Exception e) {
            log.error("获取热门项目失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误");
        }
    }
    
    /**
    * 通过 ID 获取项目信息
    * GET /api/projects/{id}
    */
    @GetMapping("/{id}")
    public Result<Project> selectById(
            HttpServletRequest request,
            @PathVariable Integer id) {
        log.info("收到获取项目信息请求，项目 ID：{}", id);
        try {
            Integer userId = getCurrentUserId(request);
    
            Project project = projectService.selectById(id);
                
            // 检查项目是否存在
            if (project == null) {
                log.warn("获取项目失败：项目不存在，ID: {}", id);
                return Result.error(404, "项目不存在");
            }
            
            // 丰富项目信息（添加标签、用户交互状态）
            project = projectService.enrichProject(project, userId);
    
            log.info("项目信息获取成功，ID：{}", project.getId());
            return Result.success("项目信息获取成功", project);
        }
        catch (Exception e) {
            log.error("获取项目信息失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误");
        }
    }
    /**
    * 搜索项目（支持分页）
    * GET /api/projects/search/name?name=xxx&pageNum=1&pageSize=10
    */
    @GetMapping("/search/name")
    public Result<List<Project>> selectByName(
            HttpServletRequest request,
            @RequestParam(required = true) String name,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("收到搜索项目请求，项目名称：{}, 页码：{}, 每页数量：{}", name, pageNum, pageSize);
        try {
            // 参数验证
            if (name == null || name.trim().isEmpty()) {
                log.warn("搜索项目失败：项目名称为空");
                return Result.error(400, "项目名称不能为空");
            }
            
            // 分页参数验证
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 10; // 默认每页 10 条，最多 100 条
            }
            
            // 从 request 属性中获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            List<Project> projects = projectService.selectByName(name.trim(), pageNum, pageSize);
            log.info("项目搜索成功，名称：{}, 返回数量：{}", name, projects.size());
            return Result.success("项目搜索成功", projects);
        }
        catch (Exception e) {
            log.error("搜索项目失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误");
        }
    }
    
    /**
    * 获取当前登录用户的所有项目（支持分页）
    * GET /api/projects/my?pageNum=1&pageSize=10
    */
    @GetMapping("/my")
    public Result<List<Project>> selectMyProjects(
            HttpServletRequest request,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("收到获取我的项目请求，页码：{}, 每页数量：{}", pageNum, pageSize);
        try {
            // 分页参数验证
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 10; // 默认每页 10 条，最多 100 条
            }
            
            // 从 request 属性中获取当前用户 ID
            Integer currentUserId = getCurrentUserId(request);
            
            // 只能查看当前登录用户的项目（防止越权）
            List<Project> projects = projectService.selectByUserId(currentUserId, pageNum, pageSize);
            
            // 丰富项目信息
            projects = projectService.enrichProjects(projects, currentUserId);
            
            log.info("获取我的项目成功，用户 ID: {}, 返回数量：{}", currentUserId, projects.size());
            return Result.success("获取我的项目成功", projects);
        }
        catch (Exception e) {
            log.error("获取我的项目失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误");
        }
    }
    
    /**
     * 根据标签查询项目（支持分页）
     * GET /api/projects/tag/{tagId}?pageNum=1&pageSize=10
     */
    @GetMapping("/tag/{tagId}")
    public Result<List<Project>> getProjectsByTag(
            HttpServletRequest request,
            @PathVariable Integer tagId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("按标签查询项目，标签 ID: {}, 页码：{}, 每页数量：{}", tagId, pageNum, pageSize);
        
        try {
            // 分页参数验证
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 10;
            }
            
            List<Project> projects = projectService.selectByTagId(tagId, pageNum, pageSize);
            
            // 丰富项目信息
            Integer userId = getCurrentUserId(request);
            projects = projectService.enrichProjects(projects, userId);
            
            log.info("标签项目查询成功，数量：{}", projects.size());
            return Result.success("查询成功", projects);
        } catch (Exception e) {
            log.error("查询标签项目失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误");
        }
    }
    
    /**
     * 获取所有公开项目（支持分页）
     * GET /api/projects/list?pageNum=1&pageSize=10
     */
    @GetMapping("/list")
    public Result<List<Project>> getPublicProjects(
            HttpServletRequest request,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("获取公开项目列表，页码：{}, 每页数量：{}", pageNum, pageSize);
        
        try {
            // 分页参数验证
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 10;
            }
            
            List<Project> projects = projectService.getPublicProjects(pageNum, pageSize);
            
            // 丰富项目信息
            Integer userId = getCurrentUserId(request);
            projects = projectService.enrichProjects(projects, userId);
            
            log.info("公开项目列表获取成功，数量：{}", projects.size());
            return Result.success("获取成功", projects);
        } catch (Exception e) {
            log.error("获取公开项目列表失败：{}", e.getMessage());
            return Result.error(500, "服务器内部错误");
        }
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
            HttpServletRequest request,
            @PathVariable Integer id) {
        log.info("收到项目下载请求，项目 ID: {}", id);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 获取项目信息
            Project project = projectService.selectById(id);
            if (project == null) {
                log.warn("下载失败：项目不存在，ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            // 3. 检查权限（私有项目只能所有者下载）
            if (project.getVisibility() != null && project.getVisibility() == 0) {
                if (!project.getOwnerId().equals(userId)) {
                    log.warn("下载失败：无权限下载私有项目，项目 ID: {}, 用户 ID: {}", id, userId);
                    return ResponseEntity.status(403).build();
                }
            }
            
            // 4. 打包项目为 ZIP
            java.nio.file.Path zipPath = projectService.packageProjectToZip(id, project.getName());
            
            if (zipPath == null || !Files.exists(zipPath)) {
                log.error("下载失败：ZIP 文件生成失败，项目 ID: {}", id);
                return ResponseEntity.internalServerError().build();
            }
            
            // 5. 创建响应
            Resource resource = new FileSystemResource(zipPath.toFile());
            String fileName = project.getName() + "_" + id + ".zip";
            
            log.info("项目下载成功，项目 ID: {}, 文件名: {}", id, fileName);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
                    .body(resource);
            
        } catch (Exception e) {
            log.error("项目下载失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
