<script setup>
import { ref, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import { log } from '@/utils/logger'

// 统计数据
const stats = ref({
  totalProjects: 0,
  totalStars: 0,
  totalDownloads: 0,
  totalViews: 0
})

// 加载数据
const loadData = () => {
  log('加载项目数据分析')
  // TODO: 调用后端 API 获取真实统计数据
  // TODO: 添加数据可视化图表（使用 ECharts 或 Chart.js）
  // TODO: 添加数据刷新功能
  stats.value = {
    totalProjects: 5,
    totalStars: 23,
    totalDownloads: 156,
    totalViews: 1024
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="analytics-container">
    <h1 class="page-title">项目数据分析</h1>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalProjects }}</div>
          <div class="stat-label">我的项目</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon">⭐</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalStars }}</div>
          <div class="stat-label">获得点赞</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon">📥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalDownloads }}</div>
          <div class="stat-label">下载次数</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon">👁️</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalViews }}</div>
          <div class="stat-label">浏览次数</div>
        </div>
      </div>
    </div>

    <!-- 图表区域（预留） -->
    <div class="charts-section">
      <div class="chart-placeholder">
        <p>📊 数据统计图表功能开发中...</p>
        <p class="hint">后续将展示项目趋势、热门标签等可视化数据</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analytics-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 24px 0;
  font-weight: 600;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  font-size: 40px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 12px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #064e3b;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

/* 图表区域 */
.charts-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-placeholder {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.chart-placeholder p {
  margin: 8px 0;
  font-size: 16px;
}

.chart-placeholder .hint {
  font-size: 14px;
  color: #bbb;
}

@media (max-width: 768px) {
  .analytics-container {
    padding: 20px 10px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stat-card {
    padding: 20px;
  }
  
  .stat-icon {
    width: 50px;
    height: 50px;
    font-size: 32px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>
