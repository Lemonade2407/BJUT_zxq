package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.Course;
import com.bjutzxq.server.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程字典控制器
 */
@Slf4j
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 获取所有启用的课程（用于前端下拉框）
     */
    @GetMapping("/active")
    public Result<List<Course>> getActiveCourses() {
        try {
            List<Course> courses = courseService.getActiveCourses();
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取启用课程失败", e);
            return Result.error("获取课程列表失败");
        }
    }

    /**
     * 获取所有课程（管理员使用）
     */
    @GetMapping("/all")
    public Result<List<Course>> getAllCourses() {
        try {
            List<Course> courses = courseService.getAllCourses();
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取所有课程失败", e);
            return Result.error("获取课程列表失败");
        }
    }

    /**
     * 搜索课程
     */
    @GetMapping("/search")
    public Result<List<Course>> searchCourses(@RequestParam String keyword) {
        try {
            List<Course> courses = courseService.searchCourses(keyword);
            return Result.success(courses);
        } catch (Exception e) {
            log.error("搜索课程失败", e);
            return Result.error("搜索课程失败");
        }
    }

    /**
     * 创建课程（管理员使用）
     */
    @PostMapping
    public Result<Course> createCourse(@RequestBody Map<String, String> request) {
        try {
            String courseName = request.get("courseName");
            if (courseName == null || courseName.trim().isEmpty()) {
                return Result.error("课程名称不能为空");
            }

            Course course = courseService.createCourse(courseName.trim());
            return Result.success(course);
        } catch (RuntimeException e) {
            log.warn("创建课程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建课程失败", e);
            return Result.error("创建课程失败");
        }
    }

    /**
     * 更新课程（管理员使用）
     */
    @PutMapping("/{id}")
    public Result<Course> updateCourse(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String courseName = (String) request.get("courseName");
            Integer isActive = (Integer) request.get("isActive");

            if (courseName == null || courseName.trim().isEmpty()) {
                return Result.error("课程名称不能为空");
            }

            Course course = courseService.updateCourse(id, courseName.trim(), isActive);
            return Result.success(course);
        } catch (RuntimeException e) {
            log.warn("更新课程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新课程失败", e);
            return Result.error("更新课程失败");
        }
    }

    /**
     * 删除课程（管理员使用）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Integer id) {
        try {
            courseService.deleteCourse(id);
            return Result.success(null);
        } catch (RuntimeException e) {
            log.warn("删除课程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除课程失败", e);
            return Result.error("删除课程失败");
        }
    }
}
