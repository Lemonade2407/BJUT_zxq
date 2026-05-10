<script setup>
import { ref, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'

use([PieChart, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const stats = ref(null)
const isLoading = ref(true)

const loadStats = async () => {
  try {
    const res = await request({ url: '/admin/statistics', method: 'get' })
    if (res.code === 200) stats.value = res.data
  } catch (e) { toast.error('加载统计数据失败') }
  finally { isLoading.value = false }
}

const overviewCards = [
  { key: 'users', label: '用户总数', icon: '👤' },
  { key: 'projects', label: '项目总数', icon: '📁' },
  { key: 'teams', label: '组队总数', icon: '👥' },
  { key: 'comments', label: '评论总数', icon: '💬' },
  { key: 'tags', label: '标签总数', icon: '🏷️' },
  { key: 'courses', label: '课程总数', icon: '📚' },
  { key: 'files', label: '文件总数', icon: '📄' },
  { key: 'pendingApps', label: '待审核申请', icon: '⏳' }
]

const userRoleChart = ref(null)
const projectTypeChart = ref(null)
const monthlyTrendChart = ref(null)
const topTagsChart = ref(null)

const buildCharts = () => {
  if (!stats.value) return
  const c = stats.value.cards || {}

  const roles = stats.value.userRoles || []
  userRoleChart.value = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: '65%', center: ['50%', '50%'],
      data: roles.map(r => ({
        name: r.role === 'USER' ? '学生' : r.role === 'TEACHER' ? '教师' : '管理员',
        value: r.cnt
      })),
      label: { formatter: '{b}: {c}' }
    }]
  }

  const types = stats.value.projectTypes || []
  projectTypeChart.value = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: '65%', center: ['50%', '50%'],
      data: types.map(t => ({ name: t.type || '其他', value: t.cnt })),
      label: { formatter: '{b}: {c}' }
    }]
  }

  const monthly = stats.value.monthlyTrend || []
  monthlyTrendChart.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '新增项目', '新增评论'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: monthly.map(m => m.month?.substring(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '新增用户', type: 'line', data: monthly.map(m => m.users || 0), smooth: true, color: '#10b981' },
      { name: '新增项目', type: 'line', data: monthly.map(m => m.projects || 0), smooth: true, color: '#3b82f6' },
      { name: '新增评论', type: 'line', data: monthly.map(m => m.comments || 0), smooth: true, color: '#f59e0b' }
    ]
  }

  const tags = stats.value.topTags || []
  topTagsChart.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: tags.map(t => t.name), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '使用次数' },
    series: [{ type: 'bar', data: tags.map(t => t.usageCount || 0), color: '#10b981', barMaxWidth: 40 }]
  }
}

onMounted(async () => { await loadStats(); buildCharts() })
</script>

<template>
  <div class="management-container">
    <h2 class="section-title">📊 数据概览</h2>

    <div v-if="isLoading" class="loading">加载中...</div>
    <template v-else-if="stats">
      <div class="cards-grid">
        <div v-for="card in overviewCards" :key="card.key" class="stat-card">
          <span class="card-icon">{{ card.icon }}</span>
          <span class="card-value">{{ stats.cards?.[card.key] || 0 }}</span>
          <span class="card-label">{{ card.label }}</span>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-box"><h4>用户角色分布</h4><v-chart v-if="userRoleChart" :option="userRoleChart" style="height:260px" autoresize /></div>
        <div class="chart-box"><h4>项目类型分布</h4><v-chart v-if="projectTypeChart" :option="projectTypeChart" style="height:260px" autoresize /></div>
      </div>
      <div class="chart-box" style="margin-top:16px"><h4>近6月新增趋势</h4><v-chart v-if="monthlyTrendChart" :option="monthlyTrendChart" style="height:300px" autoresize /></div>
      <div class="chart-box" style="margin-top:16px"><h4>热门标签 Top 10</h4><v-chart v-if="topTagsChart" :option="topTagsChart" style="height:280px" autoresize /></div>
    </template>
  </div>
</template>

<style scoped>
.management-container { width: 100%; max-width: 1400px; background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; padding: 24px; }
.section-title { font-size: 24px; font-weight: 600; color: #064e3b; margin: 0 0 24px 0; }
.loading { text-align: center; padding: 80px; color: #999; }
.cards-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #f9fafb; border-radius: 10px; padding: 20px; text-align: center; border: 1px solid #f0f0f0; transition: all 0.2s; }
.stat-card:hover { border-color: #10b981; transform: translateY(-1px); }
.card-icon { font-size: 24px; display: block; margin-bottom: 6px; }
.card-value { font-size: 26px; font-weight: 700; color: #064e3b; display: block; }
.card-label { font-size: 12px; color: #999; margin-top: 2px; display: block; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box { background: #fff; border-radius: 10px; padding: 20px; border: 1px solid #f0f0f0; }
.chart-box h4 { margin: 0 0 12px 0; font-size: 15px; color: #333; }
</style>
