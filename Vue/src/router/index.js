import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/components/auth/Login.vue'
import Register from '@/components/auth/Register.vue'
import Main from '@/components/home/Main.vue'
import NotFound from '@/components/layout/NotFound.vue'
import { log } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'
import { toast } from '@/utils/toast'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },

  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册' }
  },

  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: Main,
    meta: { title: '主页', requiresAuth: true }
  },

  {
    path: '/repository',
    name: 'Repository',
    component: () => import('@/components/user/UserRepository.vue'),
    meta: { title: '我的仓库', requiresAuth: true }
  },

  {
    path: '/create-project',
    name: 'CreateProject',
    component: () => import('@/components/project/CreateProject.vue'),
    meta: { title: '创建项目', requiresAuth: true }
  },

  {
    path: '/projects',
    name: 'Projects',
    component: () => import('@/components/project/ProjectSquare.vue'),
    meta: { title: '项目广场', requiresAuth: true }
  },

  {
    path: '/project/:id',
    name: 'ProjectDetail',
    component: () => import('@/components/project/ProjectDetail.vue'),
    meta: { title: '项目详情', requiresAuth: true },
    props: true
  },

  {
    path: '/team',
    name: 'Team',
    component: () => import('@/components/team/TeamSquare.vue'),
    meta: { title: '组队广场', requiresAuth: true }
  },

  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/components/user/Settings.vue'),
    meta: { title: '设置', requiresAuth: true }
  },

  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/components/user/Favorites.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },

  {
    path: '/my-stats',
    name: 'MyStats',
    component: () => import('@/components/user/MyStats.vue'),
    meta: { title: '我的统计', requiresAuth: true }
  },

  {
    path: '/my-teams',
    name: 'MyTeams',
    component: () => import('@/components/team/MyTeams.vue'),
    meta: { title: '我的组队', requiresAuth: true }
  },

  {
    path: '/search',
    name: 'SearchResult',
    component: () => import('@/components/search/SearchResult.vue'),
    meta: { title: '搜索结果', requiresAuth: true }
  },

  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/components/user/UserProfile.vue'),
    meta: { title: '个人主页', requiresAuth: true }
  },

  {
    path: '/class-management',
    name: 'ClassManagement',
    component: () => import('@/components/user/ClassManagement.vue'),
    meta: { title: '教学班级管理', requiresAuth: true, requiresTeacher: true }
  },

  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/components/admin/AdminDashboard.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  document.title = to.meta?.title || '项目协作平台'

  const isLoggedIn = tokenManager.isLoggedIn()

  if (to.meta?.requiresAuth && !isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta?.requiresAdmin) {
    const userInfo = tokenManager.getUserInfo()
    if (!userInfo || userInfo.role !== 'ADMIN') {
      toast.error('无权访问该页面')
      return '/home'
    }
  }

  if (to.meta?.requiresTeacher) {
    const userInfo = tokenManager.getUserInfo()
    if (!userInfo || (userInfo.role !== 'TEACHER' && userInfo.role !== 'ADMIN')) {
      toast.error('无权访问该页面')
      return '/home'
    }
  }

  if (to.path === '/login' && isLoggedIn) {
    return '/home'
  }
})

router.afterEach((to, from) => {
  if (from) {
    log(`页面切换: ${from.path} -> ${to.path}`)
  }
})

export default router
