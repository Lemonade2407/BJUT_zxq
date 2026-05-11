package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.Course;
import com.bjutzxq.server.mapper.CourseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("课程服务测试")
class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId(1);
        testCourse.setCourseName("软件工程");
    }

    // ==================== createCourse ====================

    @Test
    @DisplayName("创建课程成功 - 正常流程")
    void createCourse_Success() {
        // Arrange
        String courseName = "软件工程";
        when(courseMapper.selectByCourseName(courseName)).thenReturn(null);
        when(courseMapper.insert(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(1);
            return 1;
        });

        // Act
        Course result = courseService.createCourse(courseName);

        // Assert
        assertNotNull(result);
        assertEquals(courseName, result.getCourseName());
        assertEquals(1, result.getId());
        verify(courseMapper).selectByCourseName(courseName);
        verify(courseMapper).insert(any(Course.class));
    }

    @Test
    @DisplayName("创建课程失败 - 课程名称为空")
    void createCourse_EmptyName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(""));
        assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(null));
        verify(courseMapper, never()).insert(any(Course.class));
    }

    @Test
    @DisplayName("创建课程失败 - 课程名称已存在")
    void createCourse_DuplicateName() {
        // Arrange
        String courseName = "软件工程";
        when(courseMapper.selectByCourseName(courseName)).thenReturn(testCourse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.createCourse(courseName));
        assertEquals("课程已存在：" + courseName, exception.getMessage());
        verify(courseMapper, never()).insert(any(Course.class));
    }

    // ==================== deleteCourse ====================

    @Test
    @DisplayName("删除课程成功 - 正常流程")
    void deleteCourse_Success() {
        // Arrange
        when(courseMapper.deleteById(1)).thenReturn(1);

        // Act
        courseService.deleteCourse(1);

        // Assert
        verify(courseMapper).deleteById(1);
    }

    @Test
    @DisplayName("删除课程失败 - 课程不存在")
    void deleteCourse_NotFound() {
        // Arrange
        when(courseMapper.deleteById(99)).thenReturn(0);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.deleteCourse(99));
        assertEquals("课程不存在", exception.getMessage());
    }

    // ==================== updateCourse ====================

    @Test
    @DisplayName("更新课程成功 - 正常流程")
    void updateCourse_Success() {
        // Arrange
        when(courseMapper.selectById(1)).thenReturn(testCourse);
        when(courseMapper.update(any(Course.class))).thenReturn(1);

        // Act
        Course result = courseService.updateCourse(1, "数据结构");

        // Assert
        assertNotNull(result);
        assertEquals("数据结构", result.getCourseName());
        verify(courseMapper).selectById(1);
        verify(courseMapper).update(any(Course.class));
    }

    @Test
    @DisplayName("更新课程 - 名称未改变不检查重名")
    void updateCourse_SameName() {
        // Arrange
        when(courseMapper.selectById(1)).thenReturn(testCourse);
        when(courseMapper.update(any(Course.class))).thenReturn(1);

        // Act
        Course result = courseService.updateCourse(1, "软件工程");

        // Assert
        assertNotNull(result);
        verify(courseMapper, never()).selectByCourseName(anyString());
        verify(courseMapper).update(any(Course.class));
    }

    @Test
    @DisplayName("更新课程失败 - 课程名称为空")
    void updateCourse_EmptyName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> courseService.updateCourse(1, ""));
        assertThrows(IllegalArgumentException.class,
                () -> courseService.updateCourse(1, null));
    }

    @Test
    @DisplayName("更新课程失败 - 课程不存在")
    void updateCourse_NotFound() {
        // Arrange
        when(courseMapper.selectById(99)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.updateCourse(99, "测试课程"));
        assertEquals("课程不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新课程失败 - 新名称已被使用")
    void updateCourse_DuplicateName() {
        // Arrange
        Course otherCourse = new Course();
        otherCourse.setId(2);
        otherCourse.setCourseName("数据结构");
        when(courseMapper.selectById(1)).thenReturn(testCourse);
        when(courseMapper.selectByCourseName("数据结构")).thenReturn(otherCourse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.updateCourse(1, "数据结构"));
        assertEquals("课程名称已被使用：数据结构", exception.getMessage());
    }

    // ==================== getAllCourses ====================

    @Test
    @DisplayName("查询所有课程成功 - 正常流程")
    void getAllCourses_Success() {
        // Arrange
        List<Course> courses = List.of(testCourse);
        when(courseMapper.selectAllCourses()).thenReturn(courses);

        // Act
        List<Course> result = courseService.getAllCourses();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseMapper).selectAllCourses();
    }

    @Test
    @DisplayName("查询所有课程 - 空结果")
    void getAllCourses_Empty() {
        // Arrange
        when(courseMapper.selectAllCourses()).thenReturn(List.of());

        // Act
        List<Course> result = courseService.getAllCourses();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== searchCourses ====================

    @Test
    @DisplayName("搜索课程成功 - 正常关键字")
    void searchCourses_WithKeyword() {
        // Arrange
        List<Course> courses = List.of(testCourse);
        when(courseMapper.searchCourses("软件")).thenReturn(courses);

        // Act
        List<Course> result = courseService.searchCourses("软件");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseMapper).searchCourses("软件");
    }

    @Test
    @DisplayName("搜索课程 - 空关键字返回全部")
    void searchCourses_EmptyKeyword() {
        // Arrange
        List<Course> courses = List.of(testCourse);
        when(courseMapper.selectAllCourses()).thenReturn(courses);

        // Act
        List<Course> result = courseService.searchCourses("");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseMapper).selectAllCourses();
        verify(courseMapper, never()).searchCourses(anyString());
    }

    @Test
    @DisplayName("搜索课程 - null关键字返回全部")
    void searchCourses_NullKeyword() {
        // Arrange
        when(courseMapper.selectAllCourses()).thenReturn(List.of());

        // Act
        List<Course> result = courseService.searchCourses(null);

        // Assert
        assertNotNull(result);
        verify(courseMapper).selectAllCourses();
    }
}
