<script setup>
import { ref, onMounted } from 'vue'
import { getAllCourses, createCourse, updateCourse, deleteCourse } from '@/api/course'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'

const courses = ref([])
const isLoading = ref(false)
const searchKeyword = ref('')

// 对话框
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const editingCourse = ref(null)
const createForm = ref({
  courseName: ''
})
const editForm = ref({
  courseName: ''
})

const loadCourses = async () => {
  isLoading.value = true
  try {
    const res = await getAllCourses()
    if (res.code === 200) {
      // 如果有搜索关键词，进行前端过滤
      let filteredCourses = res.data || []
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.toLowerCase()
        filteredCourses = filteredCourses.filter(course => 
          course.courseName.toLowerCase().includes(keyword)
        )
      }
      courses.value = filteredCourses
    }
  } catch (error) {
    logError('加载课程列表失败:', error)
    toast.error('加载课程列表失败')
  } finally {
    isLoading.value = false
  }
}

// 搜索课程
const searchCourses = () => {
  loadCourses()
}

// 打开新建对话框
const openCreateDialog = () => {
  createForm.value = {
    courseName: ''
  }
  showCreateDialog.value = true
}

// 关闭新建对话框
const closeCreateDialog = () => {
  showCreateDialog.value = false
  createForm.value = {
    courseName: ''
  }
}

// 创建课程
const createNewCourse = async () => {
  if (!createForm.value.courseName.trim()) {
    toast.error('课程名称不能为空')
    return
  }
  
  try {
    const res = await createCourse(createForm.value.courseName)
    if (res.code === 200) {
      toast.success('课程创建成功')
      closeCreateDialog()
      loadCourses()
    } else {
      toast.error(res.message || '创建失败')
    }
  } catch (error) {
    logError('创建课程失败:', error)
    toast.error('操作失败')
  }
}

// 打开编辑对话框
const openEditDialog = (course) => {
  editingCourse.value = course
  editForm.value = {
    courseName: course.courseName,
    isActive: course.isActive !== undefined ? course.isActive : true
  }
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false
  editingCourse.value = null
  editForm.value = {
    courseName: '',
    isActive: true
  }
}

// 更新课程
const updateExistingCourse = async () => {
  if (!editForm.value.courseName.trim()) {
    toast.error('课程名称不能为空')
    return
  }
  
  try {
    const res = await updateCourse(
      editingCourse.value.id,
      editForm.value.courseName,
      editForm.value.isActive
    )
    if (res.code === 200) {
      toast.success('课程更新成功')
      closeEditDialog()
      loadCourses()
    } else {
      toast.error(res.message || '更新失败')
    }
  } catch (error) {
    logError('更新课程失败:', error)
    toast.error('操作失败')
  }
}

// 删除课程
const deleteExistingCourse = async (courseId) => {
  if (!confirm('确定要删除该课程吗？')) return
  
  try {
    const res = await deleteCourse(courseId)
    if (res.code === 200) {
      toast.success('课程已删除')
      loadCourses()
    } else {
      toast.error(res.message || '删除失败')
    }
  } catch (error) {
    logError('删除课程失败:', error)
    toast.error('操作失败')
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">📚 课程管理</h2>
      <div class="header-actions">
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索课程名称..."
            class="search-input"
            @keyup.enter="searchCourses"
          />
          <button @click="searchCourses" class="search-btn">搜索</button>
        </div>
        <button @click="openCreateDialog" class="btn-create">+ 新建课程</button>
      </div>
    </div>
    
    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="courses.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>暂无课程数据</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>课程名称</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.id">
            <td>{{ course.id }}</td>
            <td>{{ course.courseName }}</td>
            <td>
              <div class="action-buttons">
                <button
                  @click="openEditDialog(course)"
                  class="action-btn edit-btn"
                  title="编辑课程"
                >
                  ✏️
                </button>
                <button
                  @click="deleteExistingCourse(course.id)"
                  class="action-btn delete-btn"
                  title="删除课程"
                >
                  🗑️
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建课程对话框 -->
    <div v-if="showCreateDialog" class="modal-overlay" @click="closeCreateDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">➕ 新建课程</h3>
          <button @click="closeCreateDialog" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">课程名称 *</label>
            <input
              v-model="createForm.courseName"
              type="text"
              class="form-input"
              placeholder="请输入课程名称"
            />
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeCreateDialog" class="btn btn-cancel">取消</button>
          <button @click="createNewCourse" class="btn btn-save">创建</button>
        </div>
      </div>
    </div>

    <!-- 编辑课程对话框 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">✏️ 编辑课程</h3>
          <button @click="closeEditDialog" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">课程名称 *</label>
            <input
              v-model="editForm.courseName"
              type="text"
              class="form-input"
              placeholder="请输入课程名称"
            />
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeEditDialog" class="btn btn-cancel">取消</button>
          <button @click="updateExistingCourse" class="btn btn-save">保存</button>
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
