package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.PageResult;
import com.bjutzxq.pojo.vo.CommentVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.CommentService;
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
     * 发表评论
     * POST /api/projects/{projectId}/comments
     */
    @PostMapping("")
    public Result<CommentVO> postComment(
            @PathVariable Integer projectId,
            @RequestParam String content) {
        
        log.info("收到评论请求，项目 ID: {}", projectId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 发表评论
        com.bjutzxq.pojo.entity.Comment comment = commentService.postComment(
            userId, 
            projectId, 
            content
        );
        
        // 4. 清除缓存后重新查询（返回带用户信息的 VO）
        List<CommentVO> comments = commentService.getCommentsByProjectId(projectId, 1, 10, 1);
        CommentVO response = comments.stream()
            .filter(c -> c.getId().equals(comment.getId()))
            .findFirst()
            .orElse(null);
        
        log.info("评论发表成功，评论 ID: {}", comment.getId());
        return Result.success("评论成功", response);
    }
    
    /**
     * 获取评论列表
     * GET /api/projects/{projectId}/comments?pageNum=1&pageSize=10
     */
    @GetMapping("")
    public Result<PageResult<CommentVO>> getComments(
            @PathVariable Integer projectId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", defaultValue = "1") Integer status) {
        
        log.info("收到评论列表请求，项目 ID: {}, 页码：{}, 每页数量：{}", projectId, pageNum, pageSize);
        
        // 1. 获取评论列表（已包含用户信息，已优化 N+1 问题）
        List<CommentVO> responses = commentService.getCommentsByProjectId(
            projectId, 
            pageNum, 
            pageSize, 
            status
        );
        
        // 2. 获取评论总数
        long total = commentService.countByProjectId(projectId);
        
        // 3. 构建分页响应
        PageResult<CommentVO> response = new PageResult<>(responses, total, pageNum, pageSize);
        
        log.info("评论列表获取成功，评论数量：{}, 总数：{}", responses.size(), total);
        return Result.success("评论列表获取成功", response);
    }
    
    /**
     * 删除评论
     * DELETE /api/projects/{projectId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public Result<Boolean> deleteComment(
            @PathVariable Integer projectId,
            @PathVariable Integer commentId) {
        
        log.info("收到删除评论请求，项目 ID: {}, 评论 ID: {}", projectId, commentId);
        
        // 1. 获取当前用户 ID
        Integer userId = UserIdContext.getCurrentUserId();
        
        // 2. 删除评论
        commentService.deleteComment(commentId, userId);
        
        log.info("评论删除成功，评论 ID: {}", commentId);
        return Result.success("删除成功", true);
    }
    
}
