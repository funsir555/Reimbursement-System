import type { SystemSettingsBootstrapData } from '@/api'

type RegisterBootstrapCoordinator = (
  coordinator: (data: SystemSettingsBootstrapData) => void
) => void

export function useSystemSettingsBootstrapSync(params: {
  registerBootstrapCoordinator: RegisterBootstrapCoordinator
  applyEmployeesBootstrap: () => void
  applyRolesBootstrap: () => void
  applyCompaniesBootstrap: (companies?: SystemSettingsBootstrapData['companies']) => void
  applyDepartmentBootstrap: () => void
}) {
  const {
    registerBootstrapCoordinator,
    applyEmployeesBootstrap,
    applyRolesBootstrap,
    applyCompaniesBootstrap,
    applyDepartmentBootstrap
  } = params

  registerBootstrapCoordinator((data) => {
    applyEmployeesBootstrap()
    applyRolesBootstrap()
    applyCompaniesBootstrap(data.companies)
    applyDepartmentBootstrap()
  })
}
