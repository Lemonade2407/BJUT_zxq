<script setup>
defineProps({
  visible: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  danger: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <div v-if="visible" class="confirm-overlay" @click.self="emit('cancel')">
    <div class="confirm-card">
      <div class="confirm-header">
        <h3 class="confirm-title">{{ title }}</h3>
      </div>
      <div class="confirm-body">
        <p class="confirm-message">{{ message }}</p>
      </div>
      <div class="confirm-footer">
        <button @click="emit('cancel')" class="btn btn-cancel">{{ cancelText }}</button>
        <button @click="emit('confirm')" :class="['btn', danger ? 'btn-danger' : 'btn-primary']">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5); display: flex;
  align-items: center; justify-content: center; z-index: 1100;
}
.confirm-card {
  background: #fff; border-radius: 12px; width: 90%; max-width: 400px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2); overflow: hidden;
}
.confirm-header { padding: 20px 24px; border-bottom: 1px solid #f0f0f0; }
.confirm-title { margin: 0; font-size: 18px; font-weight: 600; color: #064e3b; }
.confirm-body { padding: 24px; }
.confirm-message { margin: 0; font-size: 15px; color: #666; line-height: 1.6; }
.confirm-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #f0f0f0; background: #fafafa; }
.btn { padding: 8px 24px; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-cancel:hover { background: #e0e0e0; }
.btn-primary { background: #10b981; color: #fff; }
.btn-primary:hover { background: #059669; }
.btn-danger { background: #ef4444; color: #fff; }
.btn-danger:hover { background: #dc2626; }
</style>
