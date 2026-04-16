package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.User;
import com.bjutzxq.server.annotation.RequireRole;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.service.UserService;
import com.github.pagehelper.PageHelper;
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
    private UserMapper userMapper;
    
    /**
     * 获取所有用户（仅管理员）
     * GET /api/admin/users?pageNum=1&pageSize=20
     */
    @GetMapping("/users")
    @RequireRole({Role.ADMIN, Role.TEACHER})
    public Result<List<User>> getAllUsers(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("获取所有用户，页码：{}, 每页数量：{}", pageNum, pageSize);
        
        try {
            // 设置分页
            PageHelper.startPage(pageNum, pageSize);
            List<User> users = userMapper.selectAll();
            
            // 清除密码字段
            users.forEach(user -> user.setPassword(null));
            
            log.info("获取用户成功，数量：{}", users.size());
            return Result.success(users);
        } catch (Exception e) {
            log.error("获取用户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 搜索用户（仅管理员）
     * GET /api/admin/users/search?keyword=张三&pageNum=1&pageSize=20
     */
    @GetMapping("/users/search")
    @RequireRole({Role.ADMIN, Role.TEACHER})
    public Result<List<User>> searchUsers(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        log.info("搜索用户，关键词：{}, 页码：{}, 每页数量：{}", keyword, pageNum, pageSize);
        
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.error("搜索关键词不能为空");
            }
            
            // 设置分页
            PageHelper.startPage(pageNum, pageSize);
            List<User> users = userMapper.searchByKeyword(keyword.trim());
            
            // 清除密码字段
            users.forEach(user -> user.setPassword(null));
            
            log.info("搜索用户成功，数量：{}", users.size());
            return Result.success(users);
        } catch (Exception e) {
            log.error("搜索用户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 封禁用户（仅管理员）
     * PUT /api/admin/users/{id}/ban
     */
    @PutMapping("/users/{id}/ban")
    @RequireRole(Role.ADMIN)
    public Result<Void> banUser(@PathVariable Integer id) {
        log.info("封禁用户，ID: {}", id);
        
        try {
            userService.banUser(id);
            return Result.success("用户已封禁", null);
        } catch (Exception e) {
            log.error("封禁用户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 解封用户（仅管理员）
     * PUT /api/admin/users/{id}/unban
     */
    @PutMapping("/users/{id}/unban")
    @RequireRole(Role.ADMIN)
    public Result<Void> unbanUser(@PathVariable Integer id) {
        log.info("解封用户，ID: {}", id);
        
        try {
            userService.unbanUser(id);
            return Result.success("用户已解封", null);
        } catch (Exception e) {
            log.error("解封用户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 设置用户角色（仅管理员）
     * PUT /api/admin/users/{id}/role
     */
    @PutMapping("/users/{id}/role")
    @RequireRole(Role.ADMIN)
    public Result<Void> setUserRole(
            @PathVariable Integer id,
            @RequestParam(required = true) Integer roleCode) {
        log.info("设置用户角色，ID: {}, 角色代码：{}", id, roleCode);
        
        try {
            Role role = Role.valueOf(roleCode);
            userService.setUserRole(id, role);
            return Result.success("角色设置成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("无效的角色代码：{}", roleCode);
            return Result.error("无效的角色代码");
        } catch (Exception e) {
            log.error("设置角色失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除用户（仅管理员）
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> deleteUser(@PathVariable Integer id) {
        log.info("删除用户，ID: {}", id);
        
        try {
            userService.deleteUser(id);
            return Result.success("用户已删除", null);
        } catch (Exception e) {
            log.error("删除用户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 查看系统统计信息（仅管理员）
     * GET /api/admin/statistics
     */
    @GetMapping("/statistics")
    @RequireRole({Role.ADMIN, Role.TEACHER})
    public Result<Map<String, Object>> getStatistics() {
        log.info("获取系统统计信息");
        
        try {
            Map<String, Object> statistics = userService.getStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取统计信息失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
