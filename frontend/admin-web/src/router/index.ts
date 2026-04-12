import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'
import { authApi } from '@/api'
import { hasAnyPermission, readStoredUser, resolveFirstAccessiblePath } from '@/utils/permissions'
import { buildRouteRecords } from './route-catalog'
import { getRoutePermissionCodes } from './route-meta'

// 前端路由总入口：负责装配路由表，并统一处理登录拦截与权限拦截。
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: buildRouteRecords()
})

async function ensureCurrentUser() {
  // 受保护页面依赖权限码；优先复用本地缓存，缺失时再向后端补拉当前用户信息。
  const cachedUser = readStoredUser()
  if (cachedUser?.permissionCodes?.length) {
    return cachedUser
  }

  const res = await authApi.getCurrentUser()
  localStorage.setItem('user', JSON.stringify(res.data))
  return res.data
}

router.beforeEach(async (to) => {
  // 公开页面（例如登录页）不需要 token，也不做权限判断。
  if (to.meta.public) {
    return true
  }

  const token = localStorage.getItem('token')
  if (!token) {
    return '/login'
  }

  const requiredCodes = getRoutePermissionCodes(to)
  if (!requiredCodes.length) {
    return true
  }

  try {
    // 页面权限判断依赖后端返回的 permissionCodes。
    const currentUser = await ensureCurrentUser()
    if (hasAnyPermission(requiredCodes, currentUser)) {
      return true
    }

    // 目标页无权访问时，跳转到该用户当前第一个可访问页面。
    const fallbackPath = resolveFirstAccessiblePath(currentUser)
    if (fallbackPath !== to.fullPath) {
      ElMessage.warning('当前没有该页面访问权限，已跳转到可访问的首页。')
      return fallbackPath
    }
    return true
  } catch {
    // 当前用户信息加载失败时，通常说明登录态不可用，统一退回登录页。
    return '/login'
  }
})

export default router
