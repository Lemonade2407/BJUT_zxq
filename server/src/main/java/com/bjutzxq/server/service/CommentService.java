package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.Comment;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bjutzxq.server.mapper.CommentMapper;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.pojo.Project;

import java.util.List;

/**
 * 评论服务类
 */
@Slf4j
@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 发表评论（包括回复）
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @param content 评论内容
     * @param parentId 父评论 ID（回复时使用）
     * @return 评论信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Comment postComment(Integer userId, Integer projectId, String content, Integer parentId) {
        log.info("发表评论，用户 ID: {}, 项目 ID: {}, 父评讻 ID: {}", userId, projectId, parentId);
            
        // TODO: 添加敏感词过滤功能
        // TODO: 添加评论频率限制，防止刷评
        // TODO: 支持 Markdown 格式解析
            
        // 1. 验证参数
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        
        // 2. 如果是回复，检查父评论是否存在
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || parent.getStatus() == 0) {
                throw new RuntimeException("父评论不存在或已被删除");
            }
            
            // 增加父评论的回复数
            if (parent.getReplyCount() == null) {
                parent.setReplyCount(0);
            }
            parent.setReplyCount(parent.getReplyCount() + 1);
            commentMapper.updateById(parent);
        }
        
        // 3. 创建评论对象
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setProjectId(projectId);
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        
        // 4. 保存到数据库
        commentMapper.insert(comment);
        
        // 5. 创建通知（如果是回复评论，通知被回复者；否则通知项目所有者）
        try {
            // TODO: 异步发送通知，避免阻塞主流程
            Project project = projectMapper.selectById(projectId);
            if (project != null) {
                Integer receiverId = parentId != null ? 
                    commentMapper.selectById(parentId).getUserId() : project.getOwnerId();
                    
                // 只有不是自己发表/回复时才发送通知
                if (!receiverId.equals(userId)) {
                    String notificationContent = parentId != null ? 
                        "回复了你的评论：" + content.trim() : 
                        "评论了你的项目：" + project.getName();
                    
                    notificationService.createNotification(
                        receiverId, userId, projectId, 
                        NotificationType.COMMENT.getCode(), notificationContent);
                }
            }
        } catch (Exception e) {
            log.warn("创建通知失败：{}", e.getMessage());
            // 通知创建失败不影响评论发表
        }
        
        log.info("评论发表成功，评论 ID: {}", comment.getId());
        
        return comment;
    }
    
    /**
     * 获取项目评论列表（分页）
     * @param projectId 项目 ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param status 评论状态
     * @return 评论列表
     */
    public List<Comment> getCommentsByProjectId(Integer projectId, Integer pageNum, Integer pageSize, Integer status) {
        log.info("获取评论列表，项目 ID: {}, 页码：{}, 每页数量：{}", projectId, pageNum, pageSize);
        
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);
        
        // 查询评论列表（只查询顶级评论）
        List<Comment> comments = commentMapper.selectByProjectId(projectId, status != null ? status : 1);
        
        // 为每个评论加载回复列表
        for (Comment comment : comments) {
            if (comment.getParentId() == null) {
                List<Comment> replies = commentMapper.selectReplies(comment.getId());
                comment.setReplyCount(replies.size());
            }
        }
        
        log.info("评论列表获取成功，评论数量：{}", comments.size());
        
        return comments;
    }
    
    /**
     * 获取评论详情
     * @param id 评论 ID
     * @return 评论信息
     */
    public Comment getCommentById(Integer id) {
        return commentMapper.selectById(id);
    }
    
    /**
     * 删除评论
     * @param commentId 评论 ID
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Integer commentId, Integer userId) {
        log.info("删除评论，评论 ID: {}, 用户 ID: {}", commentId, userId);
        
        // 1. 查询评论信息
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        // 2. 检查权限（只能删除自己的评论）
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除该评论");
        }
        
        // 3. 如果是顶级评论，先删除所有回复
        if (comment.getParentId() == null) {
            List<Comment> replies = commentMapper.selectReplies(commentId);
            for (Comment reply : replies) {
                commentMapper.deleteById(reply.getId());
            }
        } else {
            // 如果是回复，减少父评论的回复数
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null && parent.getReplyCount() != null && parent.getReplyCount() > 0) {
                parent.setReplyCount(parent.getReplyCount() - 1);
                commentMapper.updateById(parent);
            }
        }
        
        // 4. 删除评论（软删除，将状态设为 0）
        comment.setStatus(0);
        commentMapper.updateById(comment);
        
        log.info("评论删除成功，评论 ID: {}", commentId);
    }
}
