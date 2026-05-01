package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.server.service.ProjectService;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 项目文档控制器
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}/document")
public class ProjectDocumentController {
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * 从 Authorization header 中解析 Token 获取当前登录用户 ID
     * @param request HTTP 请求对象
     * @return 当前登录用户 ID
     * @throws RuntimeException 如果未登录或 Token 无效
     */
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
     * 上传项目文档（支持覆盖上传）
     * POST /api/projects/{projectId}/document/upload
     */
    @PostMapping("/upload")
    public Result<String> uploadDocument(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到项目文档上传请求，项目 ID: {}, 文件名: {}", projectId, file.getOriginalFilename());
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 验证权限（只有项目所有者可以上传文档）
            if (!projectService.isProjectOwner(projectId, userId)) {
                return Result.error(403, "无权操作此项目");
            }
            
            // 3. 上传文档
            String documentUrl = projectService.uploadProjectDocument(projectId, file);
            
            log.info("项目文档上传成功，项目 ID: {}, URL: {}", projectId, documentUrl);
            return Result.success("文档上传成功", documentUrl);
            
        } catch (IllegalArgumentException e) {
            log.warn("参数错误：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("上传项目文档失败：{}", e.getMessage(), e);
            return Result.error(500, "上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除项目文档
     * DELETE /api/projects/{projectId}/document
     */
    @DeleteMapping
    public Result<Void> deleteDocument(
            HttpServletRequest request,
            @PathVariable Integer projectId) {
        
        log.info("收到项目文档删除请求，项目 ID: {}", projectId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 验证权限（只有项目所有者可以删除文档）
            if (!projectService.isProjectOwner(projectId, userId)) {
                return Result.error(403, "无权操作此项目");
            }
            
            // 3. 删除文档
            projectService.deleteProjectDocument(projectId);
            
            log.info("项目文档删除成功，项目 ID: {}", projectId);
            return Result.success("文档删除成功", null);
            
        } catch (Exception e) {
            log.error("删除项目文档失败：{}", e.getMessage(), e);
            return Result.error(500, "删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取项目文档 URL
     * GET /api/projects/{projectId}/document
     */
    @GetMapping
    public Result<String> getDocumentUrl(@PathVariable Integer projectId) {
        
        log.info("获取项目文档 URL，项目 ID: {}", projectId);
        
        try {
            String documentUrl = projectService.getProjectDocumentUrl(projectId);
            
            if (documentUrl == null || documentUrl.isEmpty()) {
                return Result.success("暂无文档", null);
            }
            
            return Result.success(documentUrl);
            
        } catch (Exception e) {
            log.error("获取项目文档 URL 失败：{}", e.getMessage(), e);
            return Result.error(500, "获取失败：" + e.getMessage());
        }
    }
}
