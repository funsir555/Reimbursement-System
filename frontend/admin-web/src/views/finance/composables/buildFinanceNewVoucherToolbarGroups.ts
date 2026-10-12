import type { Component } from 'vue'
import {
  CircleClose, Coin, Delete, DocumentCopy, Download, Edit, Plus, Printer,
  RefreshLeft, Search, Select, Tickets, Top, TrendCharts, Tools
} from '@element-plus/icons-vue'
import type { FinanceNewVoucherToolbarActionKey } from './useFinanceNewVoucherPageOrchestration'

export interface FinanceNewVoucherToolbarAction {
  key: FinanceNewVoucherToolbarActionKey
  label: string
  icon: Component
  shortcut?: string
  emphasis?: 'primary' | 'secondary'
  disabled?: boolean
}

export interface FinanceNewVoucherToolbarState {
  reviewMode: boolean
  detailRoute: boolean
  editingExisting: boolean
  createLockedLast: boolean
  createEditingNew: boolean
  detailEditable: boolean | undefined
  canReviewVoucher: boolean
  canUnreviewVoucher: boolean
  canMarkVoucherError: boolean
  canEditExisting: boolean
  isVoidedVoucher: boolean
  currentVoucherNo: string
  readonlyMode: boolean
  currentPeriodClosed: boolean
  voucherStatus: string | undefined
}

export function buildFinanceNewVoucherToolbarGroups(state: FinanceNewVoucherToolbarState):
  Array<{ key: string; actions: FinanceNewVoucherToolbarAction[] }> {
  if (state.reviewMode) {
    return [
      {
        key: 'review-primary',
        actions: [
          { key: 'review', label: '审核', icon: Select, emphasis: 'secondary', disabled: !state.canReviewVoucher }
        ]
      },
      {
        key: 'review-actions',
        actions: [
          { key: 'export', label: '导出', icon: Download },
          { key: 'find', label: '查找', icon: Search },
          { key: 'unreview', label: '反审核', icon: RefreshLeft, disabled: !state.canUnreviewVoucher },
          {
            key: 'markError',
            label: state.voucherStatus === 'ERROR' ? '取消错误' : '标记错误',
            icon: CircleClose,
            disabled: !state.canMarkVoucherError
          },
          ...(state.isVoidedVoucher
            ? [{ key: 'restore', label: '恢复', icon: RefreshLeft,
                disabled: !state.canEditExisting || !state.currentVoucherNo } as FinanceNewVoucherToolbarAction]
            : [
                { key: 'reverse', label: '冲销', icon: RefreshLeft,
                  disabled: !state.canEditExisting || !state.currentVoucherNo } as FinanceNewVoucherToolbarAction,
                { key: 'void', label: '作废', icon: CircleClose,
                  disabled: !state.canEditExisting || !state.currentVoucherNo } as FinanceNewVoucherToolbarAction
              ]),
          { key: 'deleteVoucher', label: '删除凭证', icon: Delete,
            disabled: !state.canEditExisting || !state.currentVoucherNo }
        ]
      }
    ]
  }

  const primaryActions: FinanceNewVoucherToolbarAction[] = [
    { key: 'new', label: '新增', icon: Plus, emphasis: 'secondary', shortcut: 'F5' }
  ]
  if (
    ((state.detailRoute && !state.editingExisting) || state.createLockedLast)
    && state.detailEditable
    && state.canEditExisting
  ) {
    primaryActions.push({ key: 'modify', label: '修改', icon: Edit, emphasis: 'primary' })
  }

  const editActions: FinanceNewVoucherToolbarAction[] = [
    { key: 'print', label: '打印', icon: Printer, shortcut: 'Ctrl+P' },
    { key: 'export', label: '导出', icon: Download },
    { key: 'copy', label: '复制', icon: DocumentCopy, shortcut: 'Ctrl+F' }
  ]
  if (state.currentVoucherNo) {
    if (state.isVoidedVoucher) {
      editActions.push(
        { key: 'restore', label: '恢复', icon: RefreshLeft, disabled: !state.canEditExisting },
        { key: 'deleteVoucher', label: '删除凭证', icon: Delete, disabled: !state.canEditExisting }
      )
    } else {
      editActions.push(
        { key: 'reverse', label: '冲销', icon: RefreshLeft, disabled: !state.canEditExisting },
        { key: 'void', label: '作废', icon: CircleClose, disabled: !state.canEditExisting },
        { key: 'deleteVoucher', label: '删除凭证', icon: Delete, disabled: !state.canEditExisting }
      )
    }
  }
  if (!state.readonlyMode || state.currentPeriodClosed) {
    editActions.push({ key: 'insert', label: '插入行', icon: Top })
    editActions.push({ key: 'delete', label: '删行', icon: Delete, shortcut: 'Ctrl+D' })
  }

  const actionGroup: FinanceNewVoucherToolbarAction[] = [
    { key: 'searchReplace', label: '查找替换', icon: Search },
    { key: 'cashFlow', label: '现金流量', icon: TrendCharts }
  ]
  if (!state.readonlyMode || state.currentPeriodClosed) {
    actionGroup.push({ key: 'save', label: '保存', icon: Select, emphasis: 'primary', shortcut: 'F6' })
  }

  return [
    { key: 'primary', actions: primaryActions },
    { key: 'edit', actions: editActions },
    { key: 'actions', actions: actionGroup },
    {
      key: 'tools',
      actions: [
        { key: 'assist', label: '辅助核算', icon: Tickets },
        { key: 'balance', label: '平衡', icon: Coin },
        { key: 'calculator', label: '计算器', icon: Tools, shortcut: 'F9' },
        ...(state.createEditingNew
          ? [{ key: 'clear', label: '清空', icon: CircleClose } as FinanceNewVoucherToolbarAction] : [])
      ]
    }
  ]
}
