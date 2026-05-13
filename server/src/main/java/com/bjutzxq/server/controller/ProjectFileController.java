package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.vo.FileVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.service.ProjectFileService;
import com.bjutzxq.server.service.ProjectService;
import com.bjutzxq.server.util.DtoConverter;
import com.bjutzxq.server.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private UserMapper userMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private OssUtil ossUtil;
    
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
     * 删除项目所有文件
     * DELETE /api/projects/{projectId}/files/all
     */
    @DeleteMapping("/all")
    public Result<Integer> deleteAllFiles(@PathVariable Integer projectId) {
        Integer userId = UserIdContext.getCurrentUserId();
        log.info("删除项目所有文件，项目 ID: {}", projectId);
        int count = projectFileService.deleteAllProjectFiles(projectId);
        log.info("已删除 {} 条文件记录", count);
        return Result.success("已删除 " + count + " 个文件", count);
    }

    /**
     * 确认 OSS 直传完成，创建文件记录
     * POST /api/projects/{projectId}/files/confirm
     */
    @PostMapping("/confirm")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<List<FileVO>> confirmUpload(
            @PathVariable Integer projectId,
            @RequestBody List<Map<String, Object>> files) {

        Integer userId = UserIdContext.getCurrentUserId();
        log.info("确认 OSS 直传，项目 ID: {}, 文件数: {}", projectId, files.size());

        // 预先加载所有现有目录到缓存（避免 N+1 查询）
        java.util.Map<String, Integer> dirCache = new java.util.HashMap<>();
        List<ProjectFile> existingFiles = projectFileService.getAllFiles(projectId);
        for (ProjectFile f : existingFiles) {
            if (f.getIsDir() != null && f.getIsDir() == 1) {
                // 构建目录的唯一键：parentId/dirName
                String key = buildDirKey(f.getParentId(), f.getFileName());
                dirCache.put(key, f.getId());
            }
        }

        List<ProjectFile> projectFiles = new ArrayList<>();
        for (Map<String, Object> f : files) {
            String objectKey = (String) f.get("objectKey");
            String fileName = (String) f.get("fileName");
            String filePath = (String) f.get("path");
            Long fileSize = f.get("fileSize") != null
                ? ((Number) f.get("fileSize")).longValue() : 0L;

            String ossUrl = ossUtil.getFileAccessUrl(objectKey);

            // 如果有文件夹路径，创建目录记录
            Integer parentId = null;
            if (filePath != null && !filePath.isEmpty()) {
                // 去掉文件名，只保留目录部分
                String dirPath = filePath.replace("\\", "/");
                int lastSlash = dirPath.lastIndexOf('/');
                if (lastSlash > 0) {
                    dirPath = dirPath.substring(0, lastSlash);
                } else {
                    dirPath = "";
                }

                if (!dirPath.isEmpty()) {
                    String[] parts = dirPath.split("/");
                    StringBuilder currentPath = new StringBuilder();
                    Integer currentParentId = null;

                    for (String dirName : parts) {
                        if (dirName.isEmpty()) continue;
                        currentPath.append("/").append(dirName);
                        String cacheKey = buildDirKey(currentParentId, dirName);

                        Integer dirId = dirCache.get(cacheKey);
                        if (dirId == null) {
                            // 创建新目录
                            ProjectFile dir = new ProjectFile();
                            dir.setProjectId(projectId);
                            dir.setFileName(dirName);
                            dir.setParentId(currentParentId);
                            dir.setIsDir(1);
                            dir.setUploaderId(userId);
                            dir.setFileType("");
                            dir.setFileSize(0L);
                            dir.setStorageUrl("");
                            projectFileService.insertSingle(dir);
                            dirId = dir.getId();
                            dirCache.put(cacheKey, dirId);
                        }
                        currentParentId = dirId;
                    }
                    parentId = currentParentId;
                }
            }

            ProjectFile pf = new ProjectFile();
            pf.setProjectId(projectId);
            pf.setFileName(fileName);
            pf.setStorageUrl(ossUrl);
            pf.setFileSize(fileSize);
            pf.setFileType(ossUtil.getFileExtension(fileName));
            pf.setParentId(parentId);
            pf.setUploaderId(userId);
            pf.setIsDir(0);
            projectFiles.add(pf);
        }

        projectFiles = projectFileService.batchCreate(projectFiles);
        List<FileVO> result = convertToVOListWithBatchQuery(projectFiles);
        log.info("OSS 直传确认完成，创建 {} 条文件记录", result.size());
        return Result.success("确认成功", result);
    }

    /**
     * 构建目录的唯一键
     */
    private String buildDirKey(Integer parentId, String dirName) {
        return (parentId == null ? "null" : parentId) + "/" + dirName;
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
