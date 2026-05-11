package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.entity.Course;
import com.bjutzxq.server.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("课程控制器测试")
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId(1);
        testCourse.setCourseName("软件工程");
    }

    @Test
    @DisplayName("获取活跃课程成功 - 正常流程")
    void getActiveCourses_Success() {
        // Arrange
        when(courseService.getAllCourses()).thenReturn(List.of(testCourse));

        // Act
        Result<List<Course>> result = courseController.getActiveCourses();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("获取所有课程成功 - 正常流程")
    void getAllCourses_Success() {
        // Arrange
        when(courseService.getAllCourses()).thenReturn(List.of(testCourse));

        // Act
        Result<List<Course>> result = courseController.getAllCourses();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("搜索课程成功 - 正常流程")
    void searchCourses_Success() {
        // Arrange
        when(courseService.searchCourses("软件")).thenReturn(List.of(testCourse));

        // Act
        Result<List<Course>> result = courseController.searchCourses("软件");

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("创建课程成功 - 正常流程")
    void createCourse_Success() {
        // Arrange
        Map<String, String> request = Map.of("courseName", "数据结构");
        when(courseService.createCourse("数据结构")).thenReturn(testCourse);

        // Act
        Result<Course> result = courseController.createCourse(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(courseService).createCourse("数据结构");
    }

    @Test
    @DisplayName("更新课程成功 - 正常流程")
    void updateCourse_Success() {
        // Arrange
        Map<String, String> request = Map.of("courseName", "数据结构");
        when(courseService.updateCourse(1, "数据结构")).thenReturn(testCourse);

        // Act
        Result<Course> result = courseController.updateCourse(1, request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(courseService).updateCourse(1, "数据结构");
    }

    @Test
    @DisplayName("删除课程成功 - 正常流程")
    void deleteCourse_Success() {
        // Arrange
        doNothing().when(courseService).deleteCourse(1);

        // Act
        Result<Void> result = courseController.deleteCourse(1);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(courseService).deleteCourse(1);
    }
}
