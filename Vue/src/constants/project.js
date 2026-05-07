/**
 * 项目类型常量
 */
export const PROJECT_TYPES = {
  COURSE: 'COURSE',           // 课程设计
  THESIS: 'THESIS',           // 毕业设计
  COMPETITION: 'COMPETITION', // 竞赛作品
  PERSONAL: 'PERSONAL',       // 个人项目
  OTHER: 'OTHER'              // 其他
}

/**
 * 项目类型显示名称映射
 */
export const PROJECT_TYPE_LABELS = {
  [PROJECT_TYPES.COURSE]: '课程设计',
  [PROJECT_TYPES.THESIS]: '毕业设计',
  [PROJECT_TYPES.COMPETITION]: '竞赛作品',
  [PROJECT_TYPES.PERSONAL]: '个人项目',
  [PROJECT_TYPES.OTHER]: '其他'
}

/**
 * 获取项目类型的显示文本
 * @param {string} type - 项目类型代码
 * @returns {string} 显示文本
 */
export function getProjectTypeText(type) {
  return PROJECT_TYPE_LABELS[type] || '未知'
}
