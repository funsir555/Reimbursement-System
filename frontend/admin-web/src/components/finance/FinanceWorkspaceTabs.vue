<template>
  <div class="finance-tabs-wrap">
    <div class="finance-tabs-list">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
        class="finance-tab"
        :class="{ 'finance-tab-active': tab.path === activePath }"
        @click="$emit('select', tab.path)"
      >
        <span class="finance-tab-title">{{ tab.title }}</span>
        <span
          v-if="tabs.length > 1"
          class="finance-tab-close"
          role="button"
          tabindex="0"
          aria-label="关闭页签"
          @click.stop="$emit('close', tab.path)"
          @keydown.enter.stop="$emit('close', tab.path)"
        >
          ×
        </span>
      </button>
    </div>

    <div class="finance-tabs-tools">
      <div class="finance-tool-inline-group finance-period-group">
        <span class="finance-tools-label">会计期间</span>
        <div class="finance-period-controls">
          <el-select
            :model-value="periodYearValue"
            placeholder="年"
            :disabled="periodDisabled"
            class="finance-period-select finance-period-select-year"
            data-testid="period-year-select"
            :style="{ '--finance-period-select-width': PERIOD_YEAR_WIDTH }"
            @update:model-value="handleYearChange"
          >
            <el-option
              v-for="year in periodYearOptions"
              :key="year"
              :label="String(year)"
              :value="year"
            />
          </el-select>
          <span class="finance-period-separator">年</span>
          <el-select
            :model-value="periodMonthValue"
            placeholder="月"
            :disabled="periodDisabled"
            class="finance-period-select finance-period-select-month"
            data-testid="period-month-select"
            :style="{ '--finance-period-select-width': PERIOD_MONTH_WIDTH }"
            @update:model-value="handleMonthChange"
          >
            <el-option
              v-for="month in periodMonthOptions"
              :key="month"
              :label="String(month).padStart(2, '0')"
              :value="month"
            />
          </el-select>
          <span class="finance-period-separator">月</span>
          <el-tooltip v-if="periodHint" :content="periodHint" placement="bottom">
            <span class="finance-tools-help" data-testid="period-tooltip" aria-label="期间提示">!</span>
          </el-tooltip>
        </div>
      </div>

      <div class="finance-tool-inline-group finance-company-group">
        <span class="finance-tools-label">当前公司</span>
        <el-select
          :model-value="currentCompanyId"
          filterable v-bind="globalFilterableSelectProps"
          :filter-method="handleCompanyFilter"
          :loading="companyLoading || companySwitching"
          :disabled="companyLoading || companySwitching"
          class="finance-company-select"
          data-testid="company-select"
          placeholder="请选择公司"
          :style="{ '--finance-company-select-width': COMPANY_SELECT_WIDTH }"
          @update:model-value="handleCompanyChange"
          @visible-change="handleCompanyDropdownVisibleChange"
        >
          <el-option
            v-for="item in filteredCompanyOptions"
            :key="item.companyId"
            :label="item.companyName"
            :value="item.companyId"
          />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'


type FinanceTabItem = {
  path: string
  title: string
}

type FinanceCompanyOption = {
  companyId: string
  companyCode?: string
  companyName: string
  label?: string
  value?: string
}

const COMPANY_SELECT_WIDTH = '292px'
const PERIOD_YEAR_WIDTH = '74px'
const PERIOD_MONTH_WIDTH = '60px'

const props = withDefaults(defineProps<{
  tabs: FinanceTabItem[]
  activePath: string
  companyOptions: FinanceCompanyOption[]
  currentCompanyId?: string
  companyLoading?: boolean
  companySwitching?: boolean
  periodYear?: number
  periodMonth?: number
  periodYearOptions?: number[]
  periodMonthOptions?: number[]
  periodDisabled?: boolean
  periodHint?: string
}>(), {
  currentCompanyId: '',
  companyLoading: false,
  companySwitching: false,
  periodYear: 0,
  periodMonth: 0,
  periodYearOptions: () => [],
  periodMonthOptions: () => [],
  periodDisabled: false,
  periodHint: ''
})

const emit = defineEmits<{
  (event: 'select', path: string): void
  (event: 'close', path: string): void
  (event: 'changeCompany', companyId: string): void
  (event: 'changePeriod', payload: { year: number; month: number }): void
}>()

const companyKeyword = ref('')

const filteredCompanyOptions = computed(() => {
  const keyword = companyKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return props.companyOptions
  }
  return props.companyOptions.filter((item) => {
    const name = String(item.companyName || '').toLowerCase()
    const code = String(item.companyCode || '').toLowerCase()
    return name.includes(keyword) || code.includes(keyword)
  })
})

const periodYearValue = computed(() => (props.periodYear > 0 ? props.periodYear : undefined))
const periodMonthValue = computed(() => (props.periodMonth > 0 ? props.periodMonth : undefined))

function handleCompanyFilter(query: string) {
  companyKeyword.value = String(query || '')
}

function handleCompanyDropdownVisibleChange(visible: boolean) {
  if (!visible) {
    companyKeyword.value = ''
  }
}

function handleCompanyChange(companyId: string | number) {
  emit('changeCompany', String(companyId || ''))
}

function handleYearChange(year: string | number) {
  const nextYear = Number(year || 0)
  const nextMonth = props.periodMonthOptions.includes(props.periodMonth)
    ? props.periodMonth
    : (props.periodMonthOptions[0] || 0)
  if (nextYear > 0 && nextMonth > 0) {
    emit('changePeriod', { year: nextYear, month: nextMonth })
  }
}

function handleMonthChange(month: string | number) {
  const nextMonth = Number(month || 0)
  const nextYear = props.periodYear || props.periodYearOptions[0] || 0
  if (nextYear > 0 && nextMonth > 0) {
    emit('changePeriod', { year: nextYear, month: nextMonth })
  }
}
</script>

<style scoped>
.finance-tabs-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #dbe4f0;
  background: linear-gradient(180deg, #f9fbff 0%, #f3f7fd 100%);
  padding: 10px 14px 8px;
}

.finance-tabs-list {
  display: flex;
  min-width: 0;
  flex: 1;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.finance-tabs-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.finance-tool-inline-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  border: 1px solid #d5dde9;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  padding: 6px 10px;
}

.finance-tools-label {
  flex-shrink: 0;
  color: #4d6179;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.finance-period-controls {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.finance-period-select {
  width: var(--finance-period-select-width);
}

.finance-period-separator {
  color: #6b7f97;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.finance-tools-help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #f7e7bb;
  color: #8a5a12;
  cursor: help;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.finance-company-select {
  width: var(--finance-company-select-width);
}

.finance-tab {
  display: inline-flex;
  min-width: 168px;
  max-width: 240px;
  align-items: center;
  gap: 8px;
  border: 1px solid #d5dde9;
  border-radius: 12px 12px 0 0;
  background: #edf2f9;
  color: #516174;
  cursor: pointer;
  flex: 0 0 auto;
  padding: 8px 10px 7px 12px;
  transition: all 0.18s ease;
}

.finance-tab:hover {
  border-color: #bfd1ea;
  background: #f6f9fe;
  color: #28415f;
}

.finance-tab-active {
  border-color: #b8cce6;
  border-bottom-color: #f8fbff;
  background: #f8fbff;
  box-shadow: 0 -1px 0 #e7effa inset;
  color: #1f3c63;
}

.finance-tab-title {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
}

.finance-tab-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0;
}

:deep(.finance-company-select .el-select__wrapper),
:deep(.finance-period-select .el-select__wrapper) {
  min-height: 34px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d8e2f0 inset;
}

@media (max-width: 1200px) {
  .finance-tabs-wrap {
    flex-direction: column;
    align-items: stretch;
  }

  .finance-tabs-tools {
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .finance-tabs-tools {
    flex-direction: column;
    align-items: stretch;
  }

  .finance-tool-inline-group {
    width: 100%;
    justify-content: space-between;
  }

  .finance-period-controls {
    flex: 1;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .finance-company-select,
  .finance-period-select {
    width: 100%;
  }

  .finance-period-controls .finance-period-separator {
    display: none;
  }
}
</style>
