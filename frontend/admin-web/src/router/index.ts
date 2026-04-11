import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'
import { authApi } from '@/api'
import { hasAnyPermission, readStoredUser, resolveFirstAccessiblePath } from '@/utils/permissions'
import { buildRouteRecords } from './route-catalog'
import { getRoutePermissionCodes } from './route-meta'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: buildRouteRecords()
})

async function ensureCurrentUser() {
  const cachedUser = readStoredUser()
  if (cachedUser?.permissionCodes?.length) {
    return cachedUser
  }

  const res = await authApi.getCurrentUser()
  localStorage.setItem('user', JSON.stringify(res.data))
  return res.data
}

router.beforeEach(async (to) => {
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
    const currentUser = await ensureCurrentUser()
    if (hasAnyPermission(requiredCodes, currentUser)) {
      return true
    }

    const fallbackPath = resolveFirstAccessiblePath(currentUser)
    if (fallbackPath !== to.fullPath) {
      ElMessage.warning('当前没有该页面访问权限，已跳转到可访问的首页。')
      return fallbackPath
    }
    return true
  } catch {
    return '/login'
  }
})

export default router
