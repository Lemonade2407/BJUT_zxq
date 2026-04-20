<template>
  <li class="tree-node">
    <div 
      :class="['file-item', { 
        'is-directory': file.isDir, 
        'has-children': file.children && file.children.length > 0,
        'is-expanded': expandedFolders.has(file.id)
      }]"
      :style="{ paddingLeft: (file.level * 24 + 16) + 'px' }"
      @click="file.isDir ? $emit('toggle', file.id) : null"
    >
      <!-- 展开/折叠图标 -->
      <span v-if="file.isDir && file.children && file.children.length > 0" class="expand-icon">
        {{ expandedFolders.has(file.id) ? '▼' : '▶' }}
      </span>
      <span v-else-if="file.isDir" class="expand-icon-placeholder"></span>
      
      <!-- 文件/文件夹图标 -->
      <span class="file-icon">
        {{ getFileIcon() }}
      </span>
      
      <!-- 文件名 -->
      <span class="file-name">{{ file.fileName }}</span>
      
      <!-- 文件信息 -->
      <div class="file-info">
        <span v-if="!file.isDir && file.fileSize" class="file-size">{{ formatFileSize(file.fileSize) }}</span>
        <span v-if="file.updatedAt" class="file-time">{{ formatDate(file.updatedAt) }}</span>
      </div>
    </div>
    
    <!-- 递归渲染子节点 -->
    <transition name="slide-fade">
      <ul v-if="file.isDir && file.children && file.children.length > 0 && expandedFolders.has(file.id)" class="file-list">
        <FileTreeItem 
          v-for="child in file.children" 
          :key="child.id"
          :file="child"
          :expanded-folders="expandedFolders"
          @toggle="$emit('toggle', $event)"
        />
      </ul>
    </transition>
  </li>
</template>

<script setup>
import { onMounted } from 'vue'

const props = defineProps({
  file: Object,
  expandedFolders: Set
})

const emit = defineEmits(['toggle'])

// 根据文件类型返回对应图标
const getFileIcon = () => {
  if (props.file.isDir) {
    return '📁'
  }
  
  const ext = props.file.fileType?.toLowerCase()
  const iconMap = {
    // 代码文件
    'js': '📜', 'ts': '📘', 'jsx': '⚛️', 'tsx': '⚛️',
    'java': '☕', 'py': '🐍', 'cpp': '⚙️', 'c': '⚙️', 'h': '⚙️',
    'html': '🌐', 'css': '🎨', 'scss': '🎨', 'less': '🎨',
    'json': '📋', 'xml': '📋', 'yaml': '📋', 'yml': '📋',
    'md': '📝', 'txt': '📄', 'log': '📋',
    // 图片文件
    'png': '🖼️', 'jpg': '🖼️', 'jpeg': '🖼️', 'gif': '🖼️', 'svg': '🖼️', 'webp': '🖼️',
    // 文档文件
    'pdf': '📕', 'doc': '📘', 'docx': '📘', 'xls': '📗', 'xlsx': '📗', 'ppt': '📙', 'pptx': '📙',
    // 压缩文件
    'zip': '📦', 'rar': '📦', '7z': '📦', 'tar': '📦', 'gz': '📦',
    // 其他
    'exe': '⚡', 'dll': '⚙️', 'so': '⚙️',
  }
  
  return iconMap[ext] || '📄'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  // 小于1小时显示相对时间
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return `${minutes}分钟前`
  }
  // 小于24小时
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000)
    return `${hours}小时前`
  }
  // 小于7天
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000)
    return `${days}天前`
  }
  
  // 否则显示日期
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  })
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}
</script>

<style scoped>
.tree-node {
  list-style: none;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: #2c3e50;
  border-radius: 6px;
  margin: 2px 8px;
  cursor: default;
  transition: all 0.2s ease;
  position: relative;
}

/* 文件夹样式 */
.file-item.is-directory {
  cursor: pointer;
  user-select: none;
  font-weight: 500;
}

.file-item.is-directory:hover {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  transform: translateX(2px);
}

.file-item.is-directory:active {
  transform: translateX(1px);
}

/* 文件样式 */
.file-item:not(.is-directory):hover {
  background: linear-gradient(135deg, #f093fb10 0%, #f5576c10 100%);
  transform: translateX(2px);
}

/* 展开状态 */
.file-item.is-expanded {
  background: linear-gradient(135deg, #667eea20 0%, #764ba220 100%);
  border-left: 3px solid #667eea;
}

.expand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  font-size: 10px;
  color: #999;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.expand-icon-placeholder {
  width: 16px;
  flex-shrink: 0;
}

.file-icon {
  font-size: 18px;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.1));
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.file-size {
  font-size: 12px;
  color: #999;
  font-weight: 400;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
}

.file-time {
  font-size: 12px;
  color: #bbb;
  font-weight: 400;
}

/* 滑动淡入动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
  max-height: 1000px;
  opacity: 1;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
}

/* 子列表样式 */
.file-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
</style>
