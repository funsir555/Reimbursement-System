import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { hasAnyPermission, hasPermission } from '@/utils/permissions'
import {
  systemSettingsApi,
  type PermissionTreeNode,
  type RoleRecord,
  type SyncConnectorConfig,
  type SyncJobRecord,
  type SystemSettingsBootstrapData
} from '@/api'
import { isWecomConnector, resolveConnectorPlatformName } from '../systemSettingsShared'

type BootstrapCoordinator = (data: SystemSettingsBootstrapData) => void

export function useSystemSettingsPageOrchestration() {
  const route = useRoute()
  const router = useRouter()

  const loading = ref(false)
  const bootstrapError = ref(false)
  const activeTab = ref('organization')
  const departments = ref<SystemSettingsBootstrapData['departments']>([])
  const employees = ref<SystemSettingsBootstrapData['employees']>([])
  const roles = ref<RoleRecord[]>([])
  const permissions = ref<PermissionTreeNode[]>([])
  const companies = ref<SystemSettingsBootstrapData['companies']>([])
  const companyBankAccounts = ref<SystemSettingsBootstrapData['companyBankAccounts']>([])
  const connectors = ref<SyncConnectorConfig[]>([])
  const jobs = ref<SyncJobRecord[]>([])
  const ocrProviders = ref<SystemSettingsBootstrapData['ocrProviders']>([])
  const currentUser = ref<SystemSettingsBootstrapData['currentUser'] | null>(null)
  const bootstrapCoordinator = ref<BootstrapCoordinator>()

  function can(code: string) {
    const codes = currentUser.value?.permissionCodes || []
    if (code.endsWith(':view')) {
      return hasAnyPermission(['settings:menu', code], codes)
    }
    return hasPermission(code, codes)
  }

  const visibleTabs = computed(() =>
    [
      can('settings:organization:view') ? 'organization' : null,
      can('settings:employees:view') ? 'employees' : null,
      can('settings:roles:view') ? 'roles' : null,
      can('settings:companies:view') ? 'companies' : null,
      can('settings:company_accounts:view') ? 'companyAccounts' : null,
      can('settings:api_interfaces:view') ? 'apiInterfaces' : null
    ].filter((item): item is string => !!item)
  )

  watch(activeTab, (value) => {
    router.replace({ query: { ...route.query, tab: value } })
  })

  watch(
    () => route.query.tab,
    () => {
      syncTabFromRoute()
    }
  )

  function registerBootstrapCoordinator(coordinator: BootstrapCoordinator) {
    bootstrapCoordinator.value = coordinator
  }

  function syncTabFromRoute() {
    const queryTab = String(route.query.tab || '')
    activeTab.value = visibleTabs.value.includes(queryTab)
      ? queryTab
      : String(visibleTabs.value[0] || 'organization')
  }

  async function loadBootstrap() {
    loading.value = true
    bootstrapError.value = false
    try {
      const res = await systemSettingsApi.getBootstrap()
      const data = res.data
      currentUser.value = data.currentUser
      departments.value = data.departments
      employees.value = data.employees
      roles.value = data.roles
      permissions.value = data.permissions
      companies.value = data.companies
      companyBankAccounts.value = data.companyBankAccounts
      connectors.value = data.connectors
      jobs.value = data.jobs
      ocrProviders.value = data.ocrProviders || []
      localStorage.setItem('user', JSON.stringify(data.currentUser))

      bootstrapCoordinator.value?.(data)
      syncTabFromRoute()
    } catch (error: any) {
      bootstrapError.value = true
      ElMessage.error(error?.message || '系统设置初始化失败，请联系管理员或查看后端日志')
    } finally {
      loading.value = false
    }
  }

  async function saveConnector(connector: SyncConnectorConfig) {
    const wecom = isWecomConnector(connector)
    await systemSettingsApi.updateSyncConnector({
      platformCode: connector.platformCode,
      enabled: connector.enabled ? 1 : 0,
      autoSyncEnabled: connector.autoSyncEnabled ? 1 : 0,
      syncIntervalMinutes: connector.syncIntervalMinutes,
      appKey: wecom ? undefined : connector.appKey,
      appSecret: connector.appSecret,
      appId: wecom ? undefined : connector.appId,
      corpId: connector.corpId,
      agentId: connector.agentId
    })
    ElMessage.success(`${resolveConnectorPlatformName(connector)} 配置已保存`)
    await loadBootstrap()
  }

  async function runConnectorSync(platformCode: string) {
    await systemSettingsApi.runSync([platformCode])
    ElMessage.success('同步任务已执行')
    await loadBootstrap()
  }

  onMounted(loadBootstrap)

  return {
    loading,
    bootstrapError,
    activeTab,
    departments,
    employees,
    roles,
    permissions,
    companies,
    companyBankAccounts,
    connectors,
    jobs,
    ocrProviders,
    currentUser,
    visibleTabs,
    can,
    registerBootstrapCoordinator,
    loadBootstrap,
    saveConnector,
    runConnectorSync
  }
}
