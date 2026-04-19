package com.bjutzxq.server.service;

import com.bjutzxq.common.Constants;
import com.bjutzxq.pojo.ProjectFile;
import com.bjutzxq.pojo.User;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 项目文件服务类
 */
@Slf4j
@Service
public class ProjectFileService {
    
    @Autowired
    private ProjectFileMapper projectFileMapper;
    
    @Autowired
    private OssUtil ossUtil;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 文件上传根目录(仅用于临时处理,最终存储到 OSS)
     */
    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir") + "/project_files/";
    
    /**
     * 批量上传文件（支持文件夹解析）
     * @param projectId 项目 ID
     * @param files 文件数组
     * @param parentId 父目录 ID（null 表示根目录）
     * @param uploaderId 上传者 ID
     * @return 文件信息列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ProjectFile> uploadFiles(Integer projectId, MultipartFile[] files, Integer parentId, Integer uploaderId) {
        log.info("开始批量上传文件，项目 ID: {}, 文件数量：{}", projectId, files.length);
        
        // TODO: 添加总文件大小限制检查
        // TODO: 添加病毒扫描功能
        // TODO: 支持断点续传和大文件分片上传
        
        List<ProjectFile> uploadedFiles = new ArrayList<>();
        
        try {
            // 1. 遍历所有上传的文件
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("跳过空文件：{}", file.getOriginalFilename());
                    continue;
                }
                
                // 2. 解析文件名中的路径信息（前端传来的文件名可能包含相对路径）
                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null || originalFileName.trim().isEmpty()) {
                    continue;
                }
                
                // 3. 处理路径分隔符（统一为/）
                originalFileName = originalFileName.replace("\\", "/");
                
                // 4. 判断是否是文件夹上传（文件名中包含路径分隔符）
                if (originalFileName.contains("/")) {
                    // 递归创建目录并上传文件
                    ProjectFile uploadedFile = uploadFileWithPath(projectId, file, parentId, uploaderId);
                    uploadedFiles.add(uploadedFile);
                } else {
                    // 直接上传到指定目录
                    ProjectFile uploadedFile = uploadFile(projectId, file, parentId, uploaderId);
                    uploadedFiles.add(uploadedFile);
                }
            }
            
            log.info("批量上传完成，成功上传 {} 个文件", uploadedFiles.size());
            return uploadedFiles;
            
        } catch (Exception e) {
            log.error("批量上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传文件
     * @param projectId 项目 ID
     * @param file 文件对象
     * @param parentId 父目录 ID（null 表示根目录）
     * @param uploaderId 上传者 ID
     * @return 文件信息
     */
    public ProjectFile uploadFile(Integer projectId, MultipartFile file, Integer parentId, Integer uploaderId) throws IOException {
        log.info("开始上传文件，项目 ID: {}, 文件名：{}, 大小：{} bytes", 
                   projectId, file.getOriginalFilename(), file.getSize());
            
        // 验证文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
            
        // 直接使用原始文件名（不含路径）
        String fileName = file.getOriginalFilename();
            
        // 保存文件并返回文件信息
        return saveUploadedFile(projectId, file, fileName, parentId, uploaderId);
    }


    /**
     * 创建目录
     * @param projectId 项目 ID
     * @param dirName 目录名称
     * @param parentId 父目录 ID（null 表示根目录）
     * @param uploaderId 上传者 ID
     * @return 目录信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectFile createDirectory(Integer projectId, String dirName, Integer parentId, Integer uploaderId) {
        log.info("开始创建目录，项目 ID: {}, 目录名：{}, 父目录 ID: {}", projectId, dirName, parentId);
        
        // 1. 检查同一目录下是否有重名目录
        ProjectFile existing = projectFileMapper.selectByPath(projectId, buildFilePath(parentId), dirName);
        if (existing != null) {
            throw new RuntimeException("该目录下已存在同名目录");
        }
        
        // 2. 创建目录记录
        ProjectFile directory = new ProjectFile();
        directory.setProjectId(projectId);
        directory.setFileName(dirName);
        directory.setFilePath(buildFilePath(parentId));
        directory.setIsDir(1);  // 1-目录
        directory.setParentId(parentId);
        directory.setUploaderId(uploaderId);
        
        // 3. 保存到数据库
        projectFileMapper.insert(directory);
        log.info("目录创建成功，目录 ID: {}", directory.getId());
        
        return directory;
    }
    
    /**
     * 获取文件列表
     * @param projectId 项目 ID
     * @param parentId 父目录 ID（null 表示根目录）
     * @return 文件列表
     */
    public List<ProjectFile> getFileList(Integer projectId, Integer parentId) {
        log.info("查询文件列表，项目 ID: {}, 父目录 ID: {}", projectId, parentId);
        return projectFileMapper.selectByProjectIdAndParentId(projectId, parentId);
    }
    
    /**
     * 根据 ID 获取文件详情
     * @param id 文件 ID
     * @return 文件信息
     */
    public ProjectFile getFileById(Integer id) {
        log.info("查询文件详情，文件 ID: {}", id);
        return projectFileMapper.selectById(id);
    }
    
    /**
     * 重命名文件/目录
     * @param fileId 文件 ID
     * @param newFileName 新文件名
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(Integer fileId, String newFileName, Integer userId) {
        log.info("重命名文件，文件 ID: {}, 新名称：{}", fileId, newFileName);
        
        // 1. 查询原文件信息
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 2. 验证参数
        if (newFileName == null || newFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("新文件名不能为空");
        }
        
        // 3. 检查同一目录下是否有重名文件
        ProjectFile existing = projectFileMapper.selectByPath(
            file.getProjectId(), 
            buildFilePath(file.getParentId()), 
            newFileName
        );
        if (existing != null && !existing.getId().equals(fileId)) {
            throw new RuntimeException("该目录下已存在同名文件");
        }
        
        // 4. 更新文件名
        file.setFileName(newFileName.trim());
        int rows = projectFileMapper.updateById(file);
        if (rows == 0) {
            throw new RuntimeException("重命名失败");
        }
        
        log.info("文件重命名成功，文件 ID: {}, 新名称：{}", fileId, newFileName);
    }
    
    /**
     * 移动文件到指定目录
     * @param fileId 文件 ID
     * @param targetParentId 目标父目录 ID（null 表示根目录）
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(Integer fileId, Integer targetParentId, Integer userId) {
        log.info("移动文件，文件 ID: {}, 目标目录 ID: {}", fileId, targetParentId);
        
        // 1. 查询文件信息
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 2. 如果是目录，不能移动到自身或子目录中（防止循环引用）
        if (Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
            checkCircularReference(fileId, targetParentId);
        }
        
        // 3. 检查目标位置是否有重名文件
        String targetPath = buildFilePath(targetParentId);
        ProjectFile existing = projectFileMapper.selectByPath(
            file.getProjectId(), 
            targetPath, 
            file.getFileName()
        );
        if (existing != null && !existing.getId().equals(fileId)) {
            throw new RuntimeException("目标位置已存在同名文件");
        }
        
        // 4. 更新父目录 ID 和路径
        file.setParentId(targetParentId);
        file.setFilePath(targetPath);
        int rows = projectFileMapper.updateById(file);
        if (rows == 0) {
            throw new RuntimeException("移动失败");
        }
        
        log.info("文件移动成功，文件 ID: {}, 目标目录 ID: {}", fileId, targetParentId);
    }

    /**
     * 获取物理文件路径（用于下载）
     * @param fileId 文件 ID
     * @return 物理文件的 Path 对象
     */
    public java.nio.file.Path getPhysicalFilePath(Integer fileId) {
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null || Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
            return null;
        }

        String physicalPath = UPLOAD_DIR + file.getProjectId() + "/" +
                new File(file.getStorageUrl()).getName();
        return Paths.get(physicalPath);
    }
    
    /**
     * 检查是否存在循环引用（用于目录移动）
     */
    private void checkCircularReference(Integer fileId, Integer targetParentId) {
        // 简单检查：如果目标目录是当前目录的子目录，则不允许移动
        if (targetParentId == null) {
            return;
        }
        
        ProjectFile parent = projectFileMapper.selectById(targetParentId);
        if (parent == null) {
            return;
        }
        
        // 递归检查上级目录是否包含当前目录
        while (parent != null && Constants.File.TYPE_DIRECTORY.equals(parent.getIsDir())) {
            if (parent.getId().equals(fileId)) {
                throw new RuntimeException("不能将目录移动到其子目录中");
            }
            parent = projectFileMapper.selectById(parent.getParentId());
        }
    }
    
    /**
     * 获取文件内容（用于预览）
     * @param fileId 文件 ID
     * @return 文件内容字符串
     */
    public String getFileContent(Integer fileId) {
        log.info("读取文件内容，文件 ID: {}", fileId);
        
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 如果是目录，不能读取内容
        if (Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
            throw new IllegalArgumentException("目录没有内容");
        }
        
        // 如果数据库中已有内容，直接返回
        if (file.getContent() != null && !file.getContent().isEmpty()) {
            return file.getContent();
        }
        
        // 否则从物理文件读取（针对之前上传的文件）
        try {
            String physicalPath = UPLOAD_DIR + file.getProjectId() + "/" + 
                                 new File(file.getStorageUrl()).getName();
            Path path = Paths.get(physicalPath);
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path));
            } else {
                throw new RuntimeException("物理文件不存在");
            }
        } catch (IOException e) {
            log.error("读取文件失败：{}", e.getMessage());
            throw new RuntimeException("读取文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     * @param id 文件 ID
     * @param userId 当前用户 ID（用于权限验证）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Integer id, Integer userId) {
        log.info("删除文件，文件 ID: {}, 用户 ID: {}", id, userId);
        
        // 1. 查询文件信息
        ProjectFile file = projectFileMapper.selectById(id);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        
        // 2. 如果是目录，先删除子文件和子目录
        if (Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
            List<ProjectFile> children = projectFileMapper.selectByProjectIdAndParentId(file.getProjectId(), id);
            for (ProjectFile child : children) {
                deleteFile(child.getId(), userId);
            }
        }
        
        // 3. 删除物理文件（如果是文件）
        if (Constants.File.TYPE_FILE.equals(file.getIsDir()) && file.getStorageUrl() != null) {
            try {
                String physicalPath = UPLOAD_DIR + file.getProjectId() + "/" + 
                                     new File(file.getStorageUrl()).getName();
                Path path = Paths.get(physicalPath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("物理文件删除成功：{}", physicalPath);
                }
            } catch (IOException e) {
                log.warn("物理文件删除失败：{}", e.getMessage());
                // 不抛出异常，继续删除数据库记录
            }
        }
        
        // 4. 删除数据库记录
        int rows = projectFileMapper.deleteById(id);
        if (rows == 0) {
            throw new RuntimeException("文件删除失败");
        }
        
        log.info("文件删除成功，文件 ID: {}", id);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }
    
    /**
     * 构建文件路径字符串（递归构建完整路径）
     */
    private String buildFilePath(Integer parentId) {
        if (parentId == null) {
            return "/";
        }
        
        // 递归查找父目录路径
        StringBuilder pathBuilder = new StringBuilder();
        buildPathRecursive(parentId, pathBuilder);
        return pathBuilder.toString();
    }
    
    /**
     * 递归构建路径的辅助方法
     * @param parentId 父目录 ID
     * @param pathBuilder 路径构建器
     */
    private void buildPathRecursive(Integer parentId, StringBuilder pathBuilder) {
        if (parentId == null) {
            return;
        }
        
        ProjectFile parent = projectFileMapper.selectById(parentId);
        if (parent != null && Constants.File.TYPE_DIRECTORY.equals(parent.getIsDir())) {
            // 先构建上级路径
            buildPathRecursive(parent.getParentId(), pathBuilder);
            // 添加当前目录名
            pathBuilder.append(parent.getFileName()).append("/");
        } else {
            pathBuilder.append("/");
        }
    }
    
    /**
     * 判断是否为 README.md 文件
     */
    private boolean isReadmeFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        return fileName.equalsIgnoreCase("README.md");
    }

    /**
     * 上传带路径的文件（用于文件夹上传）
     * @param projectId 项目 ID
     * @param file 文件对象
     * @param baseParentId 基础父目录 ID
     * @param uploaderId 上传者 ID
     * @return 文件信息
     */
    private ProjectFile uploadFileWithPath(Integer projectId, MultipartFile file, Integer baseParentId, Integer uploaderId) throws IOException {
        String originalFileName = file.getOriginalFilename().replace("\\", "/");

        // 1. 分离目录和文件名
        int lastSeparatorIndex = originalFileName.lastIndexOf("/");
        String relativePath = originalFileName.substring(0, lastSeparatorIndex);
        String fileName = originalFileName.substring(lastSeparatorIndex + 1);

        // 2. 递归创建目录结构
        Integer targetParentId = createDirectoriesRecursively(projectId, relativePath, baseParentId, uploaderId);

        // 3. 保存文件并返回文件信息
        return saveUploadedFile(projectId, file, fileName, targetParentId, uploaderId);
    }

    /**
     * 递归创建目录结构
     * @param projectId 项目 ID
     * @param relativePath 相对路径（如：dir1/dir2/dir3）
     * @param currentParentId 当前父目录 ID
     * @param uploaderId 上传者 ID
     * @return 最终创建的目录 ID
     */
    private Integer createDirectoriesRecursively(Integer projectId, String relativePath, Integer currentParentId, Integer uploaderId) {
        // 分割路径
        String[] pathParts = relativePath.split("/");

        Integer currentDirId = currentParentId;

        // 逐级创建目录
        for (String dirName : pathParts) {
            if (dirName == null || dirName.trim().isEmpty()) {
                continue;
            }

            // 检查目录是否已存在
            String displayPath = buildFilePath(currentDirId);
            ProjectFile existing = projectFileMapper.selectByPath(projectId, displayPath, dirName.trim());

            if (existing != null && Constants.File.TYPE_DIRECTORY.equals(existing.getIsDir())) {
                // 目录已存在，使用现有目录
                currentDirId = existing.getId();
                log.debug("目录已存在：{}, ID: {}", dirName, currentDirId);
            } else {
                // 创建新目录
                ProjectFile directory = createDirectory(projectId, dirName.trim(), currentDirId, uploaderId);
                currentDirId = directory.getId();
                log.debug("创建目录：{}, ID: {}", dirName, currentDirId);
            }
        }

        return currentDirId;
    }
    /**
     * 保存上传的文件（公共方法）
     */
    private ProjectFile saveUploadedFile(Integer projectId, MultipartFile file, String fileName, Integer parentId, Integer uploaderId) throws IOException {
        // 1. 生成文件存储路径
        String fileExtension = getFileExtension(fileName);
        
        // 2. 判断是否为多媒体或大文件 (例如 > 5MB)
        boolean isLargeFile = file.getSize() > 5 * 1024 * 1024 || 
                              fileExtension.matches("(mp4|avi|mov|jpg|png|gif|zip|rar|pdf)");
        
        ProjectFile projectFile = new ProjectFile();
        projectFile.setProjectId(projectId);
        projectFile.setFileName(fileName);
        projectFile.setFilePath(buildFilePath(parentId));
        projectFile.setFileSize(file.getSize());
        projectFile.setFileType(fileExtension);
        projectFile.setIsDir(0);  // 0-文件
        projectFile.setParentId(parentId);
        projectFile.setUploaderId(uploaderId);

        if (isLargeFile) {
            // --- 走 OSS 流程 ---
            log.info("检测到大文件/多媒体，上传至 OSS: {}", fileName);
            try {
                String ossUrl = ossUtil.upload(file);
                projectFile.setStorageType(2);
                projectFile.setStorageUrl(ossUrl);
            } catch (Exception e) {
                log.error("OSS 上传失败", e);
                throw new RuntimeException("OSS 上传失败: " + e.getMessage());
            }
        } else {
            // --- 小文件也使用 OSS(简化架构,避免 Git 存储膨胀) ---
            log.info("上传小文件至 OSS: {}", fileName);
            try {
                String ossUrl = ossUtil.upload(file);
                projectFile.setStorageType(2);  // 统一使用 OSS
                projectFile.setStorageUrl(ossUrl);
                
                // 如果是 README.md 文件，读取内容用于预览
                if (isReadmeFile(fileName)) {
                    String content = new String(file.getBytes());
                    projectFile.setContent(content);
                }
            } catch (Exception e) {
                log.error("OSS 上传失败", e);
                throw new RuntimeException("OSS 上传失败: " + e.getMessage());
            }
        }

        // 3. 保存到数据库
        projectFileMapper.insert(projectFile);
        log.info("文件上传成功，文件 ID: {}, 存储类型: {}", projectFile.getId(), projectFile.getStorageType());

        return projectFile;
    }

}
