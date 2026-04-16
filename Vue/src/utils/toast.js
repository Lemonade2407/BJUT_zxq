import { createApp, ref } from 'vue'
import Toast from '@/components/Toast.vue'

// 创建 Toast 实例
let toastInstance = null
let toastRef = null

// 初始化 Toast（在应用启动时调用）
export function initToast(app) {
  const container = document.createElement('div')
  document.body.appendChild(container)
  
  const instance = createApp(Toast)
  toastInstance = instance.mount(container)
  toastRef = toastInstance
  
  // 将 toast 方法挂载到全局属性
  app.config.globalProperties.$toast = {
    success: (message, duration) => toastInstance.show(message, 'success', duration),
    error: (message, duration) => toastInstance.show(message, 'error', duration),
    warning: (message, duration) => toastInstance.show(message, 'warning', duration),
    info: (message, duration) => toastInstance.show(message, 'info', duration)
  }
}

// 便捷方法（用于 Composition API）
export const toast = {
  success: (message, duration) => {
    if (toastRef) {
      toastRef.show(message, 'success', duration)
    }
  },
  error: (message, duration) => {
    if (toastRef) {
      toastRef.show(message, 'error', duration)
    }
  },
  warning: (message, duration) => {
    if (toastRef) {
      toastRef.show(message, 'warning', duration)
    }
  },
  info: (message, duration) => {
    if (toastRef) {
      toastRef.show(message, 'info', duration)
    }
  }
}
