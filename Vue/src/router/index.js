import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/components/Login.vue'
import Register from '@/components/Register.vue'
import Main from '@/components/Main.vue'
import UserRepository from '@/components/UserRepository.vue'
import CreateProject from '@/components/CreateProject.vue'
import ProjectSquare from '@/components/ProjectSquare.vue'
// import TeamSquare from '@/components/TeamSquare.vue'  // 暂时注释，后续开发
import ProjectDetail from '@/components/ProjectDetail.vue'
import Settings from '@/components/Settings.vue'
import { log } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const routes = [
  // 登录页面
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录 - 北京工业大学项目协作平台' }
  },
  
  // 注册页面
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册 - 北京工业大学项目协作平台' }
  },
  
  // 主页
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: Main,
    meta: { 
      title: '主页 - 北京工业大学项目协作平台',
      requiresAuth: true 
    }
  },
  
  // 我的仓库
  {
    path: '/repository',
    name: 'Repository',
    component: UserRepository,
    meta: { 
      title: '我的仓库 - 北京工业大学项目协作平台',
      requiresAuth: true 
    }
  },
  
  // 创建项目
  {
    path: '/create-project',
    name: 'CreateProject',
    component: CreateProject,
    meta: { 
      title: '创建项目 - 北京工业大学项目协作平台',
      requiresAuth: true 
    }
  },
  
  // 项目广场
  {
    path: '/projects',
    name: 'Projects',
    component: ProjectSquare,
    meta: { 
      title: '项目广场 - 北京工业大学项目协作平台',
      requiresAuth: true 
    }
  },
  
  // 项目详情
  {
    path: '/project/:id',
    name: 'ProjectDetail',
    component: ProjectDetail,
    meta: { 
      title: '项目详情 - 北京工业大学项目协作平台',
      requiresAuth: true 
    },
    props: true // 允许将路由参数作为 props 传递
  },
  
  // 组队广场（暂时注释，后续开发）
  // {
  //   path: '/team',
  //   name: 'Team',
  //   component: TeamSquare,
  //   meta: { 
  //     title: '组队广场 - 北京工业大学项目协作平台',
  //     requiresAuth: true 
  //   }
  // },
  
  // 设置页面
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { 
      title: '设置 - 北京工业大学项目协作平台',
      requiresAuth: true 
    }
  },
  
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 页面切换时滚动到顶部
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局前置守卫
// TODO: 添加路由级别的权限控制（根据用户角色）
// TODO: 添加路由访问日志，便于审计
router.beforeEach((to, from) => {
  // 设置页面标题
  document.title = to.meta.title || '北京工业大学项目协作平台'
  
  // 检查是否需要登录
  const isLoggedIn = tokenManager.isLoggedIn()
  
  if (to.meta.requiresAuth && !isLoggedIn) {
    // TODO: 保存用户原始访问路径，登录后自动跳转回去
    // 需要登录但未登录，跳转到登录页
    return {
      path: '/login',
      query: { redirect: to.fullPath } // 保存目标路径，登录后可以跳转回去
    }
  } else if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    // 已登录用户访问登录/注册页，跳转到主页
    return '/home'
  }
  // 默认允许导航
})

// 全局后置钩子
router.afterEach((to, from) => {
  // 可以在这里添加页面访问统计、埋点等
  log(`页面切换: ${from.path} -> ${to.path}`)
})

export default router
