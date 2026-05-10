package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.entity.Course;
import com.bjutzxq.server.annotation.RequireRole;
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
     * 获取所有课程（用于前端下拉框）
     */
    @GetMapping("/active")
    public Result<List<Course>> getActiveCourses() {
        List<Course> courses = courseService.getAllCourses();
        return Result.success(courses);
    }

    /**
     * 获取所有课程（管理员使用）
     */
    @RequireRole({Role.ADMIN, Role.TEACHER})
    @GetMapping("/all")
    public Result<List<Course>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return Result.success(courses);
    }

    /**
     * 搜索课程
     */
    @GetMapping("/search")
    public Result<List<Course>> searchCourses(@RequestParam String keyword) {
        List<Course> courses = courseService.searchCourses(keyword);
        return Result.success(courses);
    }

    /**
     * 创建课程（管理员使用）
     */
    @RequireRole({Role.ADMIN, Role.TEACHER})
    @PostMapping
    public Result<Course> createCourse(@RequestBody Map<String, String> request) {
        String courseName = request.get("courseName");
        Course course = courseService.createCourse(courseName);
        return Result.success(course);
    }

    /**
     * 更新课程（管理员使用）
     */
    @RequireRole({Role.ADMIN, Role.TEACHER})
    @PutMapping("/{id}")
    public Result<Course> updateCourse(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request
    ) {
        String courseName = request.get("courseName");
        Course course = courseService.updateCourse(id, courseName);
        return Result.success(course);
    }

    /**
     * 删除课程（管理员使用）
     */
    @RequireRole({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Integer id) {
        courseService.deleteCourse(id);
        return Result.success(null);
    }
}
