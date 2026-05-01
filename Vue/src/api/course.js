import request from '@/utils/request'

/**
 * 获取所有启用的课程
 */
export function getActiveCourses() {
  return request({
    url: '/course/active',
    method: 'get'
  })
}

/**
 * 获取所有课程（管理员）
 */
export function getAllCourses() {
  return request({
    url: '/course/all',
    method: 'get'
  })
}

/**
 * 搜索课程
 */
export function searchCourses(keyword) {
  return request({
    url: '/course/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 创建课程（管理员）
 */
export function createCourse(courseName) {
  return request({
    url: '/course',
    method: 'post',
    data: { courseName }
  })
}

/**
 * 更新课程（管理员）
 */
export function updateCourse(id, courseName, isActive) {
  return request({
    url: `/course/${id}`,
    method: 'put',
    data: { courseName, isActive }
  })
}

/**
 * 删除课程（管理员）
 */
export function deleteCourse(id) {
  return request({
    url: `/course/${id}`,
    method: 'delete'
  })
}
