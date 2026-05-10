<script setup>
import { ref, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import UserSidebar from './UserSidebar.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'

use([PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const stats = ref(null)
const isLoading = ref(true)

const loadStats = async () => {
  try {
    const res = await request({ url: '/auth/user/statistics', method: 'get' })
    if (res.code === 200) stats.value = res.data
  } catch (e) { toast.error('加载统计数据失败') }
  finally { isLoading.value = false }
}

const cards = [
  { key: 'projects', label: '我的项目', icon: '📁' },
  { key: 'teams', label: '我的组队', icon: '👥' },
  { key: 'watches', label: '我的收藏', icon: '⭐' },
  { key: 'starsReceived', label: '获得点赞', icon: '❤️' },
  { key: 'downloads', label: '被下载', icon: '📥' },
  { key: 'comments', label: '我的评论', icon: '💬' },
  { key: 'files', label: '上传文件', icon: '📄' },
  { key: 'notifications', label: '动态数量', icon: '🔔' }
]

const projectTypeChart = ref(null)
const monthlyChart = ref(null)

const buildCharts = () => {
  if (!stats.value) return
  const c = stats.value.cards || {}
  const types = stats.value.projectTypes || []
  const monthly = stats.value.monthlyActivity || []

  projectTypeChart.value = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
      data: types.map(t => ({ name: t.type || '其他', value: t.cnt })),
      label: { formatter: '{b}: {c}' }
    }]
  }

  monthlyChart.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['创建项目', '发表评论'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: monthly.map(m => m.month?.substring(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '创建项目', type: 'line', data: monthly.map(m => m.projects || 0), smooth: true, color: '#10b981' },
      { name: '发表评论', type: 'line', data: monthly.map(m => m.comments || 0), smooth: true, color: '#3b82f6' }
    ]
  }
}

onMounted(async () => { await loadStats(); buildCharts() })
</script>

<template>
  <div class="stats-page">
    <div class="content-layout">
      <UserSidebar />
      <main class="main-content">
        <div class="page-header">
          <h2 class="page-title">📊 我的统计</h2>
        </div>

        <div v-if="isLoading" class="loading">加载中...</div>
        <template v-else-if="stats">
          <div class="cards-grid">
            <div v-for="card in cards" :key="card.key" class="stat-card">
              <span class="card-icon">{{ card.icon }}</span>
              <span class="card-value">{{ stats.cards[card.key] || 0 }}</span>
              <span class="card-label">{{ card.label }}</span>
            </div>
          </div>

          <div class="charts-row">
            <div class="chart-box">
              <h4>项目类型分布</h4>
              <v-chart v-if="projectTypeChart" :option="projectTypeChart" style="height:300px" autoresize />
              <div v-else class="chart-empty">暂无数据</div>
            </div>
            <div class="chart-box">
              <h4>近6月活跃趋势</h4>
              <v-chart v-if="monthlyChart" :option="monthlyChart" style="height:300px" autoresize />
              <div v-else class="chart-empty">暂无数据</div>
            </div>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.stats-page { width: 100%; min-height: 100vh; padding: 24px; background: #f5f7fa; }
.content-layout { display: flex; gap: 24px; max-width: 1280px; margin: 0 auto; }
.main-content { flex: 1; }
.page-title { font-size: 24px; font-weight: 700; color: #064e3b; margin: 0 0 24px 0; }
.loading { text-align: center; padding: 80px; color: #999; }
.cards-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 10px; padding: 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: all 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.card-icon { font-size: 28px; display: block; margin-bottom: 8px; }
.card-value { font-size: 28px; font-weight: 700; color: #064e3b; display: block; }
.card-label { font-size: 13px; color: #999; margin-top: 4px; display: block; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box { background: #fff; border-radius: 10px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.chart-box h4 { margin: 0 0 12px 0; font-size: 15px; color: #333; }
.chart-empty { text-align: center; padding: 80px 20px; color: #ccc; }
@media (max-width: 768px) { .cards-grid { grid-template-columns: repeat(2, 1fr); } .charts-row { grid-template-columns: 1fr; } }
</style>
