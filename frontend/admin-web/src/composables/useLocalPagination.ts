import { computed, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'

const DEFAULT_PAGE_SIZES = [10, 20, 50, 100, 200] as const

type UseLocalPaginationOptions = {
  defaultPageSize?: number
  pageSizes?: number[]
}

export function useLocalPagination<T>(
  rows: MaybeRefOrGetter<T[]>,
  options: UseLocalPaginationOptions = {}
) {
  const pageSizes = options.pageSizes?.length ? [...options.pageSizes] : [...DEFAULT_PAGE_SIZES]
  const defaultPageSize = pageSizes.includes(options.defaultPageSize || 0) ? options.defaultPageSize || 10 : pageSizes[0] || 10

  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = computed(() => toValue(rows).length)
  const paginatedRows = computed(() => {
    const sourceRows = toValue(rows)
    const start = (currentPage.value - 1) * pageSize.value
    return sourceRows.slice(start, start + pageSize.value)
  })

  function maxPage() {
    return Math.max(1, Math.ceil(total.value / pageSize.value))
  }

  function resetToFirstPage() {
    currentPage.value = 1
  }

  function clampCurrentPage() {
    if (total.value === 0) {
      currentPage.value = 1
      return
    }
    currentPage.value = Math.min(Math.max(1, currentPage.value), maxPage())
  }

  watch(pageSize, () => {
    currentPage.value = 1
    clampCurrentPage()
  })

  watch(total, () => {
    clampCurrentPage()
  })

  return {
    currentPage,
    pageSize,
    pageSizes,
    total,
    paginatedRows,
    resetToFirstPage,
    clampCurrentPage
  }
}
