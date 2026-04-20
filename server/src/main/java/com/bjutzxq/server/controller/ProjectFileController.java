package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.ProjectFile;
import com.bjutzxq.server.service.ProjectFileService;
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
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.util.List;

/**
 * 项目文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}/files")
public class ProjectFileController {
    
    @Autowired
    private ProjectFileService projectFileService;
    
    /**
     * 从 Authorization header 中解析 Token 获取当前登录用户 ID
     */
    // TODO: 与 ProjectController 中的方法重复，应抽取为公共工具类或拦截器
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
     * 上传文件
     * POST /api/projects/{projectId}/files/upload
     */
    @PostMapping("/upload")
    public Result<ProjectFile> uploadFile(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentId", required = false) Integer parentId)
    {
            
        log.info("收到文件上传请求，项目 ID: {}, 文件名：{}", projectId, file.getOriginalFilename());
            
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
                
            // 2. 上传文件
            ProjectFile projectFile = projectFileService.uploadFile(projectId, file, parentId, userId);
                
            log.info("文件上传成功，文件 ID: {}", projectFile.getId());
            return Result.success("文件上传成功", projectFile);
                
        } catch (IllegalArgumentException e) {
            log.error("文件上传失败：{}", e.getMessage());
            return Result.error(400, "文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage());
            return Result.error(500, "文件上传失败：" + e.getMessage());
        }
    }
        
    /**
     * 批量上传文件（支持文件夹）
     * POST /api/projects/{projectId}/files/upload-batch
     */
    @PostMapping("/upload-batch")
    public Result<List<ProjectFile>> uploadFiles(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "parentId", required = false) Integer parentId) {
            
        log.info("收到批量文件上传请求，项目 ID: {}, 文件数量：{}", projectId, files.length);
            
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
                
            // 2. 批量上传文件
            List<ProjectFile> projectFiles = projectFileService.uploadFiles(projectId, files, parentId, userId);
                
            log.info("批量文件上传成功，成功数量：{}", projectFiles.size());
            return Result.success("批量文件上传成功", projectFiles);
                
        } catch (Exception e) {
            log.error("批量文件上传失败：{}", e.getMessage());
            return Result.error(500, "批量文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 覆盖上传文件（先删除再上传）
     * POST /api/projects/{projectId}/files/overwrite-upload
     */
    @PostMapping("/overwrite-upload")
    public Result<List<ProjectFile>> overwriteUploadFiles(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "parentId", required = false) Integer parentId) {
            
        log.info("收到覆盖上传请求，项目 ID: {}, 文件数量：{}", projectId, files.length);
            
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 先删除项目的所有文件
            int deletedCount = projectFileService.deleteAllProjectFiles(projectId);
            log.info("已删除旧文件 {} 个", deletedCount);
                
            // 3. 批量上传新文件
            List<ProjectFile> projectFiles = projectFileService.uploadFiles(projectId, files, parentId, userId);
                
            log.info("覆盖上传成功，删除 {} 个旧文件，上传 {} 个新文件", deletedCount, projectFiles.size());
            return Result.success("覆盖上传成功，已替换 " + deletedCount + " 个旧文件", projectFiles);
                
        } catch (Exception e) {
            log.error("覆盖上传失败：{}", e.getMessage());
            return Result.error(500, "覆盖上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文件列表
     * GET /api/projects/{projectId}/files?parentId=xxx
     */
    @GetMapping("")
    public Result<List<ProjectFile>> getFileList(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestParam(value = "parentId", required = false) Integer parentId) {
        
        log.info("收到文件列表查询请求，项目 ID: {}, 父目录 ID: {}", projectId, parentId);
        
        try {
            // 1. 获取当前用户 ID（用于权限验证）
            Integer userId = getCurrentUserId(request);
            
            // 2. 查询文件列表
            List<ProjectFile> files = projectFileService.getFileList(projectId, parentId);
            
            log.info("文件列表查询成功，文件数量：{}", files.size());
            return Result.success("文件列表查询成功", files);
            
        } catch (Exception e) {
            log.error("文件列表查询失败：{}", e.getMessage());
            return Result.error(500, "文件列表查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有文件（用于构建完整的树形结构）
     * GET /api/projects/{projectId}/files/all
     */
    @GetMapping("/all")
    public Result<List<ProjectFile>> getAllFiles(
            HttpServletRequest request,
            @PathVariable Integer projectId) {
        
        log.info("收到所有文件查询请求，项目 ID: {}", projectId);
        
        try {
            // 1. 获取当前用户 ID（用于权限验证）
            Integer userId = getCurrentUserId(request);
            
            // 2. 查询所有文件
            List<ProjectFile> files = projectFileService.getAllFiles(projectId);
            
            log.info("所有文件查询成功，文件数量：{}", files.size());
            return Result.success("所有文件查询成功", files);
            
        } catch (Exception e) {
            log.error("所有文件查询失败：{}", e.getMessage());
            return Result.error(500, "所有文件查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文件详情
     * GET /api/projects/{projectId}/files/{fileId}
     */
    @GetMapping("/{fileId}")
    public Result<ProjectFile> getFileDetail(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @PathVariable Integer fileId) {
        
        log.info("收到文件详情查询请求，文件 ID: {}", fileId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 查询文件详情
            ProjectFile file = projectFileService.getFileById(fileId);
            
            if (file == null) {
                return Result.error(404, "文件不存在");
            }
            
            log.info("文件详情查询成功，文件 ID: {}", fileId);
            return Result.success("文件详情查询成功", file);
            
        } catch (Exception e) {
            log.error("文件详情查询失败：{}", e.getMessage());
            return Result.error(500, "文件详情查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 下载文件（返回文件流）
     * GET /api/projects/{projectId}/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @PathVariable Integer fileId) {
        
        log.info("收到文件下载请求，文件 ID: {}", fileId);
        
        // TODO: 添加下载次数统计
        // TODO: 添加下载日志记录，便于审计
        // TODO: 支持断点续传（Range 请求）
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 查询文件信息
            ProjectFile file = projectFileService.getFileById(fileId);
            
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // 3. 获取物理文件路径
            java.nio.file.Path physicalPath = projectFileService.getPhysicalFilePath(fileId);
            if (physicalPath == null || !Files.exists(physicalPath)) {
                return ResponseEntity.notFound().build();
            }
            
            // 4. 创建资源对象并设置响应头
            Resource resource = new FileSystemResource(physicalPath.toFile());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + file.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
                    .body(resource);
            
        } catch (Exception e) {
            log.error("文件下载失败：{}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除文件
     * DELETE /api/projects/{projectId}/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    public Result<Boolean> deleteFile(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @PathVariable Integer fileId) {
        
        log.info("收到文件删除请求，文件 ID: {}", fileId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 删除文件
            projectFileService.deleteFile(fileId, userId);
            
            log.info("文件删除成功，文件 ID: {}", fileId);
            return Result.success("文件删除成功", true);
            
        } catch (Exception e) {
            log.error("文件删除失败：{}", e.getMessage());
            return Result.error(500, "文件删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建目录
     * POST /api/projects/{projectId}/files/directory
     */
    @PostMapping("/directory")
    public Result<ProjectFile> createDirectory(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestBody ProjectFile directoryInfo) {
        
        log.info("收到目录创建请求，项目 ID: {}, 目录名：{}", projectId, directoryInfo.getFileName());
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 验证参数
            if (directoryInfo.getFileName() == null || directoryInfo.getFileName().trim().isEmpty()) {
                return Result.error(400, "目录名称不能为空");
            }
            
            // 3. 创建目录
            ProjectFile directory = projectFileService.createDirectory(
                projectId, 
                directoryInfo.getFileName().trim(), 
                directoryInfo.getParentId(), 
                userId
            );
            
            log.info("目录创建成功，目录 ID: {}", directory.getId());
            return Result.success("目录创建成功", directory);
            
        } catch (Exception e) {
            log.error("目录创建失败：{}", e.getMessage());
            return Result.error(500, "目录创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 重命名文件/目录
     * PUT /api/projects/{projectId}/files/{fileId}/rename
     */
    @PutMapping("/{fileId}/rename")
    public Result<Boolean> renameFile(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @PathVariable Integer fileId,
            @RequestBody ProjectFile fileInfo) {
        
        log.info("收到文件重命名请求，文件 ID: {}, 新名称：{}", fileId, fileInfo.getFileName());
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 验证参数
            if (fileInfo.getFileName() == null || fileInfo.getFileName().trim().isEmpty()) {
                return Result.error(400, "文件名不能为空");
            }
            
            // 3. 执行重命名
            projectFileService.renameFile(fileId, fileInfo.getFileName().trim(), userId);
            
            log.info("文件重命名成功，文件 ID: {}", fileId);
            return Result.success("文件重命名成功", true);
            
        } catch (Exception e) {
            log.error("文件重命名失败：{}", e.getMessage());
            return Result.error(500, "文件重命名失败：" + e.getMessage());
        }
    }
    
    /**
     * 移动文件到指定目录
     * PUT /api/projects/{projectId}/files/{fileId}/move
     */
    @PutMapping("/{fileId}/move")
    public Result<Boolean> moveFile(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @PathVariable Integer fileId,
            @RequestParam(value = "targetParentId", required = false) Integer targetParentId) {
        
        log.info("收到文件移动请求，文件 ID: {}, 目标目录 ID: {}", fileId, targetParentId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 执行移动操作
            projectFileService.moveFile(fileId, targetParentId, userId);
            
            log.info("文件移动成功，文件 ID: {}", fileId);
            return Result.success("文件移动成功", true);
            
        } catch (Exception e) {
            log.error("文件移动失败：{}", e.getMessage());
            return Result.error(500, "文件移动失败：" + e.getMessage());
        }
    }
}
