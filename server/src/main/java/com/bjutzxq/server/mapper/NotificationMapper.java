package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 Mapper 接口
 */
@Mapper
public interface NotificationMapper {
    
    /**
     * 根据 ID 查询通知
     * @param id 通知 ID
     * @return 通知对象
     */
    Notification selectById(@Param("id") Integer id);
    
    /**
     * 查询用户的通知列表（分页）
     * @param userId 用户 ID
     * @param isRead 是否已读（null-全部，0-未读，1-已读）
     * @return 通知列表
     */
    List<Notification> selectByUserId(
        @Param("userId") Integer userId,
        @Param("isRead") Integer isRead
    );
    
    /**
     * 查询用户的未读通知数量
     * @param userId 用户 ID
     * @return 未读通知数量
     */
    int countUnreadByUserId(@Param("userId") Integer userId);
    
    /**
     * 插入通知
     * @param notification 通知对象
     * @return 影响行数
     */
    int insert(Notification notification);
    
    /**
     * 批量插入通知
     * @param notifications 通知列表
     * @return 影响行数
     */
    int batchInsert(@Param("notifications") List<Notification> notifications);
    
    /**
     * 更新通知
     * @param notification 通知对象
     * @return 影响行数
     */
    int update(Notification notification);
    
    /**
     * 将用户的所有未读通知标记为已读
     * @param userId 用户 ID
     * @return 影响行数
     */
    int markAllAsRead(@Param("userId") Integer userId);
    
    /**
     * 将指定通知标记为已读
     * @param notificationId 通知 ID
     * @return 影响行数
     */
    int markAsRead(@Param("notificationId") Integer notificationId);
    
    /**
     * 删除通知
     * @param id 通知 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 批量删除通知
     * @param notificationIds 通知 ID 列表
     * @return 影响行数
     */
    int batchDelete(@Param("notificationIds") List<Integer> notificationIds);
}
