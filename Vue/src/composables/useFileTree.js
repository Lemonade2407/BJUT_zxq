import { ref, computed } from 'vue'

export function useFileTree(selectedFiles) {
  const expandedFolders = ref(new Set())

  const organizedFiles = computed(() => {
    const fileTree = {}
    selectedFiles.value.forEach((file, index) => {
      const path = file.relativePath || file.webkitRelativePath || file.name
      const parts = path.split('/')
      if (parts.length === 1) {
        if (!fileTree['__root__']) {
          fileTree['__root__'] = { type: 'folder', name: '根目录', children: [] }
        }
        fileTree['__root__'].children.push({
          type: 'file', fileIndex: index, name: file.name, size: file.size
        })
      } else {
        let currentLevel = fileTree
        parts.forEach((part, idx) => {
          if (idx === parts.length - 1) {
            if (!currentLevel['__files__']) currentLevel['__files__'] = []
            currentLevel['__files__'].push({ type: 'file', fileIndex: index, name: part, size: file.size })
          } else {
            if (!currentLevel[part]) currentLevel[part] = { type: 'folder', name: part, children: {} }
            currentLevel = currentLevel[part].children
          }
        })
      }
    })
    return fileTree
  })

  function renderFileTree(tree, level = 0, parentKey = '') {
    const result = []
    if (tree['__files__']) {
      tree['__files__'].forEach(file => result.push({ ...file, level }))
    }
    Object.keys(tree).forEach(key => {
      if (key !== '__files__' && key !== '__root__') {
        const folder = tree[key]
        const folderKey = parentKey ? `${parentKey}/${key}` : key
        const isExpanded = expandedFolders.value.has(folderKey)
        result.push({ type: 'folder', name: folder.name, level, isFolder: true, folderKey, isExpanded })
        if (isExpanded) result.push(...renderFileTree(folder.children, level + 1, folderKey))
      }
    })
    if (tree['__root__']) {
      tree['__root__'].children.forEach(file => result.push({ ...file, level }))
    }
    return result
  }

  const displayFiles = computed(() => renderFileTree(organizedFiles.value))

  function toggleFolder(folderKey) {
    const newSet = new Set(expandedFolders.value)
    if (newSet.has(folderKey)) newSet.delete(folderKey)
    else newSet.add(folderKey)
    expandedFolders.value = newSet
  }

  return { organizedFiles, displayFiles, expandedFolders, toggleFolder }
}
