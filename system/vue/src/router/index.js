import {createRouter, createWebHistory} from 'vue-router'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/views/Manager.vue'),
      redirect: '/home',
      children: [
        { path: 'person', component: () => import('@/views/manager/Person.vue')},
        { path: 'password', component: () => import('@/views/manager/Password.vue')},
        { path: 'home', component: () => import('@/views/manager/Home.vue')},
        { path: 'admin', component: () => import('@/views/manager/Admin.vue')},
        { path: 'user', component: () => import('@/views/manager/User.vue')},
        { path: 'recipe', component: () => import('@/views/manager/Recipe.vue')},
        { path: 'mealPlan', component: () => import('@/views/manager/MealPlan.vue')},
        { path: 'recommendation', component: () => import('@/views/manager/NutritionAnalysis.vue')},
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue')},
    { path: '/register', component: () => import('@/views/Register.vue')},
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 获取当前登录用户信息
  const user = JSON.parse(localStorage.getItem('system-user') || '{}')
  
  // 白名单路由，不需要登录就能访问
  const whiteList = ['/login', '/register']
  
  // 如果不是白名单路由且用户未登录，则重定向到登录页
  if (!whiteList.includes(to.path) && !user.id) {
    ElMessage.error('请先登录')
    next('/login')
  } else {
    next()
  }
})

export default router
