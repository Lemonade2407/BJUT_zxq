<script setup>
import { ref, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const projects = ref([])
const isLoading = ref(false)
const searchKeyword = ref('')

// 编辑对话框
const showEditDialog = ref(false)
const editingProject = ref(null)
const editForm = ref({
  name: '',
  description: '',
  projectType: '',
  visibility: 1,
  courseName: '',
  tagIds: []
})

// 所有可用标签
const allTags = ref([])

const loadProjects = async () => {
  isLoading.value = true
  try {
    let url = '/api/projects/list?pageNum=1&pageSize=100'
    if (searchKeyword.value.trim()) {
      url = `/api/projects/search/name?name=${encodeURIComponent(searchKeyword.value)}&pageNum=1&pageSize=100`
    }
    
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.code === 200) {
        // enrichProject 已经包含了标签数据，直接使用
        projects.value = data.data.records || []
      }
    }
  } catch (error) {
    logError('加载项目列表失败:', error)
    toast.error('加载项目列表失败')
  } finally {
    isLoading.value = false
  }
}

// 加载所有标签
const loadTags = async () => {
  try {
    const response = await fetch('/api/tags', {
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.code === 200) {
        allTags.value = data.data || []
      }
    }
  } catch (error) {
    logError('加载标签列表失败:', error)
  }
}

// 搜索项目
const searchProjects = () => {
  loadProjects()
}

const deleteProject = async (projectId) => {
  if (!confirm('确定要删除该项目吗？')) return
  
  try {
    const response = await fetch(`/api/projects/${projectId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      toast.success('项目已删除')
      loadProjects()
    }
  } catch (error) {
    logError('删除项目失败:', error)
    toast.error('操作失败')
  }
}

// 打开编辑对话框
const openEditDialog = (project) => {
  editingProject.value = project
  // 提取项目的标签 ID 列表
  const tagIds = project.tags ? project.tags.map(tag => tag.id) : []
  
  editForm.value = {
    name: project.name,
    description: project.description || '',
    projectType: project.projectType || '',
    visibility: project.visibility,
    courseName: project.courseName || '',
    tagIds: tagIds
  }
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false
  editingProject.value = null
  editForm.value = {
    name: '',
    description: '',
    projectType: '',
    visibility: 1,
    courseName: '',
    tagIds: []
  }
}

// 保存项目信息
const saveProject = async () => {
  if (!editForm.value.name.trim()) {
    toast.error('项目名称不能为空')
    return
  }
  
  try {
    const response = await fetch(`/api/admin/projects/${editingProject.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: JSON.stringify(editForm.value)
    })
    
    if (response.ok) {
      toast.success('项目信息已更新')
      closeEditDialog()
      loadProjects()
    } else {
      const data = await response.json()
      toast.error(data.message || '更新失败')
    }
  } catch (error) {
    logError('更新项目失败:', error)
    toast.error('操作失败')
  }
}

onMounted(() => {
  loadProjects()
  loadTags()
})
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">📁 项目管理</h2>
      <div class="search-box">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索项目名称..."
          class="search-input"
          @keyup.enter="searchProjects"
        />
        <button @click="searchProjects" class="search-btn">搜索</button>
      </div>
    </div>
    
    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="projects.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>暂无项目数据</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>项目名称</th>
            <th>所有者</th>
            <th>类型</th>
            <th>可见性</th>
            <th>浏览量</th>
            <th>收藏数</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="project in projects" :key="project.id">
            <td>{{ project.id }}</td>
            <td>{{ project.name }}</td>
            <td>{{ project.ownerId }}</td>
            <td>{{ project.projectType }}</td>
            <td>{{ project.visibility === 1 ? '公开' : '私有' }}</td>
            <td>{{ project.viewCount || 0 }}</td>
            <td>{{ project.starCount || 0 }}</td>
            <td>{{ new Date(project.createdAt).toLocaleDateString('zh-CN') }}</td>
            <td>
              <div class="action-buttons">
                <button
                  @click="openEditDialog(project)"
                  class="action-btn edit-btn"
                  title="编辑项目"
                >
                  ✏️
                </button>
                <button
                  @click="deleteProject(project.id)"
                  class="action-btn delete-btn"
                  title="删除项目"
                >
                  🗑️
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 编辑项目对话框 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">✏️ 编辑项目信息</h3>
          <button @click="closeEditDialog" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">项目名称 *</label>
            <input
              v-model="editForm.name"
              type="text"
              class="form-input"
              placeholder="请输入项目名称"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">项目描述</label>
            <textarea
              v-model="editForm.description"
              class="form-input form-textarea"
              placeholder="请输入项目描述"
              rows="3"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label class="form-label">项目类型</label>
            <input
              v-model="editForm.projectType"
              type="text"
              class="form-input"
              placeholder="例如：课程设计、毕业设计"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">所属课程</label>
            <input
              v-model="editForm.courseName"
              type="text"
              class="form-input"
              placeholder="请输入课程名称"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">可见性</label>
            <select v-model="editForm.visibility" class="form-select">
              <option :value="1">公开</option>
              <option :value="0">私有</option>
            </select>
          </div>
          
          <div class="form-group">
            <label class="form-label">项目标签</label>
            <div class="tag-selector">
              <div v-if="allTags.length === 0" style="color: #999; padding: 10px;">
                暂无可用标签
              </div>
              <label
                v-for="tag in allTags"
                :key="tag.id"
                class="tag-checkbox"
              >
                <input
                  type="checkbox"
                  :value="tag.id"
                  v-model="editForm.tagIds"
                />
                <span class="tag-name">{{ tag.name }}</span>
              </label>
            </div>
            <div style="margin-top: 8px; font-size: 12px; color: #666;">
              已选择 {{ editForm.tagIds.length }} 个标签
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeEditDialog" class="btn btn-cancel">取消</button>
          <button @click="saveProject" class="btn btn-save">保存</button>
        </div>
      </div>
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
  margin: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.search-box {
  display: flex;
  gap: 12px;
}

.search-input {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  width: 300px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.search-btn {
  padding: 8px 20px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  background-color: #059669;
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

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn:hover {
  transform: scale(1.1);
}

.edit-btn:hover {
  background-color: rgba(59, 130, 246, 0.1);
}

.delete-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: #ffffff;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #064e3b;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999999;
  cursor: pointer;
  transition: color 0.2s;
  line-height: 1;
}

.close-btn:hover {
  color: #333333;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 8px;
}

.form-input,
.form-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #666666;
}

.btn-cancel:hover {
  background-color: #e0e0e0;
}

.btn-save {
  background-color: #10b981;
  color: #ffffff;
}

.btn-save:hover {
  background-color: #059669;
}

/* 标签选择器 */
.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.tag-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #f9fafb;
}

.tag-checkbox:hover {
  border-color: #10b981;
  background-color: rgba(16, 185, 129, 0.05);
}

.tag-checkbox input[type="checkbox"] {
  margin: 0;
  cursor: pointer;
}

.tag-checkbox input[type="checkbox"]:checked + .tag-name {
  color: #10b981;
  font-weight: 500;
}

.tag-name {
  font-size: 13px;
  color: #666666;
  user-select: none;
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
