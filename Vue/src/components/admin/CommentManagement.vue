<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminComments, searchAdminComments, adminDeleteComment } from '@/api/comment'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'

const comments = ref([])
const isLoading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref(null)

// 分页
const PAGE_SIZE = 20
const currentPage = ref(1)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

const loadComments = async () => {
  isLoading.value = true
  try {
    let res
    if (searchKeyword.value.trim()) {
      res = await searchAdminComments(searchKeyword.value.trim(), {
        pageNum: currentPage.value,
        pageSize: PAGE_SIZE
      })
    } else {
      res = await getAdminComments({
        pageNum: currentPage.value,
        pageSize: PAGE_SIZE,
        status: statusFilter.value
      })
    }
    if (res.code === 200 && res.data) {
      comments.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    logError('加载评论列表失败:', error)
    toast.error('加载评论列表失败')
  } finally {
    isLoading.value = false
  }
}

const onSearch = () => {
  currentPage.value = 1
  loadComments()
}

const onStatusFilter = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  loadComments()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadComments()
}

const handleDelete = async (comment) => {
  if (!confirm(`确定要永久删除该评论吗？\n\n评论内容：${comment.content?.substring(0, 100)}`)) return
  try {
    await adminDeleteComment(comment.id)
    toast.success('评论已删除')
    loadComments()
  } catch (error) {
    logError('删除评论失败:', error)
    toast.error('删除评论失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadComments()
})
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">💬 评论管理</h2>
      <div class="header-actions">
        <select v-model="statusFilter" @change="onStatusFilter" class="filter-select">
          <option :value="null">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">已删除</option>
        </select>
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索评论内容、用户名、项目名..."
            class="search-input"
            @keyup.enter="onSearch"
          />
          <button @click="onSearch" class="search-btn">搜索</button>
        </div>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="comments.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>{{ searchKeyword ? '未找到匹配的评论' : '暂无评论数据' }}</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th style="width:100px">用户</th>
            <th style="width:140px">项目</th>
            <th>内容</th>
            <th style="width:70px">点赞</th>
            <th style="width:70px">状态</th>
            <th style="width:160px">创建时间</th>
            <th style="width:70px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="comment in comments" :key="comment.id" :class="{ 'deleted-row': comment.status === 0 }">
            <td>{{ comment.id }}</td>
            <td>
              <span class="user-name" :title="`ID: ${comment.userId}`">{{ comment.username || comment.userId }}</span>
            </td>
            <td>
              <span class="project-name" :title="`ID: ${comment.projectId}`">{{ comment.projectName || comment.projectId }}</span>
            </td>
            <td>
              <span class="content-cell">{{ comment.content }}</span>
            </td>
            <td>{{ comment.likeCount || 0 }}</td>
            <td>
              <span :class="['status-badge', comment.status === 1 ? 'status-normal' : 'status-deleted']">
                {{ comment.status === 1 ? '正常' : '已删除' }}
              </span>
            </td>
            <td>{{ formatDate(comment.createdAt) }}</td>
            <td>
              <button @click="handleDelete(comment)" class="action-btn delete-btn" title="永久删除">🗑️</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="total > PAGE_SIZE" class="pagination">
      <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1" class="page-btn">上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages || totalPages === 0" class="page-btn">下一页</button>
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #064e3b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}

.search-box {
  display: flex;
  gap: 8px;
}

.search-input {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  width: 280px;
}

.search-input:focus {
  outline: none;
  border-color: #10b981;
}

.search-btn {
  padding: 8px 16px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.search-btn:hover {
  background-color: #059669;
}

.table-wrapper {
  overflow-x: auto;
  flex: 1;
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
  white-space: nowrap;
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

.deleted-row {
  opacity: 0.5;
}

.user-name, .project-name {
  font-weight: 500;
  color: #064e3b;
}

.content-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 300px;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.status-normal {
  background-color: #d1fae5;
  color: #065f46;
}

.status-deleted {
  background-color: #fee2e2;
  color: #991b1b;
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

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 16px 0;
}

.page-btn {
  padding: 8px 16px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background-color: #059669;
}

.page-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666666;
}
</style>
