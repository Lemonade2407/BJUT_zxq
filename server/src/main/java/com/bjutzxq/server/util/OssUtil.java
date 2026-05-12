package com.bjutzxq.server.util;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类（单例模式）
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
    
    @Value("${file.upload.max-size:104857600}")
    private long maxFileSize;
    
    @Value("${file.upload.allowed-types:jpg,jpeg,png,gif,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar,7z,tar,gz,txt,md,java,py,js,ts,vue,html,css,json,xml,yml,yaml,c,cpp,h,hpp,cs,go,rb,php,sql,sh,bat,ps1}")
    private String allowedTypesStr;
    
    /**
     * OSS 客户端单例
     */
    private OSS ossClient;
    
    /**
     * 初始化 OSS 客户端（应用启动时调用）
     */
    @PostConstruct
    public void init() {
        log.info("初始化 OSS 客户端...");
        com.aliyun.oss.ClientBuilderConfiguration conf =
            new com.aliyun.oss.ClientBuilderConfiguration();
        conf.setProtocol(com.aliyun.oss.common.comm.Protocol.HTTPS);
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
        log.info("OSS 客户端初始化成功（使用 HTTPS）");
    }
    
    /**
     * 销毁 OSS 客户端（应用关闭时调用）
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            log.info("关闭 OSS 客户端...");
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }

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
        try {
            // 获取原始文件名和后缀
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new IOException("文件名不能为空");
            }
            
            // 验证文件大小（从配置读取，-1 表示不限制）
            if (maxFileSize > 0 && file.getSize() > maxFileSize) {
                long maxSizeMB = maxFileSize / 1024 / 1024;
                throw new IOException("文件大小不能超过 " + maxSizeMB + "MB");
            }

            // 验证文件类型
            String fileExtension = getFileExtension(originalFilename);
            if (!isAllowedType(fileExtension)) {
                throw new IOException("不支持的文件类型: " + fileExtension);
            }
            
            // 获取文件扩展名（处理无扩展名的情况）
            String suffix = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                // 有有效的扩展名
                suffix = originalFilename.substring(lastDotIndex);
            } else {
                log.warn("文件 '{}' 没有扩展名，将使用空后缀", originalFilename);
            }
            
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
            
            // 使用单例 OSSClient 上传文件
            ossClient.putObject(bucketName, objectName, file.getInputStream());

            String accessUrl = getFileAccessUrl(objectName);
            log.info("文件上传成功: {}", accessUrl);
            
            return accessUrl;
        } catch (Exception e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new IOException("OSS 上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedType(String extension) {
        // 如果配置为 "*"，允许所有类型
        if ("*".equals(allowedTypesStr.trim())) {
            return true;
        }
        
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String[] allowedTypes = allowedTypesStr.split(",");
        for (String type : allowedTypes) {
            if (type.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件访问 URL(支持 CDN 加速)
     * @param objectName 对象名称
     * @return 文件访问 URL
     */
    public String getFileAccessUrl(String objectName) {
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
        
        try {
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

    /**
     * 从 OSS 下载文件内容
     * @param fileUrl 文件的完整 URL
     * @return 文件内容的字节数组
     * @throws IOException 下载失败时抛出
     */
    public byte[] download(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IOException("文件 URL 不能为空");
        }
        
        try {
            // 从 URL 中提取 ObjectName
            String objectName = extractObjectName(fileUrl);
            if (objectName == null || objectName.isEmpty()) {
                throw new IOException("无法解析 ObjectName: " + fileUrl);
            }
            
            log.debug("开始从 OSS 下载文件: {}", objectName);
            
            // 使用单例 OSSClient 获取 OSS 对象
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            
            // 读取文件内容
            try (InputStream inputStream = ossObject.getObjectContent();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                byte[] content = outputStream.toByteArray();
                log.debug("文件下载成功，大小: {} bytes", content.length);
                return content;
            }
        } catch (Exception e) {
            log.error("从 OSS 下载文件失败: {}, 错误: {}", fileUrl, e.getMessage());
            throw new IOException("OSS 下载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量删除 OSS 文件（一次请求最多 1000 个）
     * @param fileUrls 文件 URL 列表
     * @return 成功删除的文件数量
     */
    public int batchDelete(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            log.warn("批量删除文件失败: URL 列表为空");
            return 0;
        }
        
        int totalDeleted = 0;
        
        try {
            // 提取所有 ObjectName
            List<String> objectNames = fileUrls.stream()
                .map(this::extractObjectName)
                .filter(name -> name != null && !name.isEmpty())
                .collect(java.util.stream.Collectors.toList());
            
            if (objectNames.isEmpty()) {
                log.warn("批量删除文件失败: 无法解析任何 ObjectName");
                return 0;
            }
            
            log.info("开始批量删除 OSS 文件，数量: {}", objectNames.size());
            
            // OSS 批量删除 API 每次最多支持 1000 个文件
            int batchSize = 1000;
            for (int i = 0; i < objectNames.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, objectNames.size());
                List<String> batch = objectNames.subList(i, endIndex);
                
                log.info("删除第 {}-{} 个文件...", i + 1, endIndex);
                
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName);
                deleteRequest.setKeys(batch);
                deleteRequest.setQuiet(true); // 静默模式，不返回详细信息
                
                DeleteObjectsResult deleteResult = ossClient.deleteObjects(deleteRequest);
                totalDeleted += deleteResult.getDeletedObjects().size();
            }
            
            log.info("批量删除 OSS 文件完成，成功删除: {}/{} 个", totalDeleted, objectNames.size());
            
        } catch (Exception e) {
            log.error("批量删除 OSS 文件失败: {}", e.getMessage(), e);
        }
        
        return totalDeleted;
    }
    
    /**
     * 分片上传大文件（>50MB）
     * @param file 上传的文件
     * @param directory 自定义目录
     * @return 文件的访问 URL
     */
    public String multipartUpload(MultipartFile file, String directory) throws IOException {
        String uploadId = null;
        String objectName = null;
        try {
            // 获取原始文件名和后缀
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new IOException("文件名不能为空");
            }
            
            // 验证文件大小
            if (maxFileSize > 0 && file.getSize() > maxFileSize) {
                long maxSizeMB = maxFileSize / 1024 / 1024;
                throw new IOException("文件大小不能超过 " + maxSizeMB + "MB");
            }
            
            // 验证文件类型
            String fileExtension = getFileExtension(originalFilename);
            if (!isAllowedType(fileExtension)) {
                throw new IOException("不支持的文件类型: " + fileExtension);
            }
            
            // 生成唯一文件名
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String suffix = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                suffix = originalFilename.substring(lastDotIndex);
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
            
            // 决定存储路径
            if (directory != null && !directory.trim().isEmpty()) {
                objectName = directory + "/" + datePath + "/" + fileName;
            } else {
                objectName = fileHost + "/" + datePath + "/" + fileName;
            }
            
            log.info("开始分片上传文件到 OSS: {}, 大小: {} bytes ({:.2f} MB)", 
                objectName, file.getSize(), file.getSize() / 1024.0 / 1024.0);
            
            // 1. 初始化分片上传
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, objectName);
            InitiateMultipartUploadResult initResult = ossClient.initiateMultipartUpload(initRequest);
            uploadId = initResult.getUploadId();
            
            log.info("分片上传初始化成功，uploadId: {}", uploadId);
            
            // 2. 分片上传
            long fileSize = file.getSize();
            long partSize = 5 * 1024 * 1024; // 每个分片 5MB
            int partCount = (int) (fileSize / partSize);
            if (fileSize % partSize != 0) {
                partCount++;
            }
            
            log.info("文件将分为 {} 个分片上传，每个分片大小: {} MB", partCount, partSize / 1024 / 1024);
            
            java.util.List<PartETag> partETags = new java.util.ArrayList<>();
            
            try (java.io.InputStream inputStream = file.getInputStream()) {
                for (int i = 0; i < partCount; i++) {
                    // 计算当前分片的大小
                    long currentPartSize = Math.min(partSize, fileSize - i * partSize);
                    
                    // 读取分片数据
                    byte[] partBuffer = new byte[(int) currentPartSize];
                    int bytesRead = inputStream.read(partBuffer);
                    
                    if (bytesRead != currentPartSize) {
                        throw new IOException("读取分片数据失败");
                    }
                    
                    // 上传分片
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(objectName);
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequest.setInputStream(new java.io.ByteArrayInputStream(partBuffer));
                    uploadPartRequest.setPartSize(currentPartSize);
                    uploadPartRequest.setPartNumber(i + 1);
                    
                    UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                    partETags.add(uploadPartResult.getPartETag());
                    
                    log.debug("分片 {}/{} 上传成功", i + 1, partCount);
                }
            }
            
            // 3. 完成分片上传
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                bucketName, objectName, uploadId, partETags);
            ossClient.completeMultipartUpload(completeRequest);
            
            String accessUrl = getFileAccessUrl(objectName);
            log.info("分片上传成功: {}", accessUrl);
            
            return accessUrl;
            
        } catch (Exception e) {
            log.error("OSS 分片上传失败: {}", e.getMessage(), e);
            
            // 如果上传失败，取消分片上传
            if (uploadId != null && objectName != null) {
                try {
                    AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                        bucketName, objectName, uploadId);
                    ossClient.abortMultipartUpload(abortRequest);
                    log.info("已取消分片上传: {}", uploadId);
                } catch (Exception abortEx) {
                    log.error("取消分片上传失败: {}", abortEx.getMessage());
                }
            }
            
            throw new IOException("OSS 分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名 PUT URL（用于前端直接上传到 OSS）
     * @param objectKey OSS 对象路径，如 projects/2026/05/12/uuid.pdf
     * @return 预签名 URL，前端可对其发起 PUT 请求上传文件
     */
    /**
     * 生成 OSS PostObject 直传所需的 policy + 签名
     * 签名算法：base64(hmac-sha1(accessKeySecret, base64(policy)))
     */
    public Map<String, String> generatePostSignature(String objectKey) {
        long expireEnd = System.currentTimeMillis() + 600 * 1000;
        String expireStr = com.aliyun.oss.common.utils.DateUtil
            .formatIso8601Date(new Date(expireEnd));

        String policy = "{\"expiration\":\"" + expireStr + "\"," +
            "\"conditions\":[" +
            "{\"bucket\":\"" + bucketName + "\"}," +
            "{\"key\":\"" + objectKey + "\"}," +
            "[\"content-length-range\",1,10737418240]" +
            "]}";

        String encodedPolicy = java.util.Base64.getEncoder()
            .encodeToString(policy.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String signature = ossClient.calculatePostSignature(encodedPolicy);

        Map<String, String> result = new java.util.HashMap<>();
        result.put("host", "https://" + bucketName + "." + endpoint);
        result.put("accessKeyId", accessKeyId);
        result.put("policy", encodedPolicy);
        result.put("signature", signature);
        result.put("objectKey", objectKey);
        return result;
    }

    /**
     * 生成完整的 OSS 对象 key（包含日期和 UUID）
     */
    public String generateObjectKey(String originalFilename) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = getFileExtension(originalFilename);
        return fileHost + "/" + datePath + "/" + uuid + (ext.isEmpty() ? "" : "." + ext);
    }
}

