-- ===========================================
-- 数据库迁移脚本：添加教师角色支持
-- 执行时间：2026-04-29
-- ===========================================

USE bjut_zxq;

-- 1. 重命名字段：student_id -> employee_id
ALTER TABLE `user` 
CHANGE COLUMN `student_id` `employee_id` VARCHAR(20) COMMENT '身份标识号（学生为学号，教师为职工号）';

-- 2. 更新注释
ALTER TABLE `user` 
MODIFY COLUMN `class_name` VARCHAR(100) COMMENT '班级（仅学生需要填写）',
MODIFY COLUMN `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER-学生, TEACHER-教师, ADMIN-管理员';

-- 3. 更新索引
ALTER TABLE `user` 
DROP INDEX idx_student_id,
ADD INDEX idx_employee_id (`employee_id`);

-- 完成提示
SELECT '数据库迁移完成！已支持教师角色注册' AS message;
