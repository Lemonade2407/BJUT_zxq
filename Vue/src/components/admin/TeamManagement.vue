<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAdminTeams, adminDeleteTeam, updateTeamStatus } from '@/api/team'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'

const teams = ref([])
const isLoading = ref(false)
const tagFilter = ref('')
const statusFilter = ref(null)
const currentPage = ref(1)
const PAGE_SIZE = 20
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

const TAG_MAP = { COMPETITION: '竞赛', PROJECT: '项目', COURSE: '课设' }
const STATUS_MAP = { 0: '已结束', 1: '招募中', 2: '已满员' }

const loadTeams = async () => {
  isLoading.value = true
  try {
    const res = await getAdminTeams({
      pageNum: currentPage.value,
      pageSize: PAGE_SIZE,
      tag: tagFilter.value || undefined,
      status: statusFilter.value
    })
    if (res.code === 200 && res.data) {
      teams.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    logError('加载组队列表失败:', e)
    toast.error('加载组队列表失败')
  } finally {
    isLoading.value = false
  }
}

const onFilterChange = () => {
  currentPage.value = 1
  loadTeams()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadTeams()
}

const handleDelete = async (team) => {
  if (!confirm(`确定删除组队"${team.title}"吗？此操作不可恢复。`)) return
  try {
    await adminDeleteTeam(team.id)
    toast.success('组队已删除')
    loadTeams()
  } catch (e) {
    toast.error(e.message || '删除失败')
  }
}

const handleStatusChange = async (team, newStatus) => {
  try {
    await updateTeamStatus(team.id, newStatus)
    toast.success('状态已更新')
    loadTeams()
  } catch (e) {
    toast.error(e.message || '状态更新失败')
  }
}

const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : '-'

onMounted(() => loadTeams())
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">👥 组队管理</h2>
      <div class="header-actions">
        <select v-model="tagFilter" @change="onFilterChange" class="filter-select">
          <option value="">全部类型</option>
          <option value="COMPETITION">竞赛</option>
          <option value="PROJECT">项目</option>
          <option value="COURSE">课设</option>
        </select>
        <select v-model="statusFilter" @change="onFilterChange" class="filter-select">
          <option :value="null">全部状态</option>
          <option :value="1">招募中</option>
          <option :value="2">已满员</option>
          <option :value="0">已结束</option>
        </select>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="teams.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>暂无组队数据</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>创建者</th>
            <th>标题</th>
            <th>类型</th>
            <th>课程</th>
            <th>成员</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="team in teams" :key="team.id">
            <td>{{ team.id }}</td>
            <td>{{ team.creatorUsername || team.userId }}</td>
            <td>{{ team.title }}</td>
            <td>
              <span :class="['tag-badge', `tag-${team.tag?.toLowerCase()}`]">{{ TAG_MAP[team.tag] || team.tag }}</span>
            </td>
            <td>{{ team.courseName || '-' }}</td>
            <td>{{ team.currentMembers }}/{{ team.neededMembers }}</td>
            <td>
              <select :value="team.status" @change="handleStatusChange(team, Number(($event.target).value))" class="status-select">
                <option :value="1">招募中</option>
                <option :value="2">已满员</option>
                <option :value="0">已结束</option>
              </select>
            </td>
            <td>{{ formatDate(team.createdAt) }}</td>
            <td>
              <button @click="handleDelete(team)" class="action-btn delete-btn" title="删除">🗑️</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > PAGE_SIZE" class="pagination">
      <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1" class="page-btn">上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages" class="page-btn">下一页</button>
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
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
}

.table-wrapper { overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table thead { background-color: #f5f7fa; }
.data-table th {
  padding: 12px 16px; text-align: left; font-size: 14px;
  font-weight: 600; color: #333; border-bottom: 2px solid #e0e0e0; white-space: nowrap;
}
.data-table td {
  padding: 12px 16px; font-size: 14px; color: #666; border-bottom: 1px solid #f0f0f0;
}
.data-table tbody tr:hover { background-color: #f9fafb; }

.tag-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.tag-badge.tag-competition { background: #ede9fe; color: #7c3aed; }
.tag-badge.tag-project { background: #dbeafe; color: #1d4ed8; }
.tag-badge.tag-course { background: #fef3c7; color: #b45309; }

.status-select {
  padding: 4px 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; cursor: pointer;
}

.action-btn { padding: 6px 10px; border: none; border-radius: 4px; font-size: 16px; cursor: pointer; background: transparent; }
.delete-btn:hover { background-color: rgba(239, 68, 68, 0.1); }

.loading-state, .empty-state {
  display: flex; flex-direction: column; align-items: center; padding: 60px 20px; color: #999;
}
.loading-icon, .empty-icon { font-size: 48px; margin-bottom: 16px; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.pagination {
  display: flex; justify-content: center; align-items: center; gap: 16px;
  margin-top: 24px; padding: 16px 0;
}
.page-btn {
  padding: 8px 16px; background: #10b981; color: #fff; border: none;
  border-radius: 6px; font-size: 14px; cursor: pointer;
}
.page-btn:disabled { background: #d9d9d9; cursor: not-allowed; }
.page-info { font-size: 14px; color: #666; }
</style>
