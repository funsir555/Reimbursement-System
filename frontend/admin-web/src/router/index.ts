import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'expense/list',
          name: 'expense-list',
          component: () => import('../views/expense/ExpenseListView.vue'),
          meta: { title: '我的报销' }
        },
        {
          path: 'invoice/list',
          name: 'invoice-list',
          component: () => import('../views/invoice/InvoiceListView.vue'),
          meta: { title: '发票库' }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 这里可以添加登录验证逻辑
  if (!to.meta.public) {
    // 检查登录状态
    // const token = localStorage.getItem('token')
    // if (!token) return next('/login')
  }
  next()
})

export default router
