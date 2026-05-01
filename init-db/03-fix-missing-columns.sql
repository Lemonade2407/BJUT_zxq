-- ===========================================
-- 紧急修复脚本：添加缺失的数据库字段
-- 执行时间：2026-04-29
-- ===========================================

USE bjut_zxq;

-- 1. 修复 user 表：添加 employee_id 字段（如果不存在）
SET @dbname = DATABASE();
SET @tablename = 'user';
SET @columnname = 'employee_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) COMMENT ''身份标识号（学生为学号，教师为职工号）'' AFTER password')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 如果 student_id 存在，将其数据复制到 employee_id
UPDATE `user` SET `employee_id` = `student_id` WHERE `student_id` IS NOT NULL AND `employee_id` IS NULL;

-- 删除旧的 student_id 字段（如果存在）
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = 'student_id')
  ) > 0,
  CONCAT('ALTER TABLE ', @tablename, ' DROP COLUMN student_id'),
  'SELECT 1'
));
PREPARE dropIfExists FROM @preparedStatement;
EXECUTE dropIfExists;
DEALLOCATE PREPARE dropIfExists;

-- 删除旧索引（如果存在）
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_student_id')
  ) > 0,
  'ALTER TABLE user DROP INDEX idx_student_id',
  'SELECT 1'
));
PREPARE dropIndexIfExists FROM @preparedStatement;
EXECUTE dropIndexIfExists;
DEALLOCATE PREPARE dropIndexIfExists;

-- 添加新索引（如果不存在）
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_employee_id')
  ) > 0,
  'SELECT 1',
  'ALTER TABLE user ADD INDEX idx_employee_id (employee_id)'
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

-- 2. 修复 project 表：添加缺失的字段
SET @tablename = 'project';

-- 添加 project_type 字段
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = 'project_type')
  ) > 0,
  'SELECT 1',
  "ALTER TABLE project ADD COLUMN project_type VARCHAR(20) DEFAULT 'OTHER' COMMENT '项目类型: COURSE-课程设计, THESIS-毕业设计, COMPETITION-竞赛作品, PERSONAL-个人项目, OTHER-其他' AFTER owner_id"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 course_name 字段
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = 'course_name')
  ) > 0,
  'SELECT 1',
  "ALTER TABLE project ADD COLUMN course_name VARCHAR(100) COMMENT '课程名称（仅当 project_type=COURSE 时有效）' AFTER project_type"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 thesis_type 字段
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = 'thesis_type')
  ) > 0,
  'SELECT 1',
  "ALTER TABLE project ADD COLUMN thesis_type VARCHAR(20) COMMENT '毕设类型: UNDERGRADUATE-本科, MASTER-硕士, DOCTOR-博士（仅当 project_type=THESIS 时有效）' AFTER course_name"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 project_type 索引
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_project_type')
  ) > 0,
  'SELECT 1',
  'ALTER TABLE project ADD INDEX idx_project_type (project_type)'
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

-- 添加 course_name 索引
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_course_name')
  ) > 0,
  'SELECT 1',
  'ALTER TABLE project ADD INDEX idx_course_name (course_name)'
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

-- 完成提示
SELECT '数据库修复完成！所有缺失字段已添加' AS message;
