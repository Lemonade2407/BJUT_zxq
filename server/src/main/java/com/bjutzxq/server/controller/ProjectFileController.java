package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.vo.FileVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.service.ProjectFileService;
import com.bjutzxq.server.service.ProjectService;
import com.bjutzxq.server.service.UserService;
import com.bjutzxq.server.util.DtoConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProjectService projectService;
    
    /**
     * 批量上传文件（支持文件夹）
     * POST /api/projects/{projectId}/files/upload-batch
     */
    @PostMapping("/upload-batch")
    public Result<List<FileVO>> uploadFiles(
            @PathVariable Integer projectId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "parentId", required = false) Integer parentId) {
            
        log.info("收到批量文件上传请求，项目 ID: {}, 文件数量：{}", projectId, files.length);
            
        // 获取当前用户 ID 并批量上传
        Integer userId = UserIdContext.getCurrentUserId();
        List<ProjectFile> projectFiles = projectFileService.uploadFiles(projectId, files, parentId, userId);
        
        // 转换为 VO（批量查询上传者信息）
        List<FileVO> responses = convertToVOListWithBatchQuery(projectFiles);
            
        log.info("批量文件上传成功，成功数量：{}", responses.size());
        return Result.success("批量文件上传成功", responses);
    }
    
    /**
     * 覆盖上传文件（先删除再上传）
     * POST /api/projects/{projectId}/files/overwrite-upload
     */
    @PostMapping("/overwrite-upload")
    public Result<List<FileVO>> overwriteUploadFiles(
            @PathVariable Integer projectId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "parentId", required = false) Integer parentId) {
            
        log.info("收到覆盖上传请求，项目 ID: {}, 文件数量：{}", projectId, files.length);
            
        // 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 先删除项目的所有文件
        int deletedCount = projectFileService.deleteAllProjectFiles(projectId);
        log.info("已删除旧文件 {} 个", deletedCount);
            
        // 批量上传新文件
        List<ProjectFile> projectFiles = projectFileService.uploadFiles(projectId, files, parentId, userId);
        
        // 转换为 VO（批量查询上传者信息）
        List<FileVO> responses = convertToVOListWithBatchQuery(projectFiles);
            
        log.info("覆盖上传成功，删除 {} 个旧文件，上传 {} 个新文件", deletedCount, responses.size());
        return Result.success("覆盖上传成功，已替换 " + deletedCount + " 个旧文件", responses);
    }
    
    /**
     * 获取所有文件（用于构建完整的树形结构）
     * GET /api/projects/{projectId}/files/all
     */
    @GetMapping("/all")
    public Result<List<FileVO>> getAllFiles(@PathVariable Integer projectId) {
        
        log.info("收到所有文件查询请求，项目 ID: {}", projectId);
        
        // 查询所有文件并转换为 VO
        List<ProjectFile> files = projectFileService.getAllFiles(projectId);
        List<FileVO> responses = convertToVOListWithBatchQuery(files);
        
        log.info("所有文件查询成功，文件数量：{}", responses.size());
        return Result.success("所有文件查询成功", responses);
    }
    
    // ==================== 项目文档管理 ====================
    
    /**
     * 上传项目文档（支持覆盖上传）
     * POST /api/projects/{projectId}/files/document/upload
     */
    @PostMapping("/document/upload")
    public Result<String> uploadDocument(
            @PathVariable Integer projectId,
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到项目文档上传请求，项目 ID: {}, 文件名: {}", projectId, file.getOriginalFilename());
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 验证权限
        if (!projectService.isProjectOwner(projectId, userId)) {
            throw new RuntimeException("无权操作此项目");
        }
        
        // 3. 上传文档
        String documentUrl = projectService.uploadProjectDocument(projectId, file);
        
        log.info("项目文档上传成功，项目 ID: {}, URL: {}", projectId, documentUrl);
        return Result.success("文档上传成功", documentUrl);
    }
    
    /**
     * 删除项目文档
     * DELETE /api/projects/{projectId}/files/document
     */
    @DeleteMapping("/document")
    public Result<Void> deleteDocument(@PathVariable Integer projectId) {
        
        log.info("收到项目文档删除请求，项目 ID: {}", projectId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 验证权限
        if (!projectService.isProjectOwner(projectId, userId)) {
            throw new RuntimeException("无权操作此项目");
        }
        
        // 3. 删除文档
        projectService.deleteProjectDocument(projectId);
        
        log.info("项目文档删除成功，项目 ID: {}", projectId);
        return Result.success("文档删除成功", null);
    }
    
    /**
     * 获取项目文档 URL
     * GET /api/projects/{projectId}/files/document
     */
    @GetMapping("/document")
    public Result<String> getDocumentUrl(@PathVariable Integer projectId) {
        
        log.info("获取项目文档 URL，项目 ID: {}", projectId);
        
        String documentUrl = projectService.getProjectDocumentUrl(projectId);
        
        if (documentUrl == null || documentUrl.isEmpty()) {
            return Result.success("暂无文档", null);
        }
        
        return Result.success(documentUrl);
    }
    
    /**
     * 将 ProjectFile 列表转换为 FileVO 列表（批量查询优化）
     * @param projectFiles 项目文件列表
     * @return FileVO 列表
     */
    private List<FileVO> convertToVOListWithBatchQuery(List<ProjectFile> projectFiles) {
        if (projectFiles == null || projectFiles.isEmpty()) {
            return List.of();
        }
        
        // 1. 收集所有上传者 ID
        java.util.Set<Integer> uploaderIds = projectFiles.stream()
            .map(ProjectFile::getUploaderId)
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toSet());
        
        // 2. 批量查询用户信息
        java.util.Map<Integer, String> uploaderMap = new java.util.HashMap<>();
        if (!uploaderIds.isEmpty()) {
            java.util.List<User> users = userMapper.selectBatchIds(new java.util.ArrayList<>(uploaderIds));
            for (User user : users) {
                uploaderMap.put(user.getId(), user.getUsername());
            }
        }
        
        // 3. 使用 DtoConverter 批量转换
        return DtoConverter.toFileVOList(projectFiles, uploaderMap);
    }
}
