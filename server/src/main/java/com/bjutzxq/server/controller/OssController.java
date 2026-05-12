package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oss")
public class OssController {

    @Autowired
    private OssUtil ossUtil;

    @PostMapping("/upload-signatures")
    public Result<List<Map<String, String>>> getUploadSignatures(
            @RequestBody Map<String, Object> body) {

        Integer userId = UserIdContext.getCurrentUserId();
        log.info("用户 {} 请求上传签名", userId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) body.get("files");
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("文件列表不能为空");
        }

        List<Map<String, String>> signatures = new ArrayList<>();
        for (Map<String, Object> fileInfo : files) {
            String fileName = (String) fileInfo.get("name");
            String path = (String) fileInfo.get("path");
            log.info("收到文件签名请求: name={}, path={}", fileName, path);
            String objectKey = ossUtil.generateObjectKey(fileName);
            Map<String, String> sig = ossUtil.generatePostSignature(objectKey);
            sig.put("fileName", fileName);
            if (path != null && !path.isEmpty()) {
                sig.put("path", path);
            }
            signatures.add(sig);
        }

        log.info("已生成 {} 个上传签名", signatures.size());
        return Result.success(signatures);
    }
}
