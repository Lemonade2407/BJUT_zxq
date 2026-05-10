<script setup>
import { ref, computed, onMounted } from 'vue'
import { getTeams, createTeam, updateTeam, deleteTeam, applyToTeam, hasAppliedToTeam } from '@/api/team'
import { getActiveCourses } from '@/api/course'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'

const PAGE_SIZE = 12
const currentPage = ref(1)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

const teams = ref([])
const isLoading = ref(false)
const tagFilter = ref('')
const statusFilter = ref(null)
const courseFilter = ref('')

// 课程列表（用于课设tag）
const courses = ref([])
const filterCourses = ref([])

// 创建/编辑对话框
const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const form = ref({
  title: '',
  description: '',
  currentMembers: 1,
  neededMembers: 2,
  tag: 'PROJECT',
  courseName: ''
})

// 详情对话框
const showDetail = ref(false)
const detailTeam = ref(null)

// 申请入队
const showApplyDialog = ref(false)
const applyTeam = ref(null)
const applyMessage = ref('')
const appliedTeamIds = ref(new Set())

const checkApplied = () => {
  Promise.allSettled(teams.value.map(team =>
    hasAppliedToTeam(team.id).then(res => {
      if (res.code === 200 && res.data) appliedTeamIds.value.add(team.id)
    }).catch(() => {})
  ))
}

const openApply = (team) => {
  applyTeam.value = team
  applyMessage.value = ''
  showApplyDialog.value = true
}

const submitApply = async () => {
  try {
    await applyToTeam(applyTeam.value.id, applyMessage.value)
    toast.success('申请已提交')
    appliedTeamIds.value.add(applyTeam.value.id)
    showApplyDialog.value = false
  } catch (e) {
    toast.error(e.message || '申请失败')
  }
}

const TAG_OPTIONS = [
  { value: 'COMPETITION', label: '竞赛' },
  { value: 'PROJECT', label: '项目' },
  { value: 'COURSE', label: '课设' }
]

const TAG_MAP = { COMPETITION: '竞赛', PROJECT: '项目', COURSE: '课设' }
const STATUS_MAP = { 0: '已结束', 1: '招募中', 2: '已满员' }

const loadTeams = async () => {
  isLoading.value = true
  try {
    const res = await getTeams({
      pageNum: currentPage.value,
      pageSize: PAGE_SIZE,
      tag: tagFilter.value || undefined,
      status: statusFilter.value,
      courseName: tagFilter.value === 'COURSE' && courseFilter.value ? courseFilter.value : undefined
    })
    if (res.code === 200 && res.data) {
      teams.value = res.data.records || []
      total.value = res.data.total || 0
      appliedTeamIds.value = new Set()
      checkApplied()
    }
  } catch (e) {
    logError('加载组队列表失败:', e)
    toast.error('加载组队列表失败')
  } finally {
    isLoading.value = false
  }
}

const loadCourses = async () => {
  try {
    const res = await getActiveCourses()
    if (res.code === 200) courses.value = res.data || []
  } catch (e) { /* ignore */ }
}

const onFilterChange = () => {
  currentPage.value = 1
  if (tagFilter.value !== 'COURSE') courseFilter.value = ''
  loadTeams()
}

const onCourseFilterChange = () => {
  currentPage.value = 1
  loadTeams()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadTeams()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openCreate = async () => {
  isEditing.value = false
  editingId.value = null
  form.value = { title: '', description: '', currentMembers: 1, neededMembers: 2, tag: 'PROJECT', courseName: '' }
  await loadCourses()
  showDialog.value = true
}

const openEdit = async (team) => {
  isEditing.value = true
  editingId.value = team.id
  form.value = {
    title: team.title,
    description: team.description || '',
    currentMembers: team.currentMembers,
    neededMembers: team.neededMembers,
    tag: team.tag,
    courseName: team.courseName || ''
  }
  await loadCourses()
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const submitForm = async () => {
  if (!form.value.title.trim()) { toast.error('请输入组队标题'); return }
  if (!form.value.neededMembers || form.value.neededMembers < 2) { toast.error('需要成员至少为2人'); return }
  if (form.value.currentMembers < 1) { toast.error('已有成员至少为1人'); return }
  try {
    if (isEditing.value) {
      await updateTeam(editingId.value, form.value)
      toast.success('组队信息已更新')
    } else {
      await createTeam(form.value)
      toast.success('组队创建成功')
    }
    closeDialog()
    loadTeams()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

const handleDelete = async (team) => {
  if (!confirm(`确定要删除组队"${team.title}"吗？`)) return
  try {
    await deleteTeam(team.id)
    toast.success('组队已删除')
    loadTeams()
  } catch (e) {
    toast.error(e.message || '删除失败')
  }
}

const openDetail = (team) => {
  detailTeam.value = team
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  detailTeam.value = null
}

const loadFilterCourses = async () => {
  try {
    const res = await getActiveCourses()
    if (res.code === 200) filterCourses.value = res.data || []
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadTeams()
  loadFilterCourses()
})
</script>

<template>
  <main class="app-main">
    <div class="team-square-container">
      <div class="page-header">
        <h1 class="page-title">组队广场</h1>
        <p class="page-description">寻找志同道合的队友，一起创造优秀项目</p>
        <div class="header-buttons">
          <button @click="openCreate" class="btn-primary">+ 发布组队</button>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-group">
          <label class="filter-label">类型：</label>
          <button :class="['filter-tag', { active: tagFilter === '' }]" @click="tagFilter = ''; onFilterChange()">全部</button>
          <button v-for="t in TAG_OPTIONS" :key="t.value" :class="['filter-tag', { active: tagFilter === t.value }]" @click="tagFilter = t.value; onFilterChange()">{{ t.label }}</button>
        </div>
        <div class="filter-group">
          <label class="filter-label">状态：</label>
          <select v-model="statusFilter" @change="onFilterChange" class="filter-select">
            <option :value="null">全部</option>
            <option :value="1">招募中</option>
            <option :value="2">已满员</option>
            <option :value="0">已结束</option>
          </select>
        </div>
        <div v-if="tagFilter === 'COURSE'" class="filter-group">
          <label class="filter-label">课程：</label>
          <select v-model="courseFilter" @change="onCourseFilterChange" class="filter-select">
            <option value="">全部课程</option>
            <option v-for="c in filterCourses" :key="c.id" :value="c.courseName">{{ c.courseName }}</option>
          </select>
        </div>
      </div>

      <!-- 组队卡片网格 -->
      <div v-if="isLoading" class="loading-state"><span class="loading-icon">⏳</span><p>加载中...</p></div>

      <div v-else-if="teams.length === 0" class="empty-state">
        <span class="empty-icon">📥</span>
        <p class="empty-text">暂无组队信息</p>
        <button @click="openCreate" class="btn-primary" style="margin-top:16px">发布第一个组队</button>
      </div>

      <div v-else class="project-grid">
        <div v-for="team in teams" :key="team.id" class="project-card">
          <div class="project-header">
            <h3 class="project-title">{{ team.title }}</h3>
            <span :class="['status-badge-sm', `status-${team.status}`]">{{ STATUS_MAP[team.status] }}</span>
          </div>
          <div class="card-tags">
            <span :class="['tag-badge', `tag-${team.tag?.toLowerCase()}`]">{{ TAG_MAP[team.tag] || team.tag }}</span>
            <span v-if="team.courseName" class="course-badge">{{ team.courseName }}</span>
          </div>
          <p class="project-description">{{ team.description || '暂无简介' }}</p>
          <div class="project-footer">
            <div class="creator-info">
              <span class="creator-name">{{ team.creatorUsername || '匿名' }}</span>
            </div>
            <div class="members-info">
              <span class="members-count">{{ team.currentMembers }}/{{ team.neededMembers }} 人</span>
            </div>
            <div class="card-buttons">
              <button class="detail-btn" @click="openDetail(team)">查看详情</button>
              <button v-if="team.status === 1 && !appliedTeamIds.has(team.id)" class="apply-btn" @click="openApply(team)">申请入队</button>
              <button v-else-if="appliedTeamIds.has(team.id)" class="applied-btn" disabled>已申请</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-container">
        <div class="pagination">
          <button class="page-btn prev" @click="changePage(currentPage - 1)" :disabled="currentPage === 1">‹ 上一页</button>
          <button v-for="page in totalPages" :key="page" :class="['page-btn', { active: page === currentPage }]" @click="changePage(page)">{{ page }}</button>
          <button class="page-btn next" @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages">下一页 ›</button>
        </div>
        <div class="page-info">共 {{ total }} 条，第 {{ currentPage }} / {{ totalPages }} 页</div>
      </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <div v-if="showDialog" class="modal-overlay" @click="closeDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">{{ isEditing ? '编辑组队' : '发布组队' }}</h3>
          <button @click="closeDialog" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">组队标题 *</label>
            <input v-model="form.title" class="form-input" placeholder="请输入组队标题" maxlength="200" />
          </div>
          <div class="form-group">
            <label class="form-label">组队类型 *</label>
            <select v-model="form.tag" class="form-input">
              <option v-for="t in TAG_OPTIONS" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <div class="form-group" v-if="form.tag === 'COURSE'">
            <label class="form-label">课程名称</label>
            <select v-model="form.courseName" class="form-input">
              <option value="">请选择课程</option>
              <option v-for="c in courses" :key="c.id" :value="c.courseName">{{ c.courseName }}</option>
            </select>
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label class="form-label">已有成员</label>
              <input v-model.number="form.currentMembers" type="number" class="form-input" min="1" />
            </div>
            <div class="form-group half">
              <label class="form-label">需要成员 *</label>
              <input v-model.number="form.neededMembers" type="number" class="form-input" min="2" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">组队简介</label>
            <textarea v-model="form.description" class="form-input form-textarea" placeholder="描述您要找什么样的队友，组队目标是什么..." rows="4"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeDialog" class="btn btn-cancel">取消</button>
          <button @click="submitForm" class="btn btn-save">{{ isEditing ? '保存修改' : '发布' }}</button>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <div v-if="showDetail && detailTeam" class="modal-overlay" @click="closeDetail">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">{{ detailTeam.title }}</h3>
          <button @click="closeDetail" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-meta">
            <span :class="['tag-badge', `tag-${detailTeam.tag?.toLowerCase()}`]">{{ TAG_MAP[detailTeam.tag] || detailTeam.tag }}</span>
            <span v-if="detailTeam.courseName" class="course-badge">{{ detailTeam.courseName }}</span>
            <span :class="['status-badge-sm', `status-${detailTeam.status}`]">{{ STATUS_MAP[detailTeam.status] }}</span>
          </div>
          <div class="detail-row">
            <span>创建者：{{ detailTeam.creatorUsername || '匿名' }}</span>
            <span>成员：{{ detailTeam.currentMembers }}/{{ detailTeam.neededMembers }} 人</span>
          </div>
          <div class="detail-section">
            <h4>组队简介</h4>
            <p>{{ detailTeam.description || '暂无简介' }}</p>
          </div>
          <div class="detail-meta">
            <span>创建时间：{{ new Date(detailTeam.createdAt).toLocaleString('zh-CN') }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeDetail" class="btn btn-cancel">关闭</button>
        </div>
      </div>
    </div>

    <!-- 申请入队对话框 -->
    <div v-if="showApplyDialog && applyTeam" class="modal-overlay" @click.self="showApplyDialog = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">申请入队</h3>
          <button @click="showApplyDialog = false" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <p style="margin-bottom:12px;color:#666;">申请加入：<strong>{{ applyTeam.title }}</strong></p>
          <div class="form-group">
            <label class="form-label">个人简介</label>
            <textarea v-model="applyMessage" class="form-input form-textarea" placeholder="请填写个人简介，包括您的技能、经验、为什么想加入这个团队..." rows="4"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showApplyDialog = false" class="btn btn-cancel">取消申请</button>
          <button @click="submitApply" class="btn btn-save">确认申请</button>
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
  min-height: calc(100vh - 200px);
  width: 100%;
  background-color: #f5f7fa;
}

.team-square-container {
  max-width: 1400px;
  margin: 0 auto;
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
  margin: 0 0 4px 0;
}

.page-description {
  font-size: 16px;
  color: #666666;
  margin: 0;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.btn-primary {
  padding: 10px 24px;
  background-color: #10b981;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.btn-primary:hover { background-color: #059669; }

.filter-bar {
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 24px;
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label { font-size: 14px; font-weight: 500; color: #333; }
.filter-tag {
  padding: 6px 14px;
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  color: #666;
  font-size: 13px;
  cursor: pointer;
}
.filter-tag:hover { background: #e6e6e6; }
.filter-tag.active { background: #10b981; border-color: #10b981; color: #fff; }
.filter-select {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 13px;
}

/* 卡片 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
@media (max-width: 1200px) { .project-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .project-grid { grid-template-columns: 1fr; } }

.project-card {
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.project-card:hover { border-color: #10b981; box-shadow: 0 4px 12px rgba(6,78,59,0.12); transform: translateY(-2px); }

.project-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
.project-title { font-size: 16px; font-weight: 600; color: #064e3b; margin: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.status-badge-sm {
  padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; white-space: nowrap;
}
.status-badge-sm.status-1 { background: #d1fae5; color: #065f46; }
.status-badge-sm.status-2 { background: #dbeafe; color: #1e40af; }
.status-badge-sm.status-0 { background: #fee2e2; color: #991b1b; }

.card-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.tag-badge {
  padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500;
}
.tag-badge.tag-competition { background: #ede9fe; color: #7c3aed; }
.tag-badge.tag-project { background: #dbeafe; color: #1d4ed8; }
.tag-badge.tag-course { background: #fef3c7; color: #b45309; }
.course-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; background: #f3f4f6; color: #666; }

.project-description {
  font-size: 13px; color: #666; line-height: 1.5; margin: 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  flex: 1;
}

.project-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 10px; border-top: 1px solid #f0f0f0; gap: 6px;
  flex-wrap: wrap;
}
.creator-info { font-size: 12px; color: #888; min-width: 0; }
.creator-name { font-weight: 500; }
.members-count { font-size: 13px; color: #666; font-weight: 500; white-space: nowrap; }
.card-buttons { display: flex; gap: 6px; }
.detail-btn {
  padding: 6px 12px; background: #10b981; border: none; border-radius: 6px;
  color: #fff; font-size: 12px; cursor: pointer; white-space: nowrap;
}
.detail-btn:hover { background: #059669; }
.apply-btn {
  padding: 6px 12px; background: #3b82f6; border: none; border-radius: 6px;
  color: #fff; font-size: 12px; cursor: pointer; white-space: nowrap;
}
.apply-btn:hover { background: #2563eb; }
.applied-btn {
  padding: 6px 12px; background: #d1d5db; border: none; border-radius: 6px;
  color: #6b7280; font-size: 12px; cursor: not-allowed; white-space: nowrap;
}

/* 分页 */
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

/* 空状态和加载 */
.loading-state, .empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding: 80px 20px; color: #999;
}
.empty-state { background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; }
.loading-icon, .empty-icon { font-size: 48px; margin-bottom: 16px; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.empty-text { font-size: 16px; margin: 0; }

/* 模态框 */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center;
  justify-content: center; z-index: 1000;
}
.modal-content {
  background: #fff; border-radius: 12px; width: 90%; max-width: 520px;
  max-height: 85vh; overflow-y: auto; box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px; border-bottom: 1px solid #e0e0e0;
}
.modal-title { font-size: 20px; font-weight: 600; color: #064e3b; margin: 0; }
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
.btn { padding: 10px 24px; border: none; border-radius: 6px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-cancel { background: #f5f5f5; color: #666; }
.btn-cancel:hover { background: #e0e0e0; }
.btn-save { background: #10b981; color: #fff; }
.btn-save:hover { background: #059669; }

/* 详情 */
.detail-meta { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }
.detail-row { display: flex; gap: 20px; color: #666; font-size: 14px; margin-bottom: 16px; }
.detail-section h4 { margin: 0 0 8px 0; color: #333; font-size: 15px; }
.detail-section p { margin: 0; color: #666; font-size: 14px; line-height: 1.6; }

.status-text { font-size: 12px; }
.status-text.status-1 { color: #065f46; }
.status-text.status-2 { color: #1e40af; }
.status-text.status-0 { color: #991b1b; }
.members-text { font-size: 13px; color: #666; }
</style>
