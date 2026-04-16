package com.bjutzxq.server.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 密码强度工具类
 */
@Slf4j
public class PasswordStrengthUtil {
    
    /**
     * 密码强度等级
     */
    public enum StrengthLevel {
        WEAK("弱", 1),
        MEDIUM("中", 2),
        STRONG("强", 3),
        VERY_STRONG("很强", 4);
        
        private final String description;
        private final int level;
        
        StrengthLevel(String description, int level) {
            this.description = description;
            this.level = level;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * 评估密码强度
     * @param password 密码
     * @return 密码强度信息
     */
    public static PasswordStrengthInfo evaluatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("密码不能为 null");
        }
        if (password.isEmpty()) {
            return new PasswordStrengthInfo(StrengthLevel.WEAK, 0, "密码不能为空");
        }
        
        int score = 0;
        StringBuilder suggestions = new StringBuilder();
        
        // 1. 长度评分（最多 30 分）
        int length = password.length();
        if (length >= 6) {
            score += 10;
        } else {
            // 长度不足6位，直接判定为弱密码
            return new PasswordStrengthInfo(StrengthLevel.WEAK, score, "密码长度至少需要 6 位");
        }
        if (length >= 8) {
            score += 10;
        }
        if (length >= 12) {
            score += 10;
        } else {
            suggestions.append("建议密码长度至少 12 位；");
        }
        
        // 2. 包含小写字母（最多 10 分）
        if (password.matches(".*[a-z].*")) {
            score += 10;
        } else {
            suggestions.append("建议包含小写字母；");
        }
        
        // 3. 包含大写字母（最多 15 分）
        if (password.matches(".*[A-Z].*")) {
            score += 15;
        } else {
            suggestions.append("建议包含大写字母；");
        }
        
        // 4. 包含数字（最多 15 分）
        if (password.matches(".*\\d.*")) {
            score += 15;
        } else {
            suggestions.append("建议包含数字；");
        }
        
        // 5. 包含特殊字符（最多 20 分）
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            score += 20;
        } else {
            suggestions.append("建议包含特殊字符（如 !@#$%）；");
        }
        
        // 6. 字符多样性（最多 10 分）
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars >= 8) {
            score += 10;
        } else if (uniqueChars >= 5) {
            score += 5;
            suggestions.append("建议使用更多不同字符；");
        } else {
            suggestions.append("建议增加字符多样性；");
        }
        
        // 确定强度等级
        StrengthLevel level;
        if (score < 40) {
            level = StrengthLevel.WEAK;
        } else if (score < 60) {
            level = StrengthLevel.MEDIUM;
        } else if (score < 80) {
            level = StrengthLevel.STRONG;
        } else {
            level = StrengthLevel.VERY_STRONG;
        }
        
        // 移除末尾的分号
        String suggestionStr = suggestions.toString();
        if (suggestionStr.endsWith("；")) {
            suggestionStr = suggestionStr.substring(0, suggestionStr.length() - 1);
        }
        
        log.debug("密码强度评估：分数={}, 等级={}", score, level.getDescription());
        
        return new PasswordStrengthInfo(level, score, suggestionStr.isEmpty() ? "密码强度很好" : suggestionStr);
    }
    
    /**
     * 检查密码是否满足最低要求
     * @param password 密码
     * @return 是否满足要求
     */
    public static boolean meetsMinimumRequirement(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // 必须包含字母和数字
        return password.matches("^(?=.*[a-zA-Z])(?=.*\\d).+$");
    }
    
    /**
     * 密码强度信息类
     */
    public static class PasswordStrengthInfo {
        private StrengthLevel level;
        private int score;
        private String suggestion;
        
        public PasswordStrengthInfo(StrengthLevel level, int score, String suggestion) {
            this.level = level;
            this.score = score;
            this.suggestion = suggestion;
        }
        
        public StrengthLevel getLevel() {
            return level;
        }
        
        public int getScore() {
            return score;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        public String getLevelDescription() {
            return level.getDescription();
        }
    }
}
