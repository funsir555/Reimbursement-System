<template>
  <el-card
    v-for="block in blocks"
    :key="block.key"
    class="expense-wb-panel expense-wb-table-shell finance-ledger-report-panel"
  >
    <h3 v-if="ordinary" class="finance-detail-ledger-block-title">{{ block.label }}</h3>
    <el-table :data="block.rows" v-loading="loading" stripe>
      <el-table-column v-if="!ordinary" prop="groupKey" label="分组" min-width="160" show-overflow-tooltip />
      <el-table-column prop="rowType" label="行类型" min-width="100">
        <template #default="{ row }">{{ ordinary ? rowTypeLabel(row.rowType) : row.rowType }}</template>
      </el-table-column>
      <el-table-column prop="subjectCode" label="科目编码" min-width="130" />
      <el-table-column prop="subjectName" label="科目名称" min-width="180" />
      <el-table-column prop="assistLabel" label="辅助项" min-width="160" show-overflow-tooltip />
      <el-table-column prop="dbillDate" :label="ordinary ? '制单日期' : '日期'" min-width="120" />
      <el-table-column label="凭证号" min-width="130">
        <template #default="{ row }">
          <button v-if="row.rowType === 'ENTRY' && row.voucherNo" class="finance-detail-ledger-link" type="button" @click="emit('open-voucher', row)">
            {{ row.displayVoucherNo }}
          </button>
          <span v-else>{{ row.displayVoucherNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
      <el-table-column label="借方" min-width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.debit) }}</template>
      </el-table-column>
      <el-table-column label="贷方" min-width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.credit) }}</template>
      </el-table-column>
      <el-table-column label="余额" min-width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.balance) }}</template>
      </el-table-column>
      <el-table-column prop="balanceDirection" label="方向" min-width="80" align="center" />
      <template v-if="showQuantity">
        <el-table-column label="数量借方" min-width="120" align="right"><template #default="{ row }">{{ formatMoney(row.quantityDebit) }}</template></el-table-column>
        <el-table-column label="数量贷方" min-width="120" align="right"><template #default="{ row }">{{ formatMoney(row.quantityCredit) }}</template></el-table-column>
        <el-table-column label="数量余额" min-width="120" align="right"><template #default="{ row }">{{ formatMoney(row.quantityBalance) }}</template></el-table-column>
        <el-table-column prop="measureUnit" label="计量单位" min-width="100" />
      </template>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FinanceDetailLedgerRow } from '@/api'

const props = defineProps<{
  rows: FinanceDetailLedgerRow[]
  ordinary: boolean
  loading: boolean
  showQuantity: boolean
  formatMoney: (value: unknown) => string
}>()
const emit = defineEmits<{ 'open-voucher': [row: FinanceDetailLedgerRow] }>()

const blocks = computed(() => {
  if (!props.ordinary) return [{ key: 'special', label: '', rows: props.rows }]
  const result: { key: string; label: string; rows: FinanceDetailLedgerRow[] }[] = []
  for (const row of props.rows) {
    const key = row.rootSubjectCode || row.subjectCode
    let block = result[result.length - 1]
    if (!block || block.key !== key) {
      block = { key, label: `${key} ${row.rootSubjectName || row.subjectName}`, rows: [] }
      result.push(block)
    }
    block.rows.push(row)
  }
  return result
})

const labels: Record<FinanceDetailLedgerRow['rowType'], string> = {
  OPENING: '期初余额', ENTRY: '凭证分录', TOTAL: '本月合计', ENDING: '期末余额', CARRY_FORWARD: '承前余额'
}
function rowTypeLabel(type: FinanceDetailLedgerRow['rowType']) { return labels[type] || type }
</script>

<style scoped>
.finance-detail-ledger-block-title { margin: 0 0 12px; font-size: 16px; }
.finance-detail-ledger-link { border: 0; padding: 0; color: var(--el-color-primary); background: transparent; cursor: pointer; }
</style>
