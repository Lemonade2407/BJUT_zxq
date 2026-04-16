package com.bjutzxq.common;

/**
 * 常量类
 */
public class Constants {
    
    /**
     * 用户相关常量
     */
    public static class User {
        /**
         * 用户状态：正常
         */
        public static final Integer STATUS_NORMAL = 1;
        
        /**
         * 用户状态：禁用
         */
        public static final Integer STATUS_DISABLED = 0;
        
        /**
         * 性别：男
         */
        public static final String SEX_MALE = "M";
        
        /**
         * 性别：女
         */
        public static final String SEX_FEMALE = "F";
        
        /**
         * 性别：未知
         */
        public static final String SEX_UNKNOWN = "U";
    }
    
    /**
     * 项目相关常量
     */
    public static class Project {
        /**
         * 可见性：公开
         */
        public static final Integer VISIBILITY_PUBLIC = 1;
        
        /**
         * 可见性：私有
         */
        public static final Integer VISIBILITY_PRIVATE = 0;
    }
    
    /**
     * 文件相关常量
     */
    public static class File {
        /**
         * 类型：文件
         */
        public static final Integer TYPE_FILE = 0;
        
        /**
         * 类型：目录
         */
        public static final Integer TYPE_DIRECTORY = 1;
    }
    
    /**
     * 评论相关常量
     */
    public static class Comment {
        /**
         * 状态：显示
         */
        public static final Integer STATUS_SHOW = 1;
        
        /**
         * 状态：删除
         */
        public static final Integer STATUS_DELETED = 0;
    }
    
    /**
     * 通知相关常量
     */
    public static class Notification {
        /**
         * 类型：点赞
         */
        public static final Integer TYPE_STAR = 1;
        
        /**
         * 类型：评论
         */
        public static final Integer TYPE_COMMENT = 2;
        
        /**
         * 类型：关注
         */
        public static final Integer TYPE_WATCH = 3;
        
        /**
         * 是否已读：未读
         */
        public static final Integer READ_UNREAD = 0;
        
        /**
         * 是否已读：已读
         */
        public static final Integer READ_READ = 1;
    }
    
    /**
     * JWT 相关常量
     */
    public static class JWT {
        /**
         * Token 前缀
         */
        public static final String TOKEN_PREFIX = "Bearer ";
        
        /**
         * Token 请求头
         */
        public static final String TOKEN_HEADER = "Authorization";
        
        /**
         * Token 过期时间（2 小时）
         */
        public static final long TOKEN_EXPIRE_TIME = 7200 * 1000;
        
        /**
         * Token 密钥（至少 32 字符/256 位，用于 HMAC-SHA256）
         */
        public static final String TOKEN_SECRET = "bjut_zxq_2026_jwt_secret_key_for_hmac_sha256";
    }
}
