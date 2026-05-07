<script setup>
import { ref, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const comments = ref([])
const isLoading = ref(false)

const loadComments = async () => {
  isLoading.value = true
  try {
    // TODO: 需要后端提供评论管理接口
    comments.value = []
    toast.info('评论管理功能开发中')
  } catch (error) {
    logError('加载评论列表失败:', error)
    toast.error('加载评论列表失败')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadComments()
})
</script>

<template>
  <div class="management-container">
    <h2 class="section-title">💬 评论管理</h2>
    
    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="comments.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>暂无评论数据</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>项目</th>
            <th>内容</th>
            <th>点赞数</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="comment in comments" :key="comment.id">
            <td>{{ comment.id }}</td>
            <td>{{ comment.userId }}</td>
            <td>{{ comment.projectId }}</td>
            <td>{{ comment.content }}</td>
            <td>{{ comment.likeCount || 0 }}</td>
            <td>{{ comment.status === 1 ? '正常' : '已删除' }}</td>
            <td>{{ new Date(comment.createdAt).toLocaleDateString('zh-CN') }}</td>
            <td>
              <button class="action-btn delete-btn">🗑️</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.management-container {
  width: 100%;
  max-width: 1400px;
  min-height: calc(100vh - 144px);
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #064e3b;
  margin: 0 0 24px 0;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background-color: #f5f7fa;
}

.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #333333;
  border-bottom: 2px solid #e0e0e0;
}

.data-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: #666666;
  border-bottom: 1px solid #f0f0f0;
}

.data-table tbody tr:hover {
  background-color: #f9fafb;
}

.action-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: transparent;
}

.action-btn:hover {
  transform: scale(1.1);
}

.delete-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999999;
}

.loading-icon,
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
