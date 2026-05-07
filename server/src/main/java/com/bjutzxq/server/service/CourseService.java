package com.bjutzxq.server.service;

import com.bjutzxq.pojo.entity.Course;
import com.bjutzxq.server.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 课程字典服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;

    /**
     * 创建课程
     */
    @Transactional
    public Course createCourse(String courseName) {
        // 参数验证
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("课程名称不能为空");
        }
        courseName = courseName.trim();

        // 检查课程是否已存在
        Course existing = courseMapper.selectByCourseName(courseName);
        if (existing != null) {
            throw new RuntimeException("课程已存在：" + courseName);
        }

        Course course = new Course();
        course.setCourseName(courseName);

        courseMapper.insert(course);
        log.info("创建课程成功: {}", courseName);
        return course;
    }

    /**
     * 删除课程
     */
    @Transactional
    public void deleteCourse(Integer id) {
        int result = courseMapper.deleteById(id);
        if (result == 0) {
            throw new RuntimeException("课程不存在");
        }
        log.info("删除课程成功, ID: {}", id);
    }

    /**
     * 更新课程
     */
    @Transactional
    public Course updateCourse(Integer id, String courseName) {
        // 参数验证
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("课程名称不能为空");
        }
        courseName = courseName.trim();

        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        // 如果修改了课程名称，检查是否与其他课程重复
        if (!course.getCourseName().equals(courseName)) {
            Course existing = courseMapper.selectByCourseName(courseName);
            if (existing != null && !existing.getId().equals(id)) {
                throw new RuntimeException("课程名称已被使用：" + courseName);
            }
        }

        course.setCourseName(courseName);
        courseMapper.update(course);

        log.info("更新课程成功, ID: {}", id);
        return course;
    }
    /**
     * 查询所有课程
     */
    public List<Course> getAllCourses() {
        return courseMapper.selectAllCourses();
    }

    /**
     * 搜索课程
     */
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCourses();
        }
        return courseMapper.searchCourses(keyword.trim());
    }
}
