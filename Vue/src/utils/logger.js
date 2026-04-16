/**
 * 统一的日志工具
 * 生产环境自动禁用 console.log
 */

// TODO: 集成错误监控服务（如 Sentry），收集生产环境错误
// TODO: 添加日志上报功能，便于问题排查

// 是否启用控制台日志（开发环境启用，生产环境禁用）
const ENABLE_CONSOLE = import.meta.env.VITE_ENABLE_CONSOLE !== 'false'

/**
 * 开发环境日志
 * @param {...any} args - 日志参数
 */
export function log(...args) {
  if (ENABLE_CONSOLE) {
    console.log(...args)
  }
}

/**
 * 开发环境警告
 * @param {...any} args - 警告参数
 */
export function warn(...args) {
  if (ENABLE_CONSOLE) {
    console.warn(...args)
  }
}

/**
 * 开发环境错误（始终显示）
 * @param {...any} args - 错误参数
 */
export function error(...args) {
  console.error(...args)
}

/**
 * 开发环境调试信息
 * @param {...any} args - 调试参数
 */
export function debug(...args) {
  if (ENABLE_CONSOLE && import.meta.env.DEV) {
    console.debug(...args)
  }
}

/**
 * 表格形式输出（仅开发环境）
 * @param {Array|Object} data - 表格数据
 */
export function table(data) {
  if (ENABLE_CONSOLE && import.meta.env.DEV) {
    console.table(data)
  }
}

/**
 * 计时器开始（仅开发环境）
 * @param {string} label - 计时器标签
 */
export function time(label) {
  if (ENABLE_CONSOLE && import.meta.env.DEV) {
    console.time(label)
  }
}

/**
 * 计时器结束（仅开发环境）
 * @param {string} label - 计时器标签
 */
export function timeEnd(label) {
  if (ENABLE_CONSOLE && import.meta.env.DEV) {
    console.timeEnd(label)
  }
}
