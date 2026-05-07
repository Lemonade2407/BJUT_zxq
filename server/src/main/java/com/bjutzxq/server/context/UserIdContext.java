package com.bjutzxq.server.context;

/**
 * 用户ID上下文持有者（ThreadLocal）
 * 用于在请求处理过程中存储和获取当前用户ID
 * 由 UserContextInterceptor 设置，在 Controller 中通过 getCurrentUserId() 获取
 */
public class UserIdContext {
    
    private static final ThreadLocal<Integer> USER_ID_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前用户ID
     * @param userId 用户ID
     */
    public static void setCurrentUserId(Integer userId) {
        USER_ID_HOLDER.set(userId);
    }
    
    /**
     * 获取当前用户ID
     * @return 用户ID，如果未设置则返回 null
     */
    public static Integer getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }
    
    /**
     * 清除当前用户ID（防止内存泄漏）
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
