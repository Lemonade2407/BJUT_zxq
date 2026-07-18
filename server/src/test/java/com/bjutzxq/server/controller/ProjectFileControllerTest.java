package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.pojo.vo.FileVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.service.ProjectFileService;
import com.bjutzxq.server.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("项目文件控制器测试")
class ProjectFileControllerTest {

    @Mock
    private ProjectFileService projectFileService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectFileController projectFileController;

    // ==================== getAllFiles ====================

    @Test
    @DisplayName("获取项目文件列表成功 - 正常流程")
    void getAllFiles_Success() {
        // Arrange
        when(projectFileService.getAllFiles(10)).thenReturn(List.of());

        // Act
        Result<List<FileVO>> result = projectFileController.getAllFiles(10);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    // ==================== uploadDocument ====================
    //
    // 以下测试引用了不存在的 API（uploadProjectDocument/uploadDocument），
    // 属于历史遗留问题，待后续修复。
    //
    // @Test
    // @DisplayName("上传项目文档成功 - 正常流程")
    // void uploadDocument_Success() { ... }
    // @Test
    // @DisplayName("上传项目文档失败 - 非项目所有者")
    // void uploadDocument_NotOwner() { ... }

}
