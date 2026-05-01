-- ===========================================
-- 数据库修复脚本
-- 说明：如果字段已存在会报错，可以忽略错误继续执行
-- ===========================================

USE bjut_zxq;

-- 1. user 表添加 employee_id 字段
ALTER TABLE `user` ADD COLUMN `employee_id` VARCHAR(20) COMMENT '身份标识号' AFTER `password`;

-- 迁移数据
UPDATE `user` SET `employee_id` = `student_id` WHERE `student_id` IS NOT NULL;

-- 删除旧字段
ALTER TABLE `user` DROP COLUMN `student_id`;

-- 更新索引
ALTER TABLE `user` DROP INDEX `idx_student_id`;
ALTER TABLE `user` ADD INDEX `idx_employee_id` (`employee_id`);

-- 2. project 表添加缺失字段
ALTER TABLE `project` ADD COLUMN `project_type` VARCHAR(20) DEFAULT 'OTHER' COMMENT '项目类型' AFTER `owner_id`;
ALTER TABLE `project` ADD COLUMN `course_name` VARCHAR(100) COMMENT '课程名称' AFTER `project_type`;
ALTER TABLE `project` ADD COLUMN `thesis_type` VARCHAR(20) COMMENT '毕设类型' AFTER `course_name`;

-- 添加索引
ALTER TABLE `project` ADD INDEX `idx_project_type` (`project_type`);
ALTER TABLE `project` ADD INDEX `idx_course_name` (`course_name`);

SELECT '修复完成' AS message;
