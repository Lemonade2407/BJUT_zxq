<template>
  <div class="search-result-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1>🔍 搜索结果</h1>
        <p class="subtitle">关键词: "{{ searchKeyword }}" - 找到 {{ filteredProjects.length }} 个项目</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredProjects.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <h3>未找到相关项目</h3>
        <p>尝试更换搜索关键词或浏览项目广场</p>
        <router-link to="/projects" class="btn-primary">浏览项目广场</router-link>
      </div>

      <!-- 项目列表 -->
      <div v-else class="project-grid">
        <div 
          v-for="project in paginatedProjects" 
          :key="project.id"
          class="project-card"
          @click="goToProject(project.id)"
        >
          <div class="project-card-header">
            <span class="project-name">{{ project.name }}</span>
          </div>
          <p class="project-description">{{ project.description || '暂无描述' }}</p>
          <div class="project-tags">
            <span v-for="tag in (project.tags || [])" :key="tag.id" class="tech-tag">
              {{ tag.name }}
            </span>
          </div>
          <div class="project-footer">
            <div class="project-stats">
              <span class="stat">
                ❤️ {{ formatNumber(project.starCount || 0) }}
              </span>
              <span class="stat">
                ⭐ {{ formatNumber(project.watchCount || 0) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页控件 -->
      <div v-if="filteredProjects.length > 0" class="pagination-container">
        <div class="pagination">
          <button 
            class="page-btn"
            @click="changePage(currentPageNum - 1)"
            :disabled="currentPageNum === 1"
          >
            ‹ 上一页
          </button>
          
          <button 
            v-for="page in totalPages" 
            :key="page"
            :class="['page-btn', { active: page === currentPageNum }]"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          
          <button 
            class="page-btn"
            @click="changePage(currentPageNum + 1)"
            :disabled="currentPageNum === totalPages"
          >
            下一页 ›
          </button>
        </div>
        
        <div class="page-info">
          共 {{ filteredProjects.length }} 个项目，第 {{ currentPageNum }} / {{ totalPages }} 页
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { searchProjects } from '@/api/project'
import { error as logError } from '@/utils/logger'
import { formatNumber } from '@/utils/helpers'


const router = useRouter()
const route = useRoute()

const loading = ref(false)
const filteredProjects = ref([])
const searchKeyword = ref('')
const total = ref(0) // 总记录数

// 每页显示的项目数量
const PAGE_SIZE = 6
const currentPageNum = ref(1)

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE))

// 计算当前页的项目列表（直接使用后端返回的数据）
const paginatedProjects = computed(() => {
  return filteredProjects.value
})

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPageNum.value = page
    loadSearchResults() // 重新加载数据
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 跳转到项目详情
const goToProject = (projectId) => {
  router.push(`/project/${projectId}`)
}

// 加载搜索结果
const loadSearchResults = async () => {
  if (!searchKeyword.value.trim()) {
    filteredProjects.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const res = await searchProjects(searchKeyword.value, {
      pageNum: currentPageNum.value,
      pageSize: PAGE_SIZE
    })
    if (res.code === 200) {
      const data = res.data || {}
      // 后端返回的是 PageResult 对象，项目列表在 records 字段中
      filteredProjects.value = data.records || []
      total.value = data.total || 0
    }
  } catch (error) {
    logError('搜索失败:', error)
    filteredProjects.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 监听路由参数变化
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    searchKeyword.value = newKeyword
    currentPageNum.value = 1 // 重置到第一页
    loadSearchResults() // 重新加载搜索结果
  }
}, { immediate: true })

onMounted(() => {
  // 初始加载由 watch 处理
})
</script>

<style scoped>
.search-result-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-height: calc(100vh - 200px);
  width: 100%;
  background-color: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #064e3b;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: #666666;
  margin: 0;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.spinner {
  font-size: 48px;
  color: #10b981;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 16px;
  color: #999999;
  margin-bottom: 24px;
}

.empty-state p {
  font-size: 14px;
  color: #999999;
  margin-bottom: 24px;
}

.btn-primary {
  padding: 10px 24px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  white-space: nowrap;
}

.btn-primary:hover {
  background-color: #059669;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(16, 185, 129, 0.3);
}

/* 项目网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

/* 项目卡片 */
.project-card {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.project-card:hover {
  border-color: #10b981;
  box-shadow: 0 4px 12px rgba(6, 78, 59, 0.15);
  transform: translateY(-2px);
}

.project-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.project-name {
  font-size: 16px;
  font-weight: 600;
  color: #10b981;
}

.project-description {
  font-size: 14px;
  color: #666666;
  line-height: 1.6;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.project-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tech-tag {
  padding: 4px 10px;
  background-color: rgba(16, 185, 129, 0.08);
  border-radius: 4px;
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.project-stats {
  display: flex;
  gap: 12px;
}

.stat {
  font-size: 13px;
  color: #666666;
}

/* 分页控件 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  min-width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #333333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background-color: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.page-btn:disabled {
  background-color: #f5f5f5;
  color: #cccccc;
  cursor: not-allowed;
}

.page-btn.active {
  background-color: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.page-info {
  font-size: 13px;
  color: #666666;
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 24px;
  }
  
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
