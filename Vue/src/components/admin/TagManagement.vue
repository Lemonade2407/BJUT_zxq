<script setup>
import { ref, onMounted } from 'vue'
import { getTags } from '@/api/tag'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const tags = ref([])
const isLoading = ref(false)
const searchKeyword = ref('')

// 对话框
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const editingTag = ref(null)
const createForm = ref({
  name: '',
  category: ''
})
const editForm = ref({
  name: '',
  category: ''
})

const loadTags = async () => {
  isLoading.value = true
  try {
    const res = await getTags()
    if (res.code === 200) {
      // 如果有搜索关键词，进行前端过滤
      let filteredTags = res.data || []
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.toLowerCase()
        filteredTags = filteredTags.filter(tag => 
          tag.name.toLowerCase().includes(keyword) ||
          (tag.category && tag.category.toLowerCase().includes(keyword))
        )
      }
      tags.value = filteredTags
    }
  } catch (error) {
    logError('加载标签列表失败:', error)
    toast.error('加载标签列表失败')
  } finally {
    isLoading.value = false
  }
}

// 搜索标签
const searchTags = () => {
  loadTags()
}

// 打开新建对话框
const openCreateDialog = () => {
  createForm.value = {
    name: '',
    category: ''
  }
  showCreateDialog.value = true
}

// 关闭新建对话框
const closeCreateDialog = () => {
  showCreateDialog.value = false
  createForm.value = {
    name: '',
    category: ''
  }
}

// 创建标签
const createTag = async () => {
  if (!createForm.value.name.trim()) {
    toast.error('标签名称不能为空')
    return
  }
  
  if (!createForm.value.category.trim()) {
    toast.error('标签分类不能为空')
    return
  }
  
  try {
    const params = new URLSearchParams()
    params.append('name', createForm.value.name)
    params.append('category', createForm.value.category)
    
    const response = await fetch('/api/tags', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: params
    })
    
    if (response.ok) {
      toast.success('标签创建成功')
      closeCreateDialog()
      loadTags()
    } else {
      const data = await response.json()
      toast.error(data.message || '创建失败')
    }
  } catch (error) {
    logError('创建标签失败:', error)
    toast.error('操作失败')
  }
}

// 打开编辑对话框
const openEditDialog = (tag) => {
  editingTag.value = tag
  editForm.value = {
    name: tag.name,
    category: tag.category || ''
  }
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false
  editingTag.value = null
  editForm.value = {
    name: '',
    category: ''
  }
}

// 更新标签
const updateTag = async () => {
  if (!editForm.value.name.trim()) {
    toast.error('标签名称不能为空')
    return
  }
  
  if (!editForm.value.category.trim()) {
    toast.error('标签分类不能为空')
    return
  }
  
  try {
    const response = await fetch(`/api/tags/${editingTag.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: `name=${encodeURIComponent(editForm.value.name)}&category=${encodeURIComponent(editForm.value.category)}`
    })
    
    if (response.ok) {
      toast.success('标签更新成功')
      closeEditDialog()
      loadTags()
    } else {
      const data = await response.json()
      toast.error(data.message || '更新失败')
    }
  } catch (error) {
    logError('更新标签失败:', error)
    toast.error('操作失败')
  }
}

// 删除标签
const deleteTag = async (tagId) => {
  if (!confirm('确定要删除该标签吗？')) return
  
  try {
    const response = await fetch(`/api/tags/${tagId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      toast.success('标签已删除')
      loadTags()
    } else {
      const data = await response.json()
      toast.error(data.message || '删除失败')
    }
  } catch (error) {
    logError('删除标签失败:', error)
    toast.error('操作失败')
  }
}

onMounted(() => {
  loadTags()
})
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">🏷️ 标签管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索标签名称或分类..."
            class="search-input"
            @keyup.enter="searchTags"
          />
          <button @click="searchTags" class="search-btn">搜索</button>
        </div>
        <button @click="openCreateDialog" class="btn-create">+ 新建标签</button>
      </div>
    </div>
    
    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="tags.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>暂无标签数据</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标签名称</th>
            <th>分类</th>
            <th>使用次数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tag in tags" :key="tag.id">
            <td>{{ tag.id }}</td>
            <td>{{ tag.name }}</td>
            <td>{{ tag.category || '-' }}</td>
            <td>{{ tag.usageCount || 0 }}</td>
            <td>
              <div class="action-buttons">
                <button
                  @click="openEditDialog(tag)"
                  class="action-btn edit-btn"
                  title="编辑标签"
                >
                  ✏️
                </button>
                <button
                  @click="deleteTag(tag.id)"
                  class="action-btn delete-btn"
                  title="删除标签"
                >
                  🗑️
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建标签对话框 -->
    <div v-if="showCreateDialog" class="modal-overlay" @click="closeCreateDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">➕ 新建标签</h3>
          <button @click="closeCreateDialog" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">标签名称 *</label>
            <input
              v-model="createForm.name"
              type="text"
              class="form-input"
              placeholder="请输入标签名称"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">标签分类 *</label>
            <input
              v-model="createForm.category"
              type="text"
              class="form-input"
              placeholder="例如：技术栈、领域标签、其他标签"
            />
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeCreateDialog" class="btn btn-cancel">取消</button>
          <button @click="createTag" class="btn btn-save">创建</button>
        </div>
      </div>
    </div>

    <!-- 编辑标签对话框 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">✏️ 编辑标签</h3>
          <button @click="closeEditDialog" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">标签名称 *</label>
            <input
              v-model="editForm.name"
              type="text"
              class="form-input"
              placeholder="请输入标签名称"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">标签分类 *</label>
            <input
              v-model="editForm.category"
              type="text"
              class="form-input"
              placeholder="例如：技术栈、领域标签、其他标签"
            />
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeEditDialog" class="btn btn-cancel">取消</button>
          <button @click="updateTag" class="btn btn-save">保存</button>
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

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
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
  width: 250px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.search-btn {
  padding: 8px 16px;
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

.btn-create {
  padding: 8px 20px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-create:hover {
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
  max-width: 500px;
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

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form-input:focus {
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
