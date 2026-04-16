package com.bjutzxq.server.controller;

import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.Comment;
import com.bjutzxq.server.service.CommentService;
import com.bjutzxq.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}/comments")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    /**
     * 从 Authorization header 中解析 Token 获取当前登录用户 ID
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
     * 发表评论
     * POST /api/projects/{projectId}/comments
     */
    @PostMapping("")
    public Result<Comment> postComment(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @RequestBody CommentRequest commentRequest) {
        
        log.info("收到评论请求，项目 ID: {}, 父评论 ID: {}", projectId, commentRequest.getParentId());
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 验证参数
            if (commentRequest.getContent() == null || commentRequest.getContent().trim().isEmpty()) {
                return Result.error(400, "评论内容不能为空");
            }
            
            // 3. 发表评论
            Comment comment = commentService.postComment(
                userId, 
                projectId, 
                commentRequest.getContent(), 
                commentRequest.getParentId()
            );
            
            log.info("评论发表成功，评论 ID: {}", comment.getId());
            return Result.success("评论成功", comment);
            
        } catch (Exception e) {
            log.error("评论失败：{}", e.getMessage());
            return Result.error(500, "评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取评论列表
     * GET /api/projects/{projectId}/comments?pageNum=1&pageSize=10
     */
    @GetMapping("")
    public Result<List<Comment>> getComments(
            @PathVariable Integer projectId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", defaultValue = "1") Integer status) {
        
        log.info("收到评论列表请求，项目 ID: {}, 页码：{}, 每页数量：{}", projectId, pageNum, pageSize);
        
        try {
            // 1. 获取评论列表
            List<Comment> comments = commentService.getCommentsByProjectId(
                projectId, 
                pageNum, 
                pageSize, 
                status
            );
            
            log.info("评论列表获取成功，评论数量：{}", comments.size());
            return Result.success("评论列表获取成功", comments);
            
        } catch (Exception e) {
            log.error("获取评论列表失败：{}", e.getMessage());
            return Result.error(500, "获取评论列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除评论
     * DELETE /api/projects/{projectId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public Result<Boolean> deleteComment(
            HttpServletRequest request,
            @PathVariable Integer projectId,
            @PathVariable Integer commentId) {
        
        log.info("收到删除评论请求，评论 ID: {}", commentId);
        
        try {
            // 1. 获取当前用户 ID
            Integer userId = getCurrentUserId(request);
            
            // 2. 删除评论
            commentService.deleteComment(commentId, userId);
            
            log.info("评论删除成功，评论 ID: {}", commentId);
            return Result.success("删除成功", true);
            
        } catch (Exception e) {
            log.error("删除评论失败：{}", e.getMessage());
            return Result.error(500, "删除评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 评论请求参数
     */
    @Data
    public static class CommentRequest {
        private String content;
        private Integer parentId;
    }
}
