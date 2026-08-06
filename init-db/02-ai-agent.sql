-- ===========================================
-- AI 选题与组队助手：会话与消息表
-- 说明：在 01-init.sql 之后执行，不破坏现有表结构
-- ===========================================

CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  INDEX idx_conv_user (`user_id`),
  INDEX idx_conv_updated (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 会话表';

CREATE TABLE IF NOT EXISTS `ai_message` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
  `conversation_id` INT NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色: system/user/assistant/tool',
  `content` MEDIUMTEXT COMMENT '文本内容',
  `tool_calls` JSON COMMENT 'assistant 的工具调用(数组)',
  `tool_call_id` VARCHAR(100) COMMENT 'tool 消息对应的调用ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation`(`id`) ON DELETE CASCADE,
  INDEX idx_msg_conv (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 会话消息表';

SELECT 'AI 助手表初始化完成!' AS message;
