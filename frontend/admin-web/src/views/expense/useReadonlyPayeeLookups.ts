import { ref } from 'vue'
import {
  expenseCreateApi,
  type ExpenseCreatePayeeAccountOption,
  type ExpenseCreatePayeeOption,
  type ProcessFormDesignBlock,
  type ProcessFormDesignSchema
} from '@/api'
import { getBusinessComponentDefinition } from '@/views/process/formDesignerHelper'

export function useReadonlyPayeeLookups() {
  const payeeOptionMap = ref<Record<string, ExpenseCreatePayeeOption>>({})
  const payeeAccountOptionMap = ref<Record<string, ExpenseCreatePayeeAccountOption>>({})
  let syncVersion = 0

  async function syncReadonlyPayeeLookups(schema?: ProcessFormDesignSchema | null) {
    const currentVersion = ++syncVersion
    payeeOptionMap.value = {}
    payeeAccountOptionMap.value = {}

    if (!schema) {
      return
    }

    const lookups = await loadReadonlyPayeeLookups(schema)
    if (currentVersion !== syncVersion) {
      return
    }

    payeeOptionMap.value = lookups.payeeOptionMap
    payeeAccountOptionMap.value = lookups.payeeAccountOptionMap
  }

  return {
    payeeOptionMap,
    payeeAccountOptionMap,
    syncReadonlyPayeeLookups
  }
}

async function loadReadonlyPayeeLookups(schema: ProcessFormDesignSchema) {
  const needsPayee = hasBusinessComponent(schema, 'payee')
  const needsPayeeAccount = hasBusinessComponent(schema, 'payee-account')

  if (!needsPayee && !needsPayeeAccount) {
    return {
      payeeOptionMap: {},
      payeeAccountOptionMap: {}
    }
  }

  const [payeeResult, payeeAccountResult] = await Promise.allSettled([
    needsPayee
      ? expenseCreateApi.listPayeeOptions('')
      : Promise.resolve({ data: [] as ExpenseCreatePayeeOption[] }),
    needsPayeeAccount
      ? expenseCreateApi.listPayeeAccountOptions('')
      : Promise.resolve({ data: [] as ExpenseCreatePayeeAccountOption[] })
  ])

  if (payeeResult.status === 'rejected') {
    console.warn('Failed to load readonly payee options', payeeResult.reason)
  }
  if (payeeAccountResult.status === 'rejected') {
    console.warn('Failed to load readonly payee account options', payeeAccountResult.reason)
  }

  return {
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
