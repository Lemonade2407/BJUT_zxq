<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyTeams, updateTeam, deleteTeam, getTeamApplications, approveApplication, rejectApplication } from '@/api/team'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import UserSidebar from '@/components/user/UserSidebar.vue'

const router = useRouter()
const PAGE_SIZE = 6
const currentPageNum = ref(1)
const allTeams = ref([])
const isLoading = ref(false)

const showEditDialog = ref(false)
const editingTeam = ref(null)
const editForm = ref({ title: '', description: '', currentMembers: 1, neededMembers: 2, tag: 'PROJECT', courseName: '' })

const TAG_MAP = { COMPETITION: '竞赛', PROJECT: '项目', COURSE: '课设' }
const STATUS_MAP = { 0: '已结束', 1: '招募中', 2: '已满员' }

const totalPages = computed(() => Math.ceil(allTeams.value.length / PAGE_SIZE))
const teams = computed(() => {
  const start = (currentPageNum.value - 1) * PAGE_SIZE
  return allTeams.value.slice(start, start + PAGE_SIZE)
})

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPageNum.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const loadTeams = async () => {
  isLoading.value = true
  try {
    const res = await getMyTeams()
    if (res.code === 200) allTeams.value = res.data || []
  } catch (e) {
    logError('加载我的组队失败:', e)
    toast.error('加载我的组队失败')
  } finally {
    isLoading.value = false
  }
}

const openEdit = (team) => {
  editingTeam.value = team
  editForm.value = {
    title: team.title, description: team.description || '',
    currentMembers: team.currentMembers, neededMembers: team.neededMembers,
    tag: team.tag, courseName: team.courseName || ''
  }
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editForm.value.title.trim()) { toast.error('标题不能为空'); return }
  try {
    await updateTeam(editingTeam.value.id, editForm.value)
    toast.success('组队已更新')
    showEditDialog.value = false
    loadTeams()
  } catch (e) { toast.error(e.message || '更新失败') }
}

const handleDelete = async (team) => {
  if (!confirm(`确定删除组队"${team.title}"吗？`)) return
  try {
    await deleteTeam(team.id)
    toast.success('组队已删除')
    loadTeams()
  } catch (e) { toast.error(e.message || '删除失败') }
}

// 申请审核
const expandedTeamId = ref(null)
const applications = ref({})
const loadingApps = ref({})

const toggleApplications = async (teamId) => {
  if (expandedTeamId.value === teamId) {
    expandedTeamId.value = null
    return
  }
  expandedTeamId.value = teamId
  if (!applications.value[teamId]) {
    loadingApps.value[teamId] = true
    try {
      const res = await getTeamApplications(teamId)
      if (res.code === 200) applications.value[teamId] = res.data || []
    } catch (e) { toast.error('加载申请列表失败') }
    finally { loadingApps.value[teamId] = false }
  }
}

const handleApprove = async (appId, teamId) => {
  try {
    await approveApplication(appId)
    toast.success('已通过')
    applications.value[teamId] = applications.value[teamId].filter(a => a.id !== appId)
    loadTeams()
  } catch (e) { toast.error(e.message || '操作失败') }
}

const handleReject = async (appId, teamId) => {
  try {
    await rejectApplication(appId)
    toast.success('已拒绝')
    applications.value[teamId] = applications.value[teamId].filter(a => a.id !== appId)
  } catch (e) { toast.error(e.message || '操作失败') }
}

const pendingCount = (teamId) => {
  const apps = applications.value[teamId]
  if (!apps) return null
  return apps.filter(a => a.status === 0).length
}

const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : '-'

onMounted(() => loadTeams())
</script>

<template>
  <main class="app-main">
    <div class="my-teams-layout">
      <UserSidebar />

      <div class="my-teams-main">
        <div class="my-teams-container">
          <div class="page-header">
            <div class="header-left">
              <h1 class="page-title">我的组队</h1>
              <p class="page-description">管理你发布的所有组队信息</p>
            </div>
            <button class="create-btn" @click="router.push('/team')">发布新组队</button>
          </div>

          <div v-if="isLoading" class="loading-state">
            <span class="loading-icon">⏳</span>
            <p>加载中...</p>
          </div>

          <div v-else-if="allTeams.length > 0" class="teams-grid">
            <div v-for="team in teams" :key="team.id" class="team-card">
              <div class="team-card-top">
                <div class="team-tags">
                  <span :class="['tag-badge', `tag-${team.tag?.toLowerCase()}`]">{{ TAG_MAP[team.tag] || team.tag }}</span>
                  <span v-if="team.courseName" class="course-badge">{{ team.courseName }}</span>
                </div>
                <span :class="['status-badge', `status-${team.status}`]">{{ STATUS_MAP[team.status] }}</span>
              </div>
              <h3 class="team-title">{{ team.title }}</h3>
              <p class="team-desc">{{ team.description || '暂无简介' }}</p>
              <div class="team-card-footer">
                <span class="team-members">{{ team.currentMembers }}/{{ team.neededMembers }} 人</span>
                <span class="team-date">{{ formatDate(team.createdAt) }}</span>
                <div class="team-actions">
                  <button @click.stop="toggleApplications(team.id)" class="action-btn-sm review-btn-sm">
                    审核申请
                    <span v-if="applications[team.id] && pendingCount(team.id) > 0" class="pending-badge">{{ pendingCount(team.id) }}</span>
                  </button>
                  <button @click.stop="openEdit(team)" class="action-btn-sm edit-btn-sm">编辑</button>
                  <button @click.stop="handleDelete(team)" class="action-btn-sm del-btn-sm">删除</button>
                </div>
              </div>

              <!-- 申请列表 -->
              <div v-if="expandedTeamId === team.id" class="applications-panel">
                <div v-if="loadingApps[team.id]" class="apps-loading">加载中...</div>
                <div v-else-if="!applications[team.id] || applications[team.id].length === 0" class="apps-empty">暂无申请</div>
                <div v-else v-for="app in applications[team.id]" :key="app.id" class="app-item">
                  <div class="app-info">
                    <span class="app-user">{{ app.applicantUsername || '用户'+app.applicantId }}</span>
                    <span v-if="app.message" class="app-msg">{{ app.message }}</span>
                    <span class="app-date">{{ formatDate(app.createdAt) }}</span>
                  </div>
                  <div class="app-actions">
                    <button v-if="app.status === 0" @click="handleApprove(app.id, team.id)" class="app-approve">通过</button>
                    <button v-if="app.status === 0" @click="handleReject(app.id, team.id)" class="app-reject">拒绝</button>
                    <span v-else class="app-status-text">{{ app.statusText }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-state">
            <span class="empty-icon">📭</span>
            <p class="empty-text">暂无发布的组队</p>
          </div>

          <div v-if="allTeams.length > 0" class="pagination-container">
            <div class="pagination">
              <button class="page-btn prev" @click="changePage(currentPageNum - 1)" :disabled="currentPageNum === 1">‹ 上一页</button>
              <button v-for="page in totalPages" :key="page" :class="['page-btn', { active: page === currentPageNum }]" @click="changePage(page)">{{ page }}</button>
              <button class="page-btn next" @click="changePage(currentPageNum + 1)" :disabled="currentPageNum >= totalPages">下一页 ›</button>
            </div>
            <div class="page-info">共 {{ allTeams.length }} 条，第 {{ currentPageNum }} / {{ totalPages }} 页</div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditDialog" class="modal-overlay" @click.self="showEditDialog = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>编辑组队</h3>
          <button @click="showEditDialog = false" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">标题 *</label>
            <input v-model="editForm.title" class="form-input" maxlength="200" />
          </div>
          <div class="form-group">
            <label class="form-label">类型</label>
            <select v-model="editForm.tag" class="form-input">
              <option value="COMPETITION">竞赛</option>
              <option value="PROJECT">项目</option>
              <option value="COURSE">课设</option>
            </select>
          </div>
          <div class="form-group" v-if="editForm.tag === 'COURSE'">
            <label class="form-label">课程名称</label>
            <input v-model="editForm.courseName" class="form-input" placeholder="请输入课程名称" />
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label class="form-label">已有成员</label>
              <input v-model.number="editForm.currentMembers" type="number" class="form-input" min="1" />
            </div>
            <div class="form-group half">
              <label class="form-label">需要成员</label>
              <input v-model.number="editForm.neededMembers" type="number" class="form-input" min="2" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">简介</label>
            <textarea v-model="editForm.description" class="form-input form-textarea" rows="4"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showEditDialog = false" class="btn-cancel">取消</button>
          <button @click="handleUpdate" class="btn-save">保存</button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  width: 100%;
  background-color: #f5f7fa;
}

.my-teams-layout {
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 180px);
}

.my-teams-main {
  flex: 1;
  min-width: 0;
}

.my-teams-container {
  max-width: 1200px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #064e3b;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.create-btn {
  padding: 10px 24px;
  background: #10b981;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}
.create-btn:hover { background: #059669; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}
.loading-icon { font-size: 48px; margin-bottom: 16px; animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.teams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
  flex: 1;
  align-content: start;
}

.team-card {
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.team-card:hover {
  border-color: #10b981;
  box-shadow: 0 4px 12px rgba(6,78,59,0.12);
  transform: translateY(-2px);
}

.team-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.team-tags {
  display: flex;
  gap: 6px;
  align-items: center;
}

.tag-badge {
  padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500;
}
.tag-badge.tag-competition { background: #ede9fe; color: #7c3aed; }
.tag-badge.tag-project { background: #dbeafe; color: #1d4ed8; }
.tag-badge.tag-course { background: #fef3c7; color: #b45309; }

.course-badge {
  padding: 2px 8px; border-radius: 4px; font-size: 11px;
  background: #f3f4f6; color: #666;
}

.status-badge {
  padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 500;
}
.status-badge.status-1 { background: #d1fae5; color: #065f46; }
.status-badge.status-2 { background: #dbeafe; color: #1e40af; }
.status-badge.status-0 { background: #fee2e2; color: #991b1b; }

.team-title {
  font-size: 16px; font-weight: 600; color: #10b981; margin: 0;
}

.team-desc {
  font-size: 14px; color: #666; line-height: 1.5; margin: 0;
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  flex: 1;
}

.team-card-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  flex-wrap: wrap;
}

.team-members { color: #666; }
.team-date { color: #999; flex: 1; }
.team-actions { display: flex; gap: 6px; }

.action-btn-sm {
  padding: 5px 12px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: none; color: #fff; white-space: nowrap; transition: all 0.15s;
}
.edit-btn-sm { background: #3b82f6; }
.edit-btn-sm:hover { background: #2563eb; }
.del-btn-sm { background: #ef4444; }
.del-btn-sm:hover { background: #dc2626; }
.review-btn-sm { background: #8b5cf6; position: relative; }
.review-btn-sm:hover { background: #7c3aed; }

.pending-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; background: #ef4444; color: #fff;
  border-radius: 9px; font-size: 10px; margin-left: 4px; padding: 0 4px;
}

.applications-panel {
  margin-top: 12px; padding: 12px; background: #f9fafb; border-radius: 8px; border: 1px solid #e5e7eb;
}
.apps-loading, .apps-empty { padding: 12px; text-align: center; color: #999; font-size: 13px; }
.app-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #e5e7eb; }
.app-item:last-child { border-bottom: none; }
.app-info { display: flex; flex-direction: column; gap: 2px; }
.app-user { font-weight: 600; font-size: 14px; color: #333; }
.app-msg { font-size: 12px; color: #888; }
.app-date { font-size: 11px; color: #bbb; }
.app-actions { display: flex; gap: 6px; align-items: center; }
.app-approve { padding: 4px 14px; background: #10b981; color: #fff; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; }
.app-approve:hover { background: #059669; }
.app-reject { padding: 4px 14px; background: #ef4444; color: #fff; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; }
.app-reject:hover { background: #dc2626; }
.app-status-text { font-size: 12px; color: #888; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  flex: 1;
}
.empty-icon { font-size: 64px; margin-bottom: 16px; opacity: 0.5; }
.empty-text { font-size: 16px; color: #999; }

.pagination-container { display: flex; flex-direction: column; align-items: center; gap: 16px; margin-top: 32px; padding-top: 24px; border-top: 1px solid #e0e0e0; }
.pagination { display: flex; align-items: center; gap: 8px; }
.page-btn {
  min-width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  padding: 0 12px; background-color: #fff; border: 1px solid #d9d9d9;
  border-radius: 6px; color: #333; font-size: 14px; cursor: pointer; transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { background-color: #10b981; border-color: #10b981; color: #fff; }
.page-btn.active { background-color: #10b981; border-color: #10b981; color: #fff; font-weight: 600; }
.page-btn:disabled { background-color: #f5f5f5; color: #ccc; cursor: not-allowed; opacity: 0.6; }
.page-info { font-size: 13px; color: #666; }

/* modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 12px; width: 90%; max-width: 500px; max-height: 85vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #e0e0e0; }
.modal-header h3 { margin: 0; font-size: 18px; color: #064e3b; }
.close-btn { background: none; border: none; font-size: 28px; color: #999; cursor: pointer; }
.modal-body { padding: 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #e0e0e0; }
.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 14px; font-weight: 500; color: #333; margin-bottom: 6px; }
.form-input { width: 100%; padding: 10px 14px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; }
.form-input:focus { outline: none; border-color: #10b981; }
.form-textarea { resize: vertical; }
.form-row { display: flex; gap: 12px; }
.form-group.half { flex: 1; }
.btn-cancel { padding: 10px 24px; background: #f5f5f5; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; color: #666; }
.btn-save { padding: 10px 24px; background: #10b981; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-save:hover { background: #059669; }

@media (max-width: 768px) {
  .my-teams-layout { flex-direction: column; }
  .teams-grid { grid-template-columns: 1fr; }
}
</style>
