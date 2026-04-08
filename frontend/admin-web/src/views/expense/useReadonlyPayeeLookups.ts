import { ref } from 'vue'
import {
  expenseCreateApi,
  type ExpenseCreatePayeeAccountOption,
  type ExpenseCreatePayeeOption,
  type ExpenseCreateVendorOption,
  type ProcessFormDesignBlock,
  type ProcessFormDesignSchema
} from '@/api'
import { getBusinessComponentDefinition } from '@/views/process/formDesignerHelper'

export function useReadonlyPayeeLookups() {
  const vendorOptionMap = ref<Record<string, ExpenseCreateVendorOption>>({})
  const payeeOptionMap = ref<Record<string, ExpenseCreatePayeeOption>>({})
  const payeeAccountOptionMap = ref<Record<string, ExpenseCreatePayeeAccountOption>>({})
  let syncVersion = 0

  async function syncReadonlyPayeeLookups(schema?: ProcessFormDesignSchema | null) {
    const currentVersion = ++syncVersion
    vendorOptionMap.value = {}
    payeeOptionMap.value = {}
    payeeAccountOptionMap.value = {}

    if (!schema) {
      return
    }

    const lookups = await loadReadonlyPayeeLookups(schema)
    if (currentVersion !== syncVersion) {
      return
    }

    vendorOptionMap.value = lookups.vendorOptionMap
    payeeOptionMap.value = lookups.payeeOptionMap
    payeeAccountOptionMap.value = lookups.payeeAccountOptionMap
  }

  return {
    vendorOptionMap,
    payeeOptionMap,
    payeeAccountOptionMap,
    syncReadonlyPayeeLookups
  }
}

async function loadReadonlyPayeeLookups(schema: ProcessFormDesignSchema) {
  const needsVendor = hasBusinessComponent(schema, 'counterparty')
  const needsPayee = hasBusinessComponent(schema, 'payee')
  const needsPayeeAccount = hasBusinessComponent(schema, 'payee-account')

  if (!needsVendor && !needsPayee && !needsPayeeAccount) {
    return {
      vendorOptionMap: {},
      payeeOptionMap: {},
      payeeAccountOptionMap: {}
    }
  }

  const [vendorResult, payeeResult, payeeAccountResult] = await Promise.allSettled([
    needsVendor
      ? expenseCreateApi.listVendorOptions('', true)
      : Promise.resolve({ data: [] as ExpenseCreateVendorOption[] }),
    needsPayee
      ? expenseCreateApi.listPayeeOptions({ keyword: '' })
      : Promise.resolve({ data: [] as ExpenseCreatePayeeOption[] }),
    needsPayeeAccount
      ? expenseCreateApi.listPayeeAccountOptions({ keyword: '' })
      : Promise.resolve({ data: [] as ExpenseCreatePayeeAccountOption[] })
  ])

  if (vendorResult.status === 'rejected') {
    console.warn('Failed to load readonly vendor options', vendorResult.reason)
  }
  if (payeeResult.status === 'rejected') {
    console.warn('Failed to load readonly payee options', payeeResult.reason)
  }
  if (payeeAccountResult.status === 'rejected') {
    console.warn('Failed to load readonly payee account options', payeeAccountResult.reason)
  }

  return {
    vendorOptionMap: vendorResult.status === 'fulfilled' ? buildOptionMap(vendorResult.value.data) : {},
    payeeOptionMap: payeeResult.status === 'fulfilled' ? buildOptionMap(payeeResult.value.data) : {},
    payeeAccountOptionMap: payeeAccountResult.status === 'fulfilled'
      ? buildOptionMap(payeeAccountResult.value.data)
      : {}
  }
}

function hasBusinessComponent(schema: ProcessFormDesignSchema, code: string) {
  return Array.isArray(schema?.blocks) && schema.blocks.some((block) => resolveBusinessCode(block) === code)
}

function resolveBusinessCode(block: ProcessFormDesignBlock) {
  return getBusinessComponentDefinition(String(block?.props?.componentCode || ''))?.code
    || String(block?.props?.componentCode || '')
}

function buildOptionMap<T extends { value: string }>(items: T[]) {
  return items.reduce<Record<string, T>>((result, item) => {
    if (item.value) {
      result[item.value] = item
    }
    return result
  }, {})
}
