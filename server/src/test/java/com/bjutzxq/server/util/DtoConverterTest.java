package com.bjutzxq.server.util;

import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.entity.*;
import com.bjutzxq.pojo.vo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO转换工具测试")
class DtoConverterTest {

    // ==================== toProjectResponse ====================

    @Test
    @DisplayName("转换ProjectVO - 正常流程")
    void toProjectResponse_Success() {
        // Arrange
        Project project = new Project();
        project.setId(1);
        project.setName("测试项目");
        project.setDescription("描述");
        project.setOwnerId(100);
        project.setProjectType("课设项目");
        project.setCourseName("软件工程");
        project.setStarCount(10);
        project.setWatchCount(5);

        // Act
        ProjectVO result = DtoConverter.toProjectResponse(project);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("测试项目", result.getName());
        assertEquals("描述", result.getDescription());
        assertEquals(100, result.getOwnerId());
        assertEquals(10, result.getStarCount());
        assertEquals(5, result.getWatchCount());
    }

    @Test
    @DisplayName("转换ProjectVO - null返回null")
    void toProjectResponse_Null() {
        // Act
        ProjectVO result = DtoConverter.toProjectResponse(null);

        // Assert
        assertNull(result);
    }

    // ==================== toProjectResponseList ====================

    @Test
    @DisplayName("批量转换ProjectVO - 正常流程")
    void toProjectResponseList_Success() {
        // Arrange
        Project project = new Project();
        project.setId(1);
        project.setName("测试");

        // Act
        List<ProjectVO> result = DtoConverter.toProjectResponseList(List.of(project));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("批量转换ProjectVO - null返回空列表")
    void toProjectResponseList_Null() {
        // Act
        List<ProjectVO> result = DtoConverter.toProjectResponseList(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== buildLoginResponse ====================

    @Test
    @DisplayName("构建LoginVO - 正常流程")
    void buildLoginResponse_Success() {
        // Arrange
        User user = new User();
        user.setId(1);
        user.setUsername("测试用户");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        // Act
        LoginVO result = DtoConverter.buildLoginResponse(user, "access123", "refresh123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("测试用户", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("USER", result.getRole());
        assertEquals("access123", result.getAccessToken());
    }

    @Test
    @DisplayName("构建LoginVO - role为null默认USER")
    void buildLoginResponse_NullRole() {
        // Arrange
        User user = new User();
        user.setId(1);
        user.setUsername("test");

        // Act
        LoginVO result = DtoConverter.buildLoginResponse(user, "access", "refresh");

        // Assert
        assertEquals("USER", result.getRole());
    }

    @Test
    @DisplayName("构建LoginVO - null user返回null")
    void buildLoginResponse_NullUser() {
        // Act
        LoginVO result = DtoConverter.buildLoginResponse(null, "access", "refresh");

        // Assert
        assertNull(result);
    }

    // ==================== toFileVO ====================

    @Test
    @DisplayName("转换FileVO - 正常流程")
    void toFileVO_Success() {
        // Arrange
        ProjectFile file = new ProjectFile();
        file.setId(1);
        file.setProjectId(10);
        file.setFileName("test.pdf");
        file.setFileSize(1024L);
        file.setFileType("pdf");
        file.setStorageUrl("https://oss.example.com/files/test.pdf");

        // Act
        FileVO result = DtoConverter.toFileVO(file, "上传者");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test.pdf", result.getFileName());
        assertEquals("1.00 KB", result.getFormattedSize());
        assertEquals("上传者", result.getUploaderUsername());
    }

    @Test
    @DisplayName("转换FileVO - null返回null")
    void toFileVO_Null() {
        // Act
        FileVO result = DtoConverter.toFileVO(null, "uploader");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("转换FileVO - 文件大小为0显示0 B")
    void toFileVO_ZeroSize() {
        // Arrange
        ProjectFile file = new ProjectFile();
        file.setId(1);
        file.setFileSize(0L);

        // Act
        FileVO result = DtoConverter.toFileVO(file, null);

        // Assert
        assertEquals("0 B", result.getFormattedSize());
    }

    @Test
    @DisplayName("转换FileVO - null文件大小显示0 B")
    void toFileVO_NullSize() {
        // Arrange
        ProjectFile file = new ProjectFile();
        file.setId(1);
        file.setFileSize(null);

        // Act
        FileVO result = DtoConverter.toFileVO(file, null);

        // Assert
        assertEquals("0 B", result.getFormattedSize());
    }

    // ==================== toFileVOList ====================

    @Test
    @DisplayName("批量转换FileVO - null返回空列表")
    void toFileVOList_Null() {
        // Act
        List<FileVO> result = DtoConverter.toFileVOList(null, Map.of());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== toNotificationVO ====================

    @Test
    @DisplayName("转换NotificationVO - 正常流程")
    void toNotificationVO_Success() {
        // Arrange
        Notification notif = new Notification();
        notif.setId(1);
        notif.setUserId(100);
        notif.setSenderId(200);
        notif.setProjectId(10);
        notif.setType(1);
        notif.setContent("通知内容");
        notif.setIsRead(0);

        // Act
        NotificationVO result = DtoConverter.toNotificationVO(notif, "发送者", "项目名");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("发送者", result.getSenderUsername());
        assertEquals("项目名", result.getProjectName());
        assertEquals("通知内容", result.getContent());
    }

    @Test
    @DisplayName("转换NotificationVO - null返回null")
    void toNotificationVO_Null() {
        // Act
        NotificationVO result = DtoConverter.toNotificationVO(null, "sender", "project");

        // Assert
        assertNull(result);
    }

    // ==================== toCommentVO ====================

    @Test
    @DisplayName("转换CommentVO - 正常流程（含用户信息）")
    void toCommentVO_WithUserInfo() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(1);
        comment.setUserId(100);
        comment.setProjectId(10);
        comment.setContent("好项目");
        comment.setLikeCount(3);
        comment.setStatus(1);

        Map<Integer, Map<String, Object>> userMap = Map.of(
                100, Map.of("username", "评论者", "avatar", "https://example.com/avatar.png"));

        // Act
        CommentVO result = DtoConverter.toCommentVO(comment, userMap);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("好项目", result.getContent());
        assertEquals("评论者", result.getUsername());
        assertEquals("https://example.com/avatar.png", result.getAvatar());
    }

    @Test
    @DisplayName("转换CommentVO - null返回null")
    void toCommentVO_Null() {
        // Act
        CommentVO result = DtoConverter.toCommentVO(null, Map.of());

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("转换CommentVO - 用户不存在于map中")
    void toCommentVO_UserNotInMap() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(1);
        comment.setUserId(100);

        // Act
        CommentVO result = DtoConverter.toCommentVO(comment, Map.of());

        // Assert
        assertNotNull(result);
        assertNull(result.getUsername());
        assertNull(result.getAvatar());
    }

    // ==================== toCommentVOList ====================

    @Test
    @DisplayName("批量转换CommentVO - null返回空列表")
    void toCommentVOList_Null() {
        // Act
        List<CommentVO> result = DtoConverter.toCommentVOList(null, Map.of());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== toUserVO ====================

    @Test
    @DisplayName("转换UserVO - 正常流程")
    void toUserVO_Success() {
        // Arrange
        User user = new User();
        user.setId(1);
        user.setUsername("用户");
        user.setRealName("真实姓名");
        user.setClassName("软件2101");
        user.setEmail("user@example.com");
        user.setRole(Role.USER);

        // Act
        UserVO result = DtoConverter.toUserVO(user);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("用户", result.getUsername());
        assertEquals("真实姓名", result.getRealName());
        assertEquals("USER", result.getRole());
    }

    @Test
    @DisplayName("转换UserVO - null返回null")
    void toUserVO_Null() {
        // Act
        UserVO result = DtoConverter.toUserVO(null);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("转换UserVO - role为null时role字段为null")
    void toUserVO_NullRole() {
        // Arrange
        User user = new User();
        user.setId(1);
        user.setRole(null);

        // Act
        UserVO result = DtoConverter.toUserVO(user);

        // Assert
        assertNull(result.getRole());
    }

    // ==================== toTeamVO ====================

    @Test
    @DisplayName("转换TeamVO - 正常流程")
    void toTeamVO_Success() {
        // Arrange
        Team team = new Team();
        team.setId(1);
        team.setUserId(100);
        team.setTitle("招募前端");
        team.setStatus(1);
        team.setCurrentMembers(2);
        team.setNeededMembers(5);

        Map<Integer, Map<String, Object>> userMap = Map.of(
                100, Map.of("username", "组长", "avatar", "https://example.com/av.png"));

        // Act
        TeamVO result = DtoConverter.toTeamVO(team, userMap);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("招募前端", result.getTitle());
        assertEquals("招募中", result.getStatusText());
        assertEquals("组长", result.getCreatorUsername());
    }

    @Test
    @DisplayName("转换TeamVO - null返回null")
    void toTeamVO_Null() {
        // Act
        TeamVO result = DtoConverter.toTeamVO(null, Map.of());

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("转换TeamVO - 未知状态码显示'未知'")
    void toTeamVO_UnknownStatus() {
        // Arrange
        Team team = new Team();
        team.setId(1);
        team.setStatus(99);

        // Act
        TeamVO result = DtoConverter.toTeamVO(team, null);

        // Assert
        assertEquals("未知", result.getStatusText());
    }

    // ==================== toTeamVOList ====================

    @Test
    @DisplayName("批量转换TeamVO - null或空返回空列表")
    void toTeamVOList_NullOrEmpty() {
        // Act
        List<TeamVO> nullResult = DtoConverter.toTeamVOList(null, Map.of());
        List<TeamVO> emptyResult = DtoConverter.toTeamVOList(List.of(), Map.of());

        // Assert
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());
    }

    // ==================== toTeamApplicationVO ====================

    @Test
    @DisplayName("转换TeamApplicationVO - 正常流程")
    void toTeamApplicationVO_Success() {
        // Arrange
        TeamApplication app = new TeamApplication();
        app.setId(1);
        app.setTeamId(10);
        app.setApplicantId(200);
        app.setMessage("我想加入");
        app.setStatus(0);

        Map<Integer, Map<String, Object>> userMap = Map.of(
                200, Map.of("username", "申请者", "avatar", "https://example.com/av.png"));

        // Act
        TeamApplicationVO result = DtoConverter.toTeamApplicationVO(app, userMap);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("待审核", result.getStatusText());
        assertEquals("申请者", result.getApplicantUsername());
    }

    @Test
    @DisplayName("转换TeamApplicationVO - null返回null")
    void toTeamApplicationVO_Null() {
        // Act
        TeamApplicationVO result = DtoConverter.toTeamApplicationVO(null, Map.of());

        // Assert
        assertNull(result);
    }

    // ==================== toTeamApplicationVOList ====================

    @Test
    @DisplayName("批量转换TeamApplicationVO - null返回空列表")
    void toTeamApplicationVOList_Null() {
        // Act
        List<TeamApplicationVO> result = DtoConverter.toTeamApplicationVOList(null, Map.of());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
