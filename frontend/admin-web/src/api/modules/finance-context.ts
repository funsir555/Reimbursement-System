// 这里集中封装 finance-context.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request from './core'
import type { FinanceContextMeta } from './finance-context-types'

// 这一组方法供对应页面统一调用。
export const financeContextApi = {
  getMeta: () => request<FinanceContextMeta>('/auth/finance/context/meta')
}
