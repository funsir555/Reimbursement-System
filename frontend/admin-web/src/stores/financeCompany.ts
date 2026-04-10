import { defineStore } from 'pinia'
import { financeContextApi, type FinanceCompanyOption } from '@/api'

type FinanceCompanySwitchGuard = () => boolean | Promise<boolean>

const STORAGE_KEY = 'finance-current-company-id'
const switchGuards = new Map<string, FinanceCompanySwitchGuard>()
let initPromise: Promise<void> | null = null

function normalizeText(value?: string | null) {
  const text = String(value || '').trim()
  return text || ''
}

export const useFinanceCompanyStore = defineStore('financeCompany', {
  state: () => ({
    initialized: false,
    loading: false,
    switching: false,
    companyOptions: [] as FinanceCompanyOption[],
    currentCompanyId: '',
    currentUserCompanyId: '',
    defaultCompanyId: ''
  }),
  getters: {
    currentCompanyOption: (state) => state.companyOptions.find((item) => item.companyId === state.currentCompanyId),
    currentCompanyName: (state) =>
      state.companyOptions.find((item) => item.companyId === state.currentCompanyId)?.companyName || '',
    currentCompanyLabel: (state) =>
      state.companyOptions.find((item) => item.companyId === state.currentCompanyId)?.label || '',
    currentCompanyHasActiveAccountSet: (state) =>
      Boolean(state.companyOptions.find((item) => item.companyId === state.currentCompanyId)?.hasActiveAccountSet),
    hasCompany: (state) => Boolean(state.currentCompanyId)
  },
  actions: {
    registerSwitchGuard(key: string, guard: FinanceCompanySwitchGuard) {
      switchGuards.set(key, guard)
    },
    unregisterSwitchGuard(key: string) {
      switchGuards.delete(key)
    },
    async ensureInitialized(preferredCompanyId?: string) {
      if (this.initialized) {
        if (!this.currentCompanyId) {
          this.applyCurrentCompany(this.resolveInitialCompanyId(preferredCompanyId))
        }
        return
      }

      if (initPromise) {
        await initPromise
        return
      }

      this.loading = true
      initPromise = (async () => {
        const res = await financeContextApi.getMeta()
        this.companyOptions = res.data.companyOptions || []
        this.currentUserCompanyId = normalizeText(res.data.currentUserCompanyId)
        this.defaultCompanyId = normalizeText(res.data.defaultCompanyId)
        this.applyCurrentCompany(this.resolveInitialCompanyId(preferredCompanyId))
        this.initialized = true
      })()

      try {
        await initPromise
      } finally {
        initPromise = null
        this.loading = false
      }
    },
    async switchCompany(nextCompanyId: string) {
      const normalizedCompanyId = normalizeText(nextCompanyId)
      if (!normalizedCompanyId || normalizedCompanyId === this.currentCompanyId) {
        return true
      }
      if (!this.companyOptions.some((item) => item.companyId === normalizedCompanyId)) {
        return false
      }

      this.switching = true
      try {
        for (const guard of switchGuards.values()) {
          const allowed = await guard()
          if (allowed === false) {
            return false
          }
        }
        this.applyCurrentCompany(normalizedCompanyId)
        return true
      } finally {
        this.switching = false
      }
    },
    reset() {
      this.initialized = false
      this.loading = false
      this.switching = false
      this.companyOptions = []
      this.currentCompanyId = ''
      this.currentUserCompanyId = ''
      this.defaultCompanyId = ''
      localStorage.removeItem(STORAGE_KEY)
      switchGuards.clear()
    },
    resolveInitialCompanyId(preferredCompanyId?: string) {
      const storedCompanyId = normalizeText(localStorage.getItem(STORAGE_KEY))
      const storedOption = this.findCompanyOption(storedCompanyId)
      if (storedOption?.hasActiveAccountSet) {
        return storedOption.companyId
      }

      const candidates = [
        this.defaultCompanyId,
        this.currentUserCompanyId,
        normalizeText(preferredCompanyId),
        storedOption?.companyId || '',
        this.companyOptions.find((item) => item.hasActiveAccountSet)?.companyId || '',
        this.companyOptions[0]?.companyId || ''
      ]
      return candidates.find((item) => item && this.findCompanyOption(item)) || ''
    },
    findCompanyOption(companyId?: string) {
      const normalizedCompanyId = normalizeText(companyId)
      if (!normalizedCompanyId) {
        return undefined
      }
      return this.companyOptions.find((item) => item.companyId === normalizedCompanyId)
    },
    applyCurrentCompany(companyId: string) {
      this.currentCompanyId = companyId
      if (companyId) {
        localStorage.setItem(STORAGE_KEY, companyId)
      } else {
        localStorage.removeItem(STORAGE_KEY)
      }
    }
  }
})
