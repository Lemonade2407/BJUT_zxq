package com.bjutzxq.server.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类
 */
@Slf4j
@Component
public class OssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.file-host}")
    private String fileHost;

    @Value("${aliyun.oss.cdn.enabled:false}")
    private boolean cdnEnabled;

    @Value("${aliyun.oss.cdn.domain:}")
    private String cdnDomain;

    /**
     * 上传文件到 OSS
     * @param file 上传的文件
     * @return 文件的访问 URL
     */
    public String upload(MultipartFile file) throws IOException {
        return upload(file, null);
    }

    /**
     * 上传文件到 OSS(指定目录)
     * @param file 上传的文件
     * @param directory 自定义目录(如 "avatars", "projects")
     * @return 文件的访问 URL
     */
    public String upload(MultipartFile file, String directory) throws IOException {
        // 创建 OSSClient 实例
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 获取原始文件名和后缀
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new IOException("文件名不能为空");
            }
            
            // 验证文件大小(最大 100MB)
            long maxSize = 100 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                throw new IOException("文件大小不能超过 100MB");
            }
            
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            
            // 生成唯一文件名
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
            
            // 根据目录参数决定存储路径
            String objectName;
            if (directory != null && !directory.trim().isEmpty()) {
                // 自定义目录: avatars/2026/04/19/uuid.jpg
                objectName = directory + "/" + datePath + "/" + fileName;
            } else {
                // 默认目录: projects/2026/04/19/uuid.jpg
                objectName = fileHost + "/" + datePath + "/" + fileName;
            }

            log.info("开始上传文件到 OSS: {}, 大小: {} bytes", objectName, file.getSize());
            
            // 上传文件
            ossClient.putObject(bucketName, objectName, file.getInputStream());

            String accessUrl = getFileAccessUrl(objectName);
            log.info("文件上传成功: {}", accessUrl);
            
            return accessUrl;
        } catch (Exception e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new IOException("OSS 上传失败: " + e.getMessage(), e);
        } finally {
            // 关闭 OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 获取文件访问 URL(支持 CDN 加速)
     * @param objectName 对象名称
     * @return 文件访问 URL
     */
    private String getFileAccessUrl(String objectName) {
        if (cdnEnabled && cdnDomain != null && !cdnDomain.isEmpty()) {
            // 使用 CDN 加速域名
            return cdnDomain + "/" + objectName;
        } else {
            // 使用 OSS 原始域名
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        }
    }

    /**
     * 删除 OSS 上的文件
     * @param fileUrl 文件的完整 URL
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("删除文件失败: URL 为空");
            return;
        }
        
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            // 从 URL 中提取 ObjectName
            String objectName = extractObjectName(fileUrl);
            if (objectName != null && !objectName.isEmpty()) {
                log.info("删除 OSS 文件: {}", objectName);
                ossClient.deleteObject(bucketName, objectName);
                log.info("文件删除成功: {}", objectName);
            } else {
                log.warn("无法解析 ObjectName: {}", fileUrl);
            }
        } catch (Exception e) {
            log.error("删除 OSS 文件失败: {}, 错误: {}", fileUrl, e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 从文件 URL 中提取 ObjectName
     * @param fileUrl 文件 URL
     * @return ObjectName
     */
    private String extractObjectName(String fileUrl) {
        // 尝试从 CDN 域名提取
        if (cdnEnabled && cdnDomain != null && !cdnDomain.isEmpty() && fileUrl.startsWith(cdnDomain)) {
            return fileUrl.substring(cdnDomain.length() + 1);
        }
        
        // 尝试从 OSS 原始域名提取
        String ossPrefix = "https://" + bucketName + "." + endpoint + "/";
        if (fileUrl.startsWith(ossPrefix)) {
            return fileUrl.substring(ossPrefix.length());
        }
        
        return null;
    }
}
