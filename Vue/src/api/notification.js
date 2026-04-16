import request from '@/utils/request'

/**
 * 通知相关 API
 */

// 获取我的通知列表（分页）
export function getNotifications(params = {}) {
  return request({
    url: '/notifications',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      isRead: params.isRead // null-全部，0-未读，1-已读
    }
  })
}

// 获取未读通知数量
export function getUnreadCount() {
  return request({
    url: '/notifications/unread/count',
    method: 'get'
  })
}

// 标记单个通知为已读
export function markAsRead(notificationId) {
  return request({
    url: `/notifications/${notificationId}/read`,
    method: 'put'
  })
}

// 将所有通知标记为已读
export function markAllAsRead() {
  return request({
    url: '/notifications/read/all',
    method: 'put'
  })
}

// 删除单个通知
export function deleteNotification(notificationId) {
  return request({
    url: `/notifications/${notificationId}`,
    method: 'delete'
  })
}

// 批量删除通知
export function batchDeleteNotifications(ids) {
  return request({
    url: '/notifications/batch-delete',
    method: 'post',
    data: { ids }
  })
}
