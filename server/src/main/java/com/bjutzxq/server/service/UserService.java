package com.bjutzxq.server.service;
import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.ProjectFile;
import com.bjutzxq.pojo.User;
import com.bjutzxq.server.mapper.CommentMapper;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.util.JwtUtil;
import com.bjutzxq.server.util.OssUtil;
import com.bjutzxq.server.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务类
 */
@Slf4j
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private ProjectFileMapper projectFileMapper;
    
    @Autowired
    private OssUtil ossUtil;
    
    /**
     * 用户注册
     * @param user 用户对象
     * @return 注册后的用户信息（不含密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public User register(User user) {
        log.info("开始注册用户，用户名：{}", user.getUsername());
        
        // 1. 参数验证
        validateRegisterUser(user);
        
        // 2. 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            log.warn("用户名已存在：{}", user.getUsername());
            throw new RuntimeException("用户名已存在");
        }
        
        // 3. 检查学号是否已存在
        if (user.getStudentId() == null || user.getStudentId().trim().isEmpty()) {
            log.warn("注册失败：学号为空");
            throw new RuntimeException("学号不能为空");
        }
        existingUser = userMapper.selectByStudentId(user.getStudentId().trim());
        if (existingUser != null) {
            log.warn("学号已被使用：{}", user.getStudentId());
            throw new RuntimeException("学号已被使用");
        }
        
        // 4. 检查邮箱是否已存在
        existingUser = userMapper.selectByEmail(user.getEmail());
        if (existingUser != null) {
            log.warn("邮箱已被使用：{}", user.getEmail());
            throw new RuntimeException("邮箱已被使用");
        }
        
        // 5. 密码加密（BCrypt）
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        // 6. 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(Constants.User.STATUS_NORMAL);
        }
        if (user.getSex() == null || user.getSex().isEmpty()) {
            user.setSex(Constants.User.SEX_UNKNOWN);
        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        // 设置默认头像
        if (user.getAvatar() == null || user.getAvatar().trim().isEmpty()) {
            user.setAvatar("/bjut-logo.svg");
        }
        
        // 7. 插入用户
        userMapper.insert(user);
        
        log.info("用户注册成功，ID：{}", user.getId());
        return user;
    }
    
    /**
     * 验证注册用户信息
     * @param user 用户对象
     */
    private void validateRegisterUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        
        // 验证用户名
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (user.getUsername().length() < 2 || user.getUsername().length() > 20) {
            throw new IllegalArgumentException("用户名长度应为 2-20 位");
        }
        
        // 验证密码
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 20) {
            throw new IllegalArgumentException("密码长度应为 6-20 位");
        }
        // 密码必须包含字母和数字
        if (!user.getPassword().matches("^(?=.*[a-zA-Z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException("密码必须包含字母和数字");
        }
        
        // 验证学号
        if (user.getStudentId() == null || user.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("学号不能为空");
        }
        // 学号格式：8位数字 或 1位字母+8位数字
        if (!user.getStudentId().matches("^([A-Za-z]?\\d{8})$")) {
            throw new IllegalArgumentException("学号格式不正确");
        }
        
        // 验证邮箱
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        // 简单的邮箱格式验证
        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        
        // 验证手机号（如果提供）
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
        }
    }
    
    /**
     * 用户登录
     * @param username 用户名或邮箱
     * @param password 密码
     * @return 登录信息（包含 token 和用户信息）
     */
    public Map<String, Object> login(String username, String password) {
        log.info("用户登录，用户名/邮箱/学号：{}", username);
        
        // 查询用户（可能是用户名、邮箱或学号）
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            user = userMapper.selectByEmail(username);
        }
        if (user == null) {
            user = userMapper.selectByStudentId(username);
        }
        
        if (user == null) {
            log.warn("用户不存在：{}", username);
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (!Constants.User.STATUS_NORMAL.equals(user.getStatus())) {
            log.warn("账号已被禁用：{}", username);
            throw new RuntimeException("账号已被禁用");
        }
        
        // 验证密码（BCrypt）
        if (!PasswordUtil.matches(password, user.getPassword())) {
            log.warn("密码错误：{}", username);
            throw new RuntimeException("密码错误");
        }
        
        // 生成 Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getAvatar());
        
        log.info("用户登录成功：{}, ID: {}", username, user.getId());
        
        // 返回登录信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("expiresIn", Constants.JWT.TOKEN_EXPIRE_TIME / 1000); // 秒
        
        // 返回脱敏的用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        result.put("user", userInfo);
        
        return result;
    }
    
    /**
     * 退出登录
     * （JWT 无状态，前端删除 token 即可）
     */
    public void logout() {
        // JWT 不需要服务端处理退出
    }
    
    /**
     * 获取当前用户信息
     * @param userId 用户 ID
     * @return 用户信息
     */
    public User getCurrentUser(Integer userId) {
        log.debug("获取用户信息，ID：{}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在，ID：{}", userId);
            throw new RuntimeException("用户不存在");
        }
        // 清除密码
        user.setPassword(null);
        return user;
    }
    
    /**
     * 更新用户信息
     * @param userId 用户 ID
     * @param user 新的用户信息
     * @return 更新后的用户
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Integer userId, User user) {
        log.info("更新用户信息，ID：{}", userId);
        
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            log.warn("用户不存在，ID：{}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 只允许更新部分字段
        if (user.getAvatar() != null) {
            existingUser.setAvatar(user.getAvatar());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getSex() != null) {
            existingUser.setSex(user.getSex());
        }
        if (user.getBio() != null) {
            existingUser.setBio(user.getBio());
        }
        
        userMapper.update(existingUser);
        
        log.info("用户信息更新成功，ID：{}", userId);
        existingUser.setPassword(null);
        return existingUser;
    }
    
    /**
     * 修改密码
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        log.info("修改密码，ID：{}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在，ID：{}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码（BCrypt）
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            log.warn("原密码错误，ID：{}", userId);
            throw new RuntimeException("原密码错误");
        }
        
        // 加密新密码（BCrypt）
        String encodedNewPassword = PasswordUtil.encode(newPassword);
        user.setPassword(encodedNewPassword);
        
        userMapper.update(user);
        
        log.info("密码修改成功，ID：{}", userId);
    }
    
    /**
     * 检查用户是否为管理员
     * @param userId 用户 ID
     * @return true-是管理员，false-非管理员
     */
    public boolean isAdmin(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() == Role.ADMIN;
    }
    
    /**
     * 封禁用户（仅管理员可调用）
     * @param userId 要封禁的用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Integer userId) {
        log.info("开始封禁用户，ID: {}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("封禁失败：用户不存在，ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(Constants.User.STATUS_DISABLED);
        userMapper.update(user);
        
        log.info("用户已封禁，ID: {}", userId);
    }
    
    /**
     * 解封用户（仅管理员可调用）
     * @param userId 要解封的用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Integer userId) {
        log.info("开始解封用户，ID: {}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("解封失败：用户不存在，ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(Constants.User.STATUS_NORMAL);
        userMapper.update(user);
        
        log.info("用户已解封，ID: {}", userId);
    }
    
    /**
     * 设置用户角色（仅管理员可调用）
     * @param userId 用户 ID
     * @param role 新角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void setUserRole(Integer userId, Role role) {
        log.info("开始设置用户角色，ID: {}, 角色：{}", userId, role);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("设置角色失败：用户不存在，ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        user.setRole(role);
        userMapper.update(user);
        
        log.info("用户角色已设置，ID: {}, 角色：{}", userId, role);
    }
    
    /**
     * 删除用户（级联删除相关数据）
     * @param userId 要删除的用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer userId) {
        log.info("开始删除用户，ID: {}", userId);
        
        // TODO: 考虑软删除而非硬删除，保留审计日志
        
        // 1. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("删除失败：用户不存在，ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 防止删除管理员自己
        if (user.getRole() == Role.ADMIN) {
            log.warn("不能删除管理员账户，ID: {}", userId);
            throw new RuntimeException("不能删除管理员账户");
        }
        
        // 3. 清理该用户上传的所有文件（OSS）
        cleanupUserFiles(userId);
        
        // 4. 删除该用户的所有评论
        int commentCount = commentMapper.deleteByUserId(userId);
        log.info("删除用户评论，数量：{}", commentCount);
        
        // 5. 删除该用户的所有项目（会级联删除项目文件、点赞、关注等）
        int projectCount = projectMapper.deleteByUserId(userId);
        log.info("删除用户项目，数量：{}", projectCount);
        
        // 6. 最后删除用户
        userMapper.deleteById(userId);
        
        log.info("用户删除成功，ID: {}", userId);
    }
    
    /**
     * 清理用户上传的所有文件（OSS + 本地存储）
     * @param userId 用户 ID
     */
    private void cleanupUserFiles(Integer userId) {
        log.info("开始清理用户上传的文件，用户 ID: {}", userId);
        
        try {
            // 查询该用户上传的所有文件
            List<ProjectFile> userFiles = projectFileMapper.selectByUploaderId(userId);
            
            if (userFiles == null || userFiles.isEmpty()) {
                log.info("该用户没有上传任何文件");
                return;
            }
            
            int ossDeletedCount = 0;
            int localDeletedCount = 0;
            
            for (ProjectFile file : userFiles) {
                // 跳过目录
                if (file.getIsDir() != null && Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
                    continue;
                }
                
                // 根据存储类型清理
                if (file.getStorageType() != null && Integer.valueOf(2).equals(file.getStorageType())) {
                    // OSS 存储
                    if (file.getStorageUrl() != null && file.getStorageUrl().startsWith("http")) {
                        try {
                            ossUtil.delete(file.getStorageUrl());
                            ossDeletedCount++;
                            log.debug("已删除 OSS 文件: {}", file.getStorageUrl());
                        } catch (Exception e) {
                            log.error("删除 OSS 文件失败: {}, 错误: {}", file.getStorageUrl(), e.getMessage());
                        }
                    }
                } else {
                    // 本地存储
                    if (file.getStorageUrl() != null) {
                        try {
                            // 构建物理文件路径
                            String physicalPath = System.getProperty("java.io.tmpdir") + "/project_files/" + 
                                file.getProjectId() + "/" + new File(file.getStorageUrl()).getName();
                            Path path = Paths.get(physicalPath);
                            
                            if (Files.exists(path)) {
                                Files.delete(path);
                                localDeletedCount++;
                                log.debug("已删除本地文件: {}", physicalPath);
                            }
                        } catch (IOException e) {
                            log.error("删除本地文件失败: {}, 错误: {}", file.getStorageUrl(), e.getMessage());
                        }
                    }
                }
            }
            
            log.info("文件清理完成，OSS 文件: {}, 本地文件: {}", ossDeletedCount, localDeletedCount);
            
        } catch (Exception e) {
            log.error("清理用户文件失败，用户 ID: {}, 错误: {}", userId, e.getMessage(), e);
            // 不抛出异常，继续执行其他清理操作
        }
    }
    
    /**
     * 获取系统统计信息
     * @return 统计数据 Map
     */
    public java.util.Map<String, Object> getStatistics() {
        log.info("获取系统统计信息");
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // 用户总数
        int userCount = userMapper.countAll();
        stats.put("userCount", userCount);
        
        // 项目总数
        int projectCount = projectMapper.countAll();
        stats.put("projectCount", projectCount);
        
        // 评论总数
        int commentCount = commentMapper.countAll();
        stats.put("commentCount", commentCount);
        
        log.info("统计信息：用户={}, 项目={}, 评论={}", userCount, projectCount, commentCount);
        
        return stats;
    }
    
    /**
     * 上传用户头像
     * @param userId 用户 ID
     * @param file 头像文件
     * @return 头像 URL
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Integer userId, org.springframework.web.multipart.MultipartFile file) {
        log.info("开始上传头像，用户 ID: {}", userId);
        
        // 1. 验证用户存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 验证文件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的头像文件");
        }
        
        // 3. 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }
        
        // 4. 验证文件大小（最大 5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("头像文件大小不能超过 5MB");
        }
        
        try {
            // 5. 删除旧头像（如果不是默认头像）
            String oldAvatar = user.getAvatar();
            if (oldAvatar != null && !oldAvatar.equals("/bjut-logo.svg") && !oldAvatar.trim().isEmpty()) {
                try {
                    ossUtil.delete(oldAvatar);
                    log.info("已删除旧头像: {}", oldAvatar);
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}, 错误: {}", oldAvatar, e.getMessage());
                    // 不抛出异常，继续上传新头像
                }
            }
            
            // 6. 上传新头像到 OSS
            String avatarUrl = ossUtil.upload(file);
            log.info("头像上传成功: {}", avatarUrl);
            
            // 7. 更新用户头像
            user.setAvatar(avatarUrl);
            userMapper.update(user);
            
            log.info("用户头像更新成功，用户 ID: {}", userId);
            return avatarUrl;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }
}
