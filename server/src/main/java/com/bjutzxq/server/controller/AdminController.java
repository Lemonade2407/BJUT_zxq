package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.annotation.RequireRole;
import com.bjutzxq.server.service.ProjectService;
import com.bjutzxq.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * 获取所有用户（仅管理员）
     * GET /api/admin/users?pageNum=1&pageSize=20
     */
    @GetMapping("/users")
    @RequireRole(Role.ADMIN)
    public Result<List<User>> getAllUsers(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("管理员获取所有用户，页码：{}, 每页数量：{}", pageNum, pageSize);
        List<User> users = userService.queryUsers(null, null, pageNum, pageSize);
        return Result.success(users);
    }
    
    /**
     * 搜索用户（仅管理员）
     * GET /api/admin/users/search?keyword=张三&pageNum=1&pageSize=20
     */
    @GetMapping("/users/search")
    @RequireRole(Role.ADMIN)
    public Result<List<User>> searchUsers(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("管理员搜索用户，关键词：{}, 页码：{}, 每页数量：{}", keyword, pageNum, pageSize);
        List<User> users = userService.queryUsers(null, keyword, pageNum, pageSize);
        return Result.success(users);
    }
    
    /**
     * 获取学生列表（教师和管理员）
     * GET /api/admin/students?pageNum=1&pageSize=20
     */
    @GetMapping("/students")
    @RequireRole({Role.ADMIN, Role.TEACHER})
    public Result<List<User>> getStudents(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("获取学生列表，页码：{}, 每页数量：{}", pageNum, pageSize);
        List<User> students = userService.queryUsers(Role.USER, null, pageNum, pageSize);
        return Result.success(students);
    }
    
    /**
     * 搜索学生（教师和管理员）
     * GET /api/admin/students/search?keyword=张三&pageNum=1&pageSize=20
     */
    @GetMapping("/students/search")
    @RequireRole({Role.ADMIN, Role.TEACHER})
    public Result<List<User>> searchStudents(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("搜索学生，关键词：{}, 页码：{}, 每页数量：{}", keyword, pageNum, pageSize);
        List<User> students = userService.queryUsers(Role.USER, keyword, pageNum, pageSize);
        return Result.success(students);
    }
    
    /**
     * 封禁用户（仅管理员）
     * PUT /api/admin/users/{id}/ban
     */
    @PutMapping("/users/{id}/ban")
    @RequireRole(Role.ADMIN)
    public Result<Void> banUser(@PathVariable Integer id) {
        log.info("封禁用户，ID: {}", id);
        userService.banUser(id);
        return Result.success("用户已封禁", null);
    }
    
    /**
     * 解封用户（仅管理员）
     * PUT /api/admin/users/{id}/unban
     */
    @PutMapping("/users/{id}/unban")
    @RequireRole(Role.ADMIN)
    public Result<Void> unbanUser(@PathVariable Integer id) {
        log.info("解封用户，ID: {}", id);
        userService.unbanUser(id);
        return Result.success("用户已解封", null);
    }
    
    /**
     * 设置用户角色（仅管理员）
     * PUT /api/admin/users/{id}/role
     */
    @PutMapping("/users/{id}/role")
    @RequireRole(Role.ADMIN)
    public Result<Void> setUserRole(
            @PathVariable Integer id,
            @RequestParam Integer roleCode) {
        log.info("设置用户角色，ID: {}, 角色代码：{}", id, roleCode);
        Role role = Role.valueOf(roleCode);
        userService.setUserRole(id, role);
        return Result.success("角色设置成功", null);
    }
    
    /**
     * 更新用户信息（仅管理员）
     * PUT /api/admin/users/{id}
     */
    @PutMapping("/users/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> updateUser(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> userInfo) {
        log.info("管理员更新用户信息，ID: {}", id);
        
        String username = (String) userInfo.get("username");
        String employeeId = (String) userInfo.get("employeeId");
        String realName = (String) userInfo.get("realName");
        String email = (String) userInfo.get("email");
        String password = (String) userInfo.get("password");
        Integer gender = userInfo.get("gender") != null ? (Integer) userInfo.get("gender") : null;
        String bio = (String) userInfo.get("bio");
        String className = (String) userInfo.get("className");
        String roleStr = (String) userInfo.get("role");
        
        Role role = roleStr != null ? Role.valueOf(roleStr) : null;
        
        userService.updateUserByAdmin(id, username, employeeId, realName, email, 
                                      password, gender, bio, className, role);
        
        return Result.success("用户信息更新成功", null);
    }
    
    /**
     * 更新项目信息（仅管理员）
     * PUT /api/admin/projects/{id}
     */
    @PutMapping("/projects/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> updateProject(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> projectInfo) {
        log.info("管理员更新项目信息，ID: {}", id);
        
        // 获取现有项目
        Project existingProject = projectService.selectById(id);
        if (existingProject == null) {
            return Result.error("项目不存在");
        }
        
        // 更新字段
        String name = (String) projectInfo.get("name");
        String description = (String) projectInfo.get("description");
        String projectType = (String) projectInfo.get("projectType");
        Integer visibility = projectInfo.get("visibility") != null ? 
                            (Integer) projectInfo.get("visibility") : existingProject.getVisibility();
        String courseName = (String) projectInfo.get("courseName");
        
        if (name != null) existingProject.setName(name);
        if (description != null) existingProject.setDescription(description);
        if (projectType != null) existingProject.setProjectType(projectType);
        if (courseName != null) existingProject.setCourseName(courseName);
        existingProject.setVisibility(visibility);
        
        // 处理标签 ID 列表
        List<Integer> tagIds = null;
        if (projectInfo.containsKey("tagIds")) {
            @SuppressWarnings("unchecked")
            List<Object> rawTagIds = (List<Object>) projectInfo.get("tagIds");
            if (rawTagIds != null) {
                tagIds = rawTagIds.stream()
                    .map(obj -> ((Number) obj).intValue())
                    .collect(java.util.stream.Collectors.toList());
            }
        }
        
        // 保存更新
        projectService.updateProject(existingProject, tagIds);
        
        return Result.success("项目信息更新成功", null);
    }
    
    /**
     * 删除用户（仅管理员）
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> deleteUser(@PathVariable Integer id) {
        log.info("删除用户，ID: {}", id);
        userService.deleteUser(id);
        return Result.success("用户已删除", null);
    }
    
    /**
     * 查看系统统计信息（仅管理员）
     * GET /api/admin/statistics
     */
    @GetMapping("/statistics")
    @RequireRole({Role.ADMIN, Role.TEACHER})
    public Result<Map<String, Object>> getStatistics() {
        log.info("获取系统统计信息");
        Map<String, Object> statistics = userService.getStatistics();
        return Result.success(statistics);
    }
}
