package com.bjutzxq.server.util;

import com.bjutzxq.pojo.vo.*;
import com.bjutzxq.pojo.entity.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO 转换工具类
 */
@Slf4j
public class DtoConverter {
    
    /**
     * 将 Project Entity 转换为 ProjectVO
     *
     */
    public static ProjectVO toProjectResponse(Project project) {
        if (project == null) {
            return null;
        }
        
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setDescription(project.getDescription());
        vo.setOwnerId(project.getOwnerId());
        vo.setAuthor(project.getAuthor());
        vo.setProjectType(project.getProjectType());
        vo.setCourseName(project.getCourseName());
        vo.setThesisType(project.getThesisType());
        vo.setVisibility(project.getVisibility());
        vo.setStarCount(project.getStarCount());
        vo.setWatchCount(project.getWatchCount());
        vo.setFileCount(project.getFileCount());
        vo.setDownloadCount(project.getDownloadCount());
        vo.setViewCount(project.getViewCount());
        vo.setDocumentUrl(project.getDocumentUrl());
        vo.setCreatedAt(project.getCreatedAt());
        vo.setUpdatedAt(project.getUpdatedAt());
        vo.setTags(project.getTags());
        vo.setIsStarred(project.getIsStarred());
        vo.setIsWatched(project.getIsWatched());
        vo.setOwnerUsername(project.getOwnerUsername());
        vo.setOwnerClassName(project.getOwnerClassName());
        
        return vo;
    }
    
    /**
     * 批量转换 Project
     */
    public static List<ProjectVO> toProjectResponseList(List<Project> projects) {
        if (projects == null) {
            return List.of();
        }
        
        return projects.stream()
            .map(DtoConverter::toProjectResponse)
            .collect(Collectors.toList());
    }

    /**
     * 构建 LoginVO
     */
    public static LoginVO buildLoginResponse(User user, String token) {
        if (user == null) {
            return null;
        }
        
        LoginVO vo = new LoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setEmployeeId(user.getEmployeeId());
        vo.setRole(user.getRole() != null ? user.getRole().name() : "USER");
        vo.setToken(token);
        vo.setExpiresIn(com.bjutzxq.common.Constants.JWT.TOKEN_EXPIRE_TIME / 1000L);
        
        return vo;
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double fileSize = size.doubleValue();
        
        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }

    // ==================== VO 转换方法 ====================
    
    /**
     * 将 ProjectFile Entity 转换为 FileVO（包含上传者信息）
     */
    public static FileVO toFileVO(com.bjutzxq.pojo.entity.ProjectFile file, String uploaderUsername) {
        if (file == null) {
            return null;
        }
        
        FileVO vo = new FileVO();
        vo.setId(file.getId());
        vo.setProjectId(file.getProjectId());
        vo.setFileName(file.getFileName());
        vo.setFilePath(file.getFilePath());
        vo.setFileSize(file.getFileSize());
        vo.setFormattedSize(formatFileSize(file.getFileSize()));
        vo.setFileType(file.getFileType());
        vo.setStorageUrl(file.getStorageUrl());
        vo.setIsDir(file.getIsDir());
        vo.setParentId(file.getParentId());
        vo.setUploaderId(file.getUploaderId());
        vo.setUploaderUsername(uploaderUsername);
        vo.setCreatedAt(file.getCreatedAt());
        vo.setUpdatedAt(file.getUpdatedAt());
        
        return vo;
    }
    
    /**
     * 批量转换 FileVO
     */
    public static List<FileVO> toFileVOList(List<com.bjutzxq.pojo.entity.ProjectFile> files,
                                      java.util.Map<Integer, String> uploaderMap) {
        if (files == null) {
            return List.of();
        }
        
        return files.stream()
            .map(file -> {
                String uploaderUsername = uploaderMap != null ? uploaderMap.get(file.getUploaderId()) : null;
                return toFileVO(file, uploaderUsername);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 将 Notification Entity 转换为 NotificationVO（包含发送者和项目信息）
     */
    public static NotificationVO toNotificationVO(Notification notification,
                                           String senderUsername,
                                           String projectName) {
        if (notification == null) {
            return null;
        }
        
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setUserId(notification.getUserId());
        vo.setSenderId(notification.getSenderId());
        vo.setSenderUsername(senderUsername);
        vo.setProjectId(notification.getProjectId());
        vo.setProjectName(projectName);
        vo.setType(notification.getType());
        vo.setContent(notification.getContent());
        vo.setIsRead(notification.getIsRead());
        vo.setCreatedAt(notification.getCreatedAt());
        
        return vo;
    }

    /**
     * 将 Comment Entity 转换为 CommentVO（包含用户信息）
     */
    public static CommentVO toCommentVO(Comment comment, Map<Integer, Map<String, Object>> userMap) {
        if (comment == null) {
            return null;
        }
        
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setProjectId(comment.getProjectId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setUpdatedAt(comment.getUpdatedAt());
        vo.setStatus(comment.getStatus());
        
        // 填充用户信息
        Map<String, Object> userInfo = userMap.get(comment.getUserId());
        if (userInfo != null) {
            vo.setUsername((String) userInfo.get("username"));
            vo.setAvatar((String) userInfo.get("avatar"));
        }
        
        return vo;
    }
    
    /**
     * 批量转换 CommentVO
     */
    public static List<CommentVO> toCommentVOList(List<Comment> comments,
                                                   Map<Integer, Map<String, Object>> userMap) {
        if (comments == null) {
            return List.of();
        }

        return comments.stream()
            .map(comment -> toCommentVO(comment, userMap))
            .collect(Collectors.toList());
    }

    // ==================== UserVO 转换 ====================

    public static UserVO toUserVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmployeeId(user.getEmployeeId());
        vo.setRealName(user.getRealName());
        vo.setClassName(user.getClassName());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setSex(user.getSex());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    // ==================== Team / TeamApplication 转换 ====================

    private static final Map<Integer, String> TEAM_STATUS_MAP = Map.of(0, "已结束", 1, "招募中", 2, "已满员");
    private static final Map<Integer, String> APP_STATUS_MAP = Map.of(0, "待审核", 1, "已通过", 2, "已拒绝");

    public static TeamVO toTeamVO(Team team, Map<Integer, Map<String, Object>> userMap) {
        if (team == null) return null;
        TeamVO vo = new TeamVO();
        vo.setId(team.getId());
        vo.setUserId(team.getUserId());
        vo.setTitle(team.getTitle());
        vo.setDescription(team.getDescription());
        vo.setCurrentMembers(team.getCurrentMembers());
        vo.setNeededMembers(team.getNeededMembers());
        vo.setTag(team.getTag());
        vo.setCourseName(team.getCourseName());
        vo.setStatus(team.getStatus());
        vo.setStatusText(TEAM_STATUS_MAP.getOrDefault(team.getStatus(), "未知"));
        vo.setCreatedAt(team.getCreatedAt());
        vo.setUpdatedAt(team.getUpdatedAt());
        if (userMap != null) {
            Map<String, Object> userInfo = userMap.get(team.getUserId());
            if (userInfo != null) {
                vo.setCreatorUsername((String) userInfo.get("username"));
                vo.setCreatorAvatar((String) userInfo.get("avatar"));
            }
        }
        return vo;
    }

    public static List<TeamVO> toTeamVOList(List<Team> teams, Map<Integer, Map<String, Object>> userMap) {
        if (teams == null || teams.isEmpty()) return List.of();
        return teams.stream().map(t -> toTeamVO(t, userMap)).collect(Collectors.toList());
    }

    public static TeamApplicationVO toTeamApplicationVO(TeamApplication app, Map<Integer, Map<String, Object>> userMap) {
        if (app == null) return null;
        TeamApplicationVO vo = new TeamApplicationVO();
        vo.setId(app.getId());
        vo.setTeamId(app.getTeamId());
        vo.setApplicantId(app.getApplicantId());
        vo.setMessage(app.getMessage());
        vo.setStatus(app.getStatus());
        vo.setStatusText(APP_STATUS_MAP.getOrDefault(app.getStatus(), "未知"));
        vo.setTeamTitle(app.getTeamTitle());
        vo.setCreatedAt(app.getCreatedAt());
        if (userMap != null) {
            Map<String, Object> u = userMap.get(app.getApplicantId());
            if (u != null) {
                vo.setApplicantUsername((String) u.get("username"));
                vo.setApplicantAvatar((String) u.get("avatar"));
            }
        }
        return vo;
    }

    public static List<TeamApplicationVO> toTeamApplicationVOList(List<TeamApplication> apps, Map<Integer, Map<String, Object>> userMap) {
        if (apps == null || apps.isEmpty()) return List.of();
        return apps.stream().map(a -> toTeamApplicationVO(a, userMap)).collect(Collectors.toList());
    }
}
