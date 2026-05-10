package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.vo.CommentVO;
import com.bjutzxq.pojo.entity.Comment;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.util.DtoConverter;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bjutzxq.server.mapper.CommentMapper;
import com.bjutzxq.server.mapper.UserMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务类
 */
@Slf4j
@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 发表评论
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @param content 评论内容
     * @return 评论信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Comment postComment(Integer userId, Integer projectId, String content) {
        log.info("发表评论，用户 ID: {}, 项目 ID: {}", userId, projectId);
            
        // 1. 验证参数
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        
        // 2. 创建评论对象
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setProjectId(projectId);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        // 3. 保存到数据库
        commentMapper.insert(comment);
        
        // 4. 创建通知（通知项目所有者）
        try {
            // 查询项目信息（获取项目名称和所有者ID）
            Integer ownerId = commentMapper.getProjectOwnerId(projectId);
            String projectName = commentMapper.getProjectName(projectId);
            
            // 只有不是自己发表时才发送通知
            if (ownerId != null && !ownerId.equals(userId)) {
                // 获取评论用户的名称
                User sender = userMapper.selectById(userId);
                String senderName = sender != null ? sender.getUsername() : "未知用户";
                String projectNameStr = projectName != null ? projectName : "未知项目";
                
                String notificationContent = senderName + " 评论了你的项目：" + projectNameStr;
                
                notificationService.createNotification(
                    ownerId, userId, projectId, 
                    NotificationType.COMMENT, notificationContent);
            }
        } catch (Exception e) {
            log.warn("创建通知失败：{}", e.getMessage());
            // 通知创建失败不影响评论发表
        }
        
        log.info("评论发表成功，评论 ID: {}", comment.getId());
        
        return comment;
    }
    
    /**
     * 获取项目评论列表
     * @param projectId 项目 ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param status 评论状态
     * @return 评论 VO 列表（包含用户信息）
     */
    @Cacheable(value = "commentList", key = "#projectId + '_' + #pageNum + '_' + #pageSize + '_' + #status")
    public List<CommentVO> getCommentsByProjectId(Integer projectId, Integer pageNum, Integer pageSize, Integer status) {
        log.info("获取评论列表，项目 ID: {}, 页码：{}, 每页数量：{}", projectId, pageNum, pageSize);
        
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);
        
        // 1. 查询评论列表
        List<Comment> comments = commentMapper.selectByProjectId(projectId, status != null ? status : 1);
        
        if (comments.isEmpty()) {
            return List.of();
        }
        
        // 2. 批量查询所有评论者的用户信息（解决 N+1 问题）
        List<Integer> userIds = comments.stream()
            .map(Comment::getUserId)
            .distinct()
            .collect(Collectors.toList());
        
        // 防止空列表导致 SQL 错误
        final Map<Integer, Map<String, Object>> userMap;
        if (!userIds.isEmpty()) {
            List<Map<String, Object>> users = commentMapper.selectUserBatch(userIds);
            userMap = users.stream()
                .collect(Collectors.toMap(
                    u -> (Integer) u.get("id"),
                    u -> u
                ));
        } else {
            userMap = new java.util.HashMap<>();
        }
        
        // 3. 转换为 CommentVO（包含用户信息）
        List<CommentVO> commentVOs = DtoConverter.toCommentVOList(comments, userMap);
        
        log.info("评论列表获取成功，评论数量：{}", commentVOs.size());
        
        return commentVOs;
    }
    
    /**
     * 删除评论
     * @param commentId 评论 ID
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "commentList", allEntries = true)
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

        // 3. 软删除评论（将状态设为 0）
        comment.setStatus(0);
        commentMapper.updateById(comment);
        
        log.info("评论删除成功，评论 ID: {}", commentId);
    }
    
    /**
     * 统计项目的评论总数
     * @param projectId 项目 ID
     * @return 评论总数
     */
    public long countByProjectId(Integer projectId) {
        log.debug("统计评论总数，项目 ID: {}", projectId);
        return commentMapper.countByProjectId(projectId);
    }

    /**
     * 管理员获取所有评论（分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param status 评论状态（null查全部）
     * @return 评论VO列表
     */
    public List<CommentVO> getAllCommentsForAdmin(Integer pageNum, Integer pageSize, Integer status) {
        log.info("管理员获取评论列表，页码：{}，每页数量：{}，状态：{}", pageNum, pageSize, status);
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.selectAllWithPage(status);
        return enrichCommentsWithUserAndProject(comments);
    }

    /**
     * 管理员搜索评论（分页）
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 评论VO列表
     */
    public List<CommentVO> searchCommentsForAdmin(String keyword, Integer pageNum, Integer pageSize) {
        log.info("管理员搜索评论，关键词：{}，页码：{}，每页数量：{}", keyword, pageNum, pageSize);
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.searchByKeyword(keyword);
        return enrichCommentsWithUserAndProject(comments);
    }

    /**
     * 管理员物理删除评论
     * @param commentId 评论 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "commentList", allEntries = true)
    public void adminDeleteComment(Integer commentId) {
        log.info("管理员删除评论，ID: {}", commentId);
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        commentMapper.forceDeleteById(commentId);
        log.info("管理员删除评论成功，ID: {}", commentId);
    }

    /**
     * 统计评论数量（管理员，按状态）
     * @param status 评论状态（null查全部）
     */
    public long countByStatus(Integer status) {
        return commentMapper.countByStatus(status);
    }

    /**
     * 统计评论数量（管理员，按关键词）
     * @param keyword 关键词
     */
    public long countByKeywordForAdmin(String keyword) {
        return commentMapper.countByKeyword(keyword);
    }

    /**
     * 批量填充评论的用户信息和项目名称
     */
    private List<CommentVO> enrichCommentsWithUserAndProject(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        // 批量查询用户信息
        List<Integer> userIds = comments.stream()
            .map(Comment::getUserId)
            .distinct()
            .collect(Collectors.toList());
        Map<Integer, Map<String, Object>> userMap;
        if (!userIds.isEmpty()) {
            List<Map<String, Object>> users = commentMapper.selectUserBatch(userIds);
            userMap = users.stream()
                .collect(Collectors.toMap(
                    u -> (Integer) u.get("id"),
                    u -> u
                ));
        } else {
            userMap = new java.util.HashMap<>();
        }
        // 批量查询项目名称
        List<Integer> projectIds = comments.stream()
            .map(Comment::getProjectId)
            .distinct()
            .collect(Collectors.toList());
        Map<Integer, String> projectNameMap = new java.util.HashMap<>();
        if (!projectIds.isEmpty()) {
            List<Map<String, Object>> projectNames = commentMapper.getProjectNamesBatch(projectIds);
            for (Map<String, Object> row : projectNames) {
                projectNameMap.put((Integer) row.get("id"), (String) row.get("name"));
            }
        }
        // 转换为VO
        List<CommentVO> vos = DtoConverter.toCommentVOList(comments, userMap);
        for (CommentVO vo : vos) {
            vo.setProjectName(projectNameMap.get(vo.getProjectId()));
        }
        return vos;
    }
}
