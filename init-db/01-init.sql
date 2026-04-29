-- ===========================================
-- BJUT-ZXQ 数据库初始化脚本
-- ===========================================
-- 此脚本会在 MySQL 容器首次启动时自动执行
-- 如果已有数据,不会重复创建

CREATE DATABASE IF NOT EXISTS bjut_zxq;

USE bjut_zxq;

-- ===========================================
-- 用户表
-- ===========================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `student_id` VARCHAR(20) COMMENT '学号',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `class_name` VARCHAR(100) COMMENT '班级',
  `email` VARCHAR(100) COMMENT '邮箱',
  `avatar` TEXT COMMENT '头像URL或Base64数据',
  `phone` VARCHAR(20) COMMENT '手机号',
  `sex` VARCHAR(10) DEFAULT 'U' COMMENT '性别: U-未知, M-男, F-女',
  `bio` TEXT COMMENT '个人简介',
  `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER, ADMIN',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_username (`username`),
  INDEX idx_email (`email`),
  INDEX idx_student_id (`student_id`)
) ENGINE=InnoDB COMMENT='用户表';

-- ===========================================
-- 项目表
-- ===========================================
CREATE TABLE IF NOT EXISTS `project` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
  `name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `description` TEXT COMMENT '项目描述',
  `owner_id` INT NOT NULL COMMENT '所有者ID',
  `project_type` VARCHAR(20) DEFAULT 'OTHER' COMMENT '项目类型: COURSE-课程设计, THESIS-毕业设计, COMPETITION-竞赛作品, PERSONAL-个人项目, OTHER-其他',
  `course_name` VARCHAR(100) COMMENT '课程名称（仅当 project_type=COURSE 时有效）',
  `thesis_type` VARCHAR(20) COMMENT '毕设类型: UNDERGRADUATE-本科, MASTER-硕士, DOCTOR-博士（仅当 project_type=THESIS 时有效）',
  `visibility` TINYINT DEFAULT 1 COMMENT '可见性: 0-私有, 1-公开',
  `star_count` INT DEFAULT 0 COMMENT '收藏数',
  `watch_count` INT DEFAULT 0 COMMENT '关注数',
  `file_count` INT DEFAULT 0 COMMENT '文件数',
  `download_count` INT DEFAULT 0 COMMENT '下载次数',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`owner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  INDEX idx_owner_id (`owner_id`),
  INDEX idx_visibility (`visibility`),
  INDEX idx_name (`name`),
  INDEX idx_project_type (`project_type`),
  INDEX idx_course_name (`course_name`)
) ENGINE=InnoDB COMMENT='项目表';

-- ===========================================
-- 标签表
-- ===========================================
CREATE TABLE IF NOT EXISTS `tag` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
  `category` VARCHAR(50) COMMENT '标签分组：技术栈、领域、其他',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_name (`name`),
  INDEX idx_category (`category`)
) ENGINE=InnoDB COMMENT='标签表';

-- ===========================================
-- 项目-标签关联表
-- ===========================================
CREATE TABLE IF NOT EXISTS `project_tag` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
  `project_id` INT NOT NULL COMMENT '项目ID',
  `tag_id` INT NOT NULL COMMENT '标签ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE,
  UNIQUE KEY uk_project_tag (`project_id`, `tag_id`),
  INDEX idx_project_id (`project_id`),
  INDEX idx_tag_id (`tag_id`)
) ENGINE=InnoDB COMMENT='项目-标签关联表';

-- ===========================================
-- 项目文件表
-- ===========================================
CREATE TABLE IF NOT EXISTS `project_file` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
  `project_id` INT NOT NULL COMMENT '项目ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_path` VARCHAR(500) COMMENT '文件路径',
  `file_size` BIGINT COMMENT '文件大小(字节)',
  `file_type` VARCHAR(50) COMMENT '文件类型',
  `storage_url` VARCHAR(500) COMMENT '存储URL(OSS地址)',
  `content` LONGBLOB COMMENT '文件内容(如果使用Git存储)',
  `is_dir` TINYINT DEFAULT 0 COMMENT '是否目录: 0-文件, 1-目录',
  `parent_id` INT COMMENT '父目录ID',
  `uploader_id` INT NOT NULL COMMENT '上传者ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`uploader_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  INDEX idx_project_id (`project_id`),
  INDEX idx_parent_id (`parent_id`),
  INDEX idx_uploader_id (`uploader_id`)
) ENGINE=InnoDB COMMENT='项目文件表';

-- ===========================================
-- 评论表
-- ===========================================
CREATE TABLE IF NOT EXISTS `comment` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `project_id` INT NOT NULL COMMENT '项目ID',
  `parent_id` INT DEFAULT NULL COMMENT '父评论ID(回复)',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT DEFAULT 0 COMMENT '回复数',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-删除, 1-正常',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`parent_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE,
  INDEX idx_user_id (`user_id`),
  INDEX idx_project_id (`project_id`),
  INDEX idx_parent_id (`parent_id`)
) ENGINE=InnoDB COMMENT='评论表';

-- ===========================================
-- 收藏表
-- ===========================================
CREATE TABLE IF NOT EXISTS `star` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `project_id` INT NOT NULL COMMENT '项目ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE,
  UNIQUE KEY uk_user_project (`user_id`, `project_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_project_id (`project_id`)
) ENGINE=InnoDB COMMENT='收藏表';

-- ===========================================
-- 关注表
-- ===========================================
CREATE TABLE IF NOT EXISTS `watch` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `project_id` INT NOT NULL COMMENT '项目ID',
  `notification_type` VARCHAR(20) DEFAULT 'ALL' COMMENT '通知类型: ALL, UPDATES, DISCUSSIONS',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE,
  UNIQUE KEY uk_user_project (`user_id`, `project_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_project_id (`project_id`)
) ENGINE=InnoDB COMMENT='关注表';

-- ===========================================
-- 通知表
-- ===========================================
CREATE TABLE IF NOT EXISTS `notification` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
  `user_id` INT NOT NULL COMMENT '接收者ID',
  `sender_id` INT COMMENT '发送者ID',
  `project_id` INT COMMENT '相关项目ID',
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE SET NULL,
  INDEX idx_user_id (`user_id`),
  INDEX idx_is_read (`is_read`)
) ENGINE=InnoDB COMMENT='通知表';

-- ===========================================
-- 下载日志表
-- ===========================================
CREATE TABLE IF NOT EXISTS `download_log` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `project_id` INT NOT NULL COMMENT '项目ID',
  `file_id` INT COMMENT '文件ID',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`project_id`) REFERENCES `project`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`file_id`) REFERENCES `project_file`(`id`) ON DELETE SET NULL,
  INDEX idx_user_id (`user_id`),
  INDEX idx_project_id (`project_id`),
  INDEX idx_created_at (`created_at`)
) ENGINE=InnoDB COMMENT='下载日志表';

-- ===========================================
-- 插入初始数据
-- ===========================================

-- 插入默认标签（技术栈）
INSERT INTO `tag` (`name`, `category`) VALUES
('Java', '技术栈'),
('Python', '技术栈'),
('C', '技术栈'),
('C++', '技术栈'),
('JavaScript', '技术栈'),
('TypeScript', '技术栈'),
('Go', '技术栈'),
('Rust', '技术栈'),
('Spring Boot', '技术栈'),
('Vue', '技术栈'),
('React', '技术栈'),
('Node.js', '技术栈'),
('Qt', '技术栈'),
('MySQL', '技术栈'),
('PostgreSQL', '技术栈'),
('MongoDB', '技术栈'),
('Redis', '技术栈'),
('Docker', '技术栈'),
('Kubernetes', '技术栈'),
('Linux', '技术栈'),
('Android', '技术栈'),
('iOS', '技术栈')
ON DUPLICATE KEY UPDATE `name`=`name`;

-- 插入默认标签（领域）
INSERT INTO `tag` (`name`, `category`) VALUES
('人工智能', '领域'),
('机器学习', '领域'),
('深度学习', '领域'),
('大数据', '领域'),
('云计算', '领域'),
('物联网', '领域'),
('Web开发', '领域'),
('移动开发', '领域'),
('桌面应用', '领域'),
('游戏开发', '领域'),
('嵌入式系统', '领域')
ON DUPLICATE KEY UPDATE `name`=`name`;

-- 插入默认标签（其他）
INSERT INTO `tag` (`name`, `category`) VALUES
('前后端分离', '其他'),
('微服务', '其他'),
('单体应用', '其他')
ON DUPLICATE KEY UPDATE `name`=`name`;

-- ===========================================
-- 完成提示
-- ===========================================
SELECT '数据库初始化完成!' AS message;
