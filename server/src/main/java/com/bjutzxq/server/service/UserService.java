package com.bjutzxq.server.service;
import com.bjutzxq.common.Constants;
import com.bjutzxq.common.Role;
import com.bjutzxq.pojo.vo.LoginVO;
import com.bjutzxq.pojo.entity.ProjectFile;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.mapper.CommentMapper;
import com.bjutzxq.server.mapper.ProjectFileMapper;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.util.DtoConverter;
import com.bjutzxq.server.util.JwtUtil;
import com.bjutzxq.server.util.OssUtil;
import com.bjutzxq.server.util.PasswordUtil;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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
        
        // 1. 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            log.warn("用户名已存在：{}", user.getUsername());
            throw new RuntimeException("用户名已存在");
        }
        
        // 2. 检查身份标识号是否已存在（学生为学号，教师为职工号）
        existingUser = userMapper.selectByEmployeeId(user.getEmployeeId().trim());
        if (existingUser != null) {
            log.warn("身份标识号已被使用：{}", user.getEmployeeId());
            throw new RuntimeException("身份标识号已被使用");
        }
        
        // 3. 检查邮箱是否已存在
        existingUser = userMapper.selectByEmail(user.getEmail());
        if (existingUser != null) {
            log.warn("邮箱已被使用：{}", user.getEmail());
            throw new RuntimeException("邮箱已被使用");
        }
        
        // 4. 密码加密（BCrypt）
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        // 5. 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(Constants.User.STATUS_NORMAL);
        }
        if (user.getSex() == null || user.getSex().isEmpty()) {
            user.setSex(Constants.User.SEX_UNKNOWN);
        }
        // 设置默认角色为学生
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        // 设置默认头像
        if (user.getAvatar() == null || user.getAvatar().trim().isEmpty()) {
            user.setAvatar("/logo.svg");
        }
        
        // 6. 插入用户
        userMapper.insert(user);
        
        log.info("用户注册成功，ID：{}", user.getId());
        return user;
    }
    
    /**
     * 用户登录
     * @param username 用户名或邮箱
     * @param password 密码
     * @return 登录响应 DTO（包含 token 和用户信息）
     */
    public LoginVO login(String username, String password) {
        log.info("用户登录，用户名/邮箱/学号：{}", username);
        
        // 查询用户（可能是用户名、邮箱或学号）
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            user = userMapper.selectByEmail(username);
        }
        if (user == null) {
            user = userMapper.selectByEmployeeId(username);
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
        
        // 清除用户的刷新计数（重新登录后重置）
        JwtUtil.clearRefreshCount(user.getId());
        
        log.info("用户登录成功：{}, ID: {}", username, user.getId());
        
        // 使用工具类构建 LoginResponse DTO
        return DtoConverter.buildLoginResponse(user, token);
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
     * 根据 ID 获取用户信息（公开接口）
     * @param userId 用户 ID
     * @return 用户信息
     */
    public User getUserById(Integer userId) {
        log.debug("获取用户信息，ID：{}", userId);
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 清除密码
            user.setPassword(null);
        }
        return user;
    }
    
    /**
     * 更新用户信息
     * @param userId 用户 ID
     * @param avatar 头像 URL
     * @param phone 手机号
     * @param sex 性别
     * @param bio 个人简介
     * @return 更新后的用户
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Integer userId, String avatar, String phone, String sex, String bio) {
        log.info("更新用户信息，ID：{}", userId);
        
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            log.warn("用户不存在，ID：{}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 只允许更新部分字段
        if (avatar != null) {
            existingUser.setAvatar(avatar);
        }
        if (phone != null) {
            existingUser.setPhone(phone);
        }
        if (sex != null) {
            existingUser.setSex(sex);
        }
        if (bio != null) {
            existingUser.setBio(bio);
        }
        
        userMapper.update(existingUser);
        
        log.info("用户信息更新成功，ID：{}", userId);
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
     * 管理员更新用户信息（可更新所有字段）
     * @param userId 用户 ID
     * @param username 用户名
     * @param employeeId 身份标识号
     * @param realName 真实姓名
     * @param email 邮箱
     * @param password 密码（可选，为空则不修改）
     * @param gender 性别
     * @param bio 简介
     * @param className 班级
     * @param role 角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserByAdmin(Integer userId, String username, String employeeId, 
                                   String realName, String email, String password,
                                   Integer gender, String bio, String className, Role role) {
        log.info("管理员开始更新用户信息，ID: {}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("更新失败：用户不存在，ID: {}", userId);
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户名是否已被其他用户使用
        if (username != null && !username.equals(user.getUsername())) {
            User existingUser = userMapper.selectByUsername(username);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("用户名已被使用");
            }
            user.setUsername(username);
        }
        
        // 检查身份标识号是否已被其他用户使用
        if (employeeId != null && !employeeId.equals(user.getEmployeeId())) {
            User existingUser = userMapper.selectByEmployeeId(employeeId);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("身份标识号已被使用");
            }
            user.setEmployeeId(employeeId);
        }
        
        // 检查邮箱是否已被其他用户使用
        if (email != null && !email.equals(user.getEmail())) {
            User existingUser = userMapper.selectByEmail(email);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(email);
        }
        
        // 更新其他字段
        if (realName != null) {
            user.setRealName(realName);
        }
        
        // 密码处理：如果提供了新密码，则加密后更新
        if (password != null && !password.trim().isEmpty()) {
            String encodedPassword = PasswordUtil.encode(password);
            user.setPassword(encodedPassword);
        }
        
        if (gender != null) {
            user.setSex(gender == 1 ? Constants.User.SEX_MALE : Constants.User.SEX_FEMALE);
        }
        
        if (bio != null) {
            user.setBio(bio);
        }
        
        if (className != null) {
            user.setClassName(className);
        }
        
        if (role != null) {
            user.setRole(role);
        }
        
        userMapper.update(user);
        
        log.info("用户信息更新成功，ID: {}", userId);
    }
    
    /**
     * 删除用户（级联删除相关数据）
     * @param userId 要删除的用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer userId) {
        log.info("开始删除用户，ID: {}", userId);
        
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
     * 清理用户上传的所有 OSS 文件
     * @param userId 用户 ID
     */
    private void cleanupUserFiles(Integer userId) {
        log.info("开始清理用户上传的 OSS 文件，用户 ID: {}", userId);
        
        try {
            // 查询该用户上传的所有文件
            List<ProjectFile> userFiles = projectFileMapper.selectByUploaderId(userId);
            
            if (userFiles == null || userFiles.isEmpty()) {
                log.info("该用户没有上传任何文件");
                return;
            }
            
            int deletedCount = 0;
            
            for (ProjectFile file : userFiles) {
                // 跳过目录
                if (file.getIsDir() != null && Constants.File.TYPE_DIRECTORY.equals(file.getIsDir())) {
                    continue;
                }
                
                // 只处理 OSS 文件（有 storageUrl 的文件）
                if (file.getStorageUrl() != null && file.getStorageUrl().startsWith("http")) {
                    try {
                        ossUtil.delete(file.getStorageUrl());
                        deletedCount++;
                        log.debug("已删除 OSS 文件: {}", file.getStorageUrl());
                    } catch (Exception e) {
                        log.error("删除 OSS 文件失败: {}, 错误: {}", file.getStorageUrl(), e.getMessage());
                    }
                }
            }
            
            log.info("OSS 文件清理完成，共删除: {} 个文件", deletedCount);
            
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
            if (oldAvatar != null && !oldAvatar.equals("/logo.svg") && !oldAvatar.trim().isEmpty()) {
                try {
                    ossUtil.delete(oldAvatar);
                    log.info("已删除旧头像: {}", oldAvatar);
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}, 错误: {}", oldAvatar, e.getMessage());
                    // 不抛出异常，继续上传新头像
                }
            }
            
            // 6. 上传新头像到 OSS (使用 avatars 目录)
            String avatarUrl = ossUtil.upload(file, "avatars");
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
    
    /**
     * 查询用户列表（支持按角色和关键词筛选）
     * @param role 角色（null-不限制角色）
     * @param keyword 搜索关键词（null或空-不搜索）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 用户列表
     */
    public java.util.List<User> queryUsers(Role role, String keyword, Integer pageNum, Integer pageSize) {
        log.info("查询用户列表，角色：{}, 关键词：{}, 页码：{}, 每页数量：{}", 
                 role != null ? role.getDescription() : "全部", 
                 keyword != null ? keyword : "无", 
                 pageNum, pageSize);
        
        // 参数验证
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 20;
        }
        
        PageHelper.startPage(pageNum, pageSize);
        
        java.util.List<User> users;
        
        // 根据参数选择不同的查询方式
        if (role != null && keyword != null && !keyword.trim().isEmpty()) {
            // 按角色 + 关键词搜索
            users = userMapper.searchByRoleAndKeyword(role, keyword.trim());
        } else if (role != null) {
            // 仅按角色查询
            users = userMapper.selectByRole(role);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 仅按关键词搜索
            users = userMapper.searchByKeyword(keyword.trim());
        } else {
            // 查询所有用户
            users = userMapper.selectAll();
        }
        
        // 清除密码
        users.forEach(user -> user.setPassword(null));
        
        log.info("查询用户列表成功，数量：{}", users.size());
        return users;
    }
}
