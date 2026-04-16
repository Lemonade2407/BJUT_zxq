<script setup>
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('info') // success, error, warning, info
let timer = null

// 显示提示
const show = (msg, toastType = 'info', duration = 3000) => {
  // 清除之前的定时器
  if (timer) {
    clearTimeout(timer)
  }
  
  message.value = msg
  type.value = toastType
  visible.value = true
  
  // 自动隐藏
  timer = setTimeout(() => {
    visible.value = false
  }, duration)
}

// 手动隐藏
const hide = () => {
  visible.value = false
  if (timer) {
    clearTimeout(timer)
  }
}

// 暴露方法给父组件
defineExpose({ show, hide })
</script>

<template>
  <Transition name="toast">
    <div v-if="visible" :class="['toast-container', type]" @click="hide">
      <div class="toast-content">
        <span class="toast-icon">
          {{ type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️' }}
        </span>
        <span class="toast-message">{{ message }}</span>
        <button class="toast-close" @click.stop="hide">×</button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
  min-width: 300px;
  max-width: 500px;
  animation: slideIn 0.3s ease-out;
  cursor: pointer;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-left: 4px solid;
}

/* 不同类型的样式 */
.toast-container.success .toast-content {
  border-left-color: #52c41a;
  background-color: #f6ffed;
}

.toast-container.error .toast-content {
  border-left-color: #ff4d4f;
  background-color: #fff2f0;
}

.toast-container.warning .toast-content {
  border-left-color: #faad14;
  background-color: #fffbe6;
}

.toast-container.info .toast-content {
  border-left-color: #1890ff;
  background-color: #e6f7ff;
}

.toast-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.toast-message {
  flex: 1;
  font-size: 14px;
  color: #333333;
  line-height: 1.5;
  word-break: break-word;
}

.toast-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999999;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
  flex-shrink: 0;
}

.toast-close:hover {
  color: #333333;
}

/* 动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .toast-container {
    top: 70px;
    right: 12px;
    left: 12px;
    min-width: auto;
    max-width: none;
  }
}
</style>
