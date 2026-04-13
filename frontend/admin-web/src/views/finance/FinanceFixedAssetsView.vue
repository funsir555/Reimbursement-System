<template>
  <div class="fa-page">
    <section class="fa-toolbar">
      <div class="fa-actions">
        <el-button v-if="canCreate" type="primary" @click="openCardDialog()">新增卡片</el-button>
        <el-button v-if="canImport" @click="downloadOpeningTemplate">下载模板</el-button>
        <el-button v-if="canImport" @click="triggerOpeningUpload">期初导入</el-button>
        <el-button v-if="canChange" @click="openChangeDialog()">资产变动</el-button>
        <el-button v-if="canDepreciate" @click="previewDepreciation">折旧试算</el-button>
        <el-button v-if="canDispose" @click="openDisposalDialog()">资产处置</el-button>
        <el-button v-if="canClosePeriod" @click="closePeriod">期间结账</el-button>
        <el-button @click="refreshAll">刷新</el-button>
      </div>
    </section>

    <input ref="openingFileInput" class="fa-hidden" type="file" accept=".csv,text/csv" @change="onOpeningFilePicked" />

    <section class="fa-header-card">
      <div>
        <h1>固定资产工作台</h1>
        <p>按 U8 固定资产闭环设计落地，覆盖类别、卡片、期初、折旧、变动、处置与凭证联动。</p>
      </div>
      <div class="fa-chips">
        <div class="fa-chip"><span>期间状态</span><strong>{{ meta?.periodStatus === 'CLOSED' ? '已结账' : '开放中' }}</strong></div>
        <div class="fa-chip"><span>资产卡片</span><strong>{{ meta?.cardCount || 0 }}</strong></div>
        <div class="fa-chip"><span>待计提数</span><strong>{{ meta?.pendingDepreciationCount || 0 }}</strong></div>
        <div class="fa-chip"><span>本期折旧</span><strong>{{ moneyText(meta?.currentPeriodDepreciationAmount || 0) }}</strong></div>
      </div>
    </section>

    <section class="fa-filter-card">
      <label><span>公司</span><div class="fa-static-field">{{ currentCompanyName || '未设置' }}</div></label>
      <label><span>账簿</span><el-select v-model="filters.bookCode" @change="refreshAll"><el-option v-for="item in meta?.bookOptions || []" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
      <label><span>年度</span><el-input-number v-model="filters.fiscalYear" :controls="false" :min="2000" :max="2099" @change="refreshAll" /></label>
      <label><span>期间</span><el-input-number v-model="filters.fiscalPeriod" :controls="false" :min="1" :max="12" @change="refreshAll" /></label>
      <label><span>资产类别</span><el-select v-model="filters.categoryId" clearable filterable @change="loadCards"><el-option v-for="item in meta?.categoryOptions || []" :key="item.value" :label="item.label" :value="Number(item.value)" /></el-select></label>
      <label><span>状态</span><el-select v-model="filters.status" clearable @change="loadCards"><el-option v-for="item in meta?.cardStatusOptions || []" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
      <label class="search"><span>关键字</span><el-input v-model="filters.keyword" clearable placeholder="资产编码 / 资产名称" @keyup.enter="loadCards" /></label>
    </section>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="资产台账" name="ledger">
        <div class="fa-grid two-col">
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>资产台账</h2><span>{{ cards.length }} 项</span></div></template>
            <el-table v-loading="loading.cards" :data="cards" row-key="id" highlight-current-row @current-change="handleCardCurrentChange">
              <el-table-column prop="assetCode" label="资产编码" min-width="120" />
              <el-table-column prop="assetName" label="资产名称" min-width="160" />
              <el-table-column prop="categoryName" label="类别" min-width="120" />
              <el-table-column prop="useDeptName" label="使用部门" min-width="120" />
              <el-table-column prop="keeperName" label="保管人" min-width="100" />
              <el-table-column label="原值" min-width="100"><template #default="{ row }">{{ moneyText(row.originalAmount) }}</template></el-table-column>
              <el-table-column label="净值" min-width="100"><template #default="{ row }">{{ moneyText(row.netAmount) }}</template></el-table-column>
              <el-table-column label="状态" min-width="90"><template #default="{ row }"><el-tag effect="plain">{{ cardStatusLabel(row.status) }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="88" fixed="right"><template #default="{ row }"><el-button v-if="canEdit" type="primary" link @click="openCardDialog(row)">编辑</el-button></template></el-table-column>
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>卡片摘要</h2></div></template>
            <template v-if="selectedCard">
              <div class="fa-summary">
                <div><span>资产编码</span><strong>{{ selectedCard.assetCode }}</strong></div>
                <div><span>资产名称</span><strong>{{ selectedCard.assetName }}</strong></div>
                <div><span>原值</span><strong>{{ moneyText(selectedCard.originalAmount) }}</strong></div>
                <div><span>累计折旧</span><strong>{{ moneyText(selectedCard.accumDeprAmount) }}</strong></div>
                <div><span>净残值</span><strong>{{ moneyText(selectedCard.salvageAmount) }}</strong></div>
                <div><span>净值</span><strong>{{ moneyText(selectedCard.netAmount) }}</strong></div>
                <div><span>已折旧月数</span><strong>{{ selectedCard.depreciatedMonths }}</strong></div>
                <div><span>剩余月数</span><strong>{{ selectedCard.remainingMonths }}</strong></div>
              </div>
            </template>
            <el-empty v-else description="请选择左侧资产卡片查看详情" />
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="类别与会计政策" name="category">
        <div class="fa-grid two-col">
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>资产类别</h2><el-button v-if="canEdit" type="primary" @click="openCategoryDialog()">新增类别</el-button></div></template>
            <el-table v-loading="loading.categories" :data="categories" row-key="id" highlight-current-row @current-change="handleCategoryCurrentChange">
              <el-table-column prop="categoryCode" label="类别编码" min-width="120" />
              <el-table-column prop="categoryName" label="类别名称" min-width="140" />
              <el-table-column label="共享范围" min-width="100"><template #default="{ row }">{{ row.shareScope === 'GROUP' ? '集团级' : '公司级' }}</template></el-table-column>
              <el-table-column label="折旧方法" min-width="120"><template #default="{ row }">{{ optionLabel(meta?.depreciationMethodOptions, row.depreciationMethod) }}</template></el-table-column>
              <el-table-column prop="assetAccount" label="资产科目" min-width="120" />
              <el-table-column prop="accumDeprAccount" label="累计折旧科目" min-width="130" />
              <el-table-column label="操作" width="88" fixed="right"><template #default="{ row }"><el-button v-if="canEdit" type="primary" link @click="openCategoryDialog(row)">编辑</el-button></template></el-table-column>
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>政策摘要</h2></div></template>
            <template v-if="selectedCategory">
              <div class="fa-summary">
                <div><span>类别编码</span><strong>{{ selectedCategory.categoryCode }}</strong></div>
                <div><span>类别名称</span><strong>{{ selectedCategory.categoryName }}</strong></div>
                <div><span>共享范围</span><strong>{{ selectedCategory.shareScope === 'GROUP' ? '集团级' : '公司级' }}</strong></div>
                <div><span>折旧方法</span><strong>{{ optionLabel(meta?.depreciationMethodOptions, selectedCategory.depreciationMethod) }}</strong></div>
                <div><span>默认使用月数</span><strong>{{ selectedCategory.usefulLifeMonths }} 个月</strong></div>
                <div><span>残值率</span><strong>{{ Number(selectedCategory.residualRate || 0).toFixed(4) }}</strong></div>
                <div><span>处置清理科目</span><strong>{{ selectedCategory.disposalAccount || '-' }}</strong></div>
                <div><span>对方科目</span><strong>{{ selectedCategory.offsetAccount || '-' }}</strong></div>
              </div>
            </template>
            <el-empty v-else description="请选择左侧类别查看会计政策" />
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="期初导入" name="opening">
        <div class="fa-grid two-col">
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>期初导入</h2><div class="fa-actions"><el-button v-if="canImport" @click="downloadOpeningTemplate">下载模板</el-button><el-button v-if="canImport" type="primary" @click="triggerOpeningUpload">上传 CSV</el-button></div></div></template>
            <el-alert type="info" :closable="false" show-icon title="导入只创建资产卡片和期初余额，不会补录历史凭证。" />
            <div v-if="lastImport" class="fa-summary compact">
              <div><span>批次号</span><strong>{{ lastImport.batchNo }}</strong></div>
              <div><span>总行数</span><strong>{{ lastImport.totalRows }}</strong></div>
              <div><span>成功行</span><strong>{{ lastImport.successRows }}</strong></div>
              <div><span>失败行</span><strong>{{ lastImport.failedRows }}</strong></div>
            </div>
            <el-table v-if="lastImport" :data="lastImport.lines">
              <el-table-column prop="rowNo" label="行号" width="80" />
              <el-table-column prop="assetCode" label="资产编码" min-width="120" />
              <el-table-column prop="assetName" label="资产名称" min-width="140" />
              <el-table-column prop="categoryCode" label="类别编码" min-width="120" />
              <el-table-column label="结果" min-width="90"><template #default="{ row }">{{ row.resultStatus === 'SUCCESS' ? '成功' : '失败' }}</template></el-table-column>
              <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="暂无导入回执，请先下载模板并上传文件" />
          </el-card>
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>导入说明</h2></div></template>
            <div class="fa-tips">
              <p>模板中的类别编码必须已在当前公司下维护。</p>
              <p>已结账期间不允许再做期初导入。</p>
              <p>重复资产编码会整批报错，不会静默跳过。</p>
              <p>导入成功后可直接继续折旧试算或变动处理。</p>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
      <el-tab-pane label="折旧台账" name="depreciation">
        <div class="fa-grid two-col">
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>折旧批次</h2><div class="fa-actions"><el-button v-if="canDepreciate" @click="previewDepreciation">折旧试算</el-button><el-button v-if="canDepreciate && previewRun" type="primary" @click="createDepreciationRun">生成批次</el-button></div></div></template>
            <el-card v-if="previewRun" shadow="never" class="preview-card">
              <template #header><div class="fa-card-head"><h2>最新试算</h2><span>{{ previewRun.assetCount }} 项 / {{ moneyText(previewRun.totalAmount) }}</span></div></template>
              <el-table :data="previewRun.lines" max-height="220">
                <el-table-column prop="assetCode" label="资产编码" min-width="120" />
                <el-table-column prop="assetName" label="资产名称" min-width="140" />
                <el-table-column label="折旧方法" min-width="120"><template #default="{ row }">{{ optionLabel(meta?.depreciationMethodOptions, row.depreciationMethod) }}</template></el-table-column>
                <el-table-column label="折旧金额" min-width="100"><template #default="{ row }">{{ moneyText(row.depreciationAmount) }}</template></el-table-column>
              </el-table>
            </el-card>
            <el-table v-loading="loading.depreciation" :data="depreciationRuns" row-key="id">
              <el-table-column prop="runNo" label="批次号" min-width="160" />
              <el-table-column prop="assetCount" label="资产数" min-width="90" />
              <el-table-column label="折旧金额" min-width="100"><template #default="{ row }">{{ moneyText(row.totalAmount) }}</template></el-table-column>
              <el-table-column label="状态" min-width="90"><template #default="{ row }"><el-tag effect="plain">{{ billStatusLabel(row.status) }}</el-tag></template></el-table-column>
              <el-table-column label="凭证号" min-width="140"><template #default="{ row }">{{ canViewVoucherLink ? row.voucherLink?.voucherNo || '-' : '无权限' }}</template></el-table-column>
              <el-table-column label="操作" width="88" fixed="right"><template #default="{ row }"><el-button v-if="canDepreciate && row.status === 'DRAFT' && row.id" type="primary" link @click="postDepreciationRun(row.id)">过账</el-button></template></el-table-column>
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>折旧提示</h2></div></template>
            <div class="fa-tips">
              <p>{{ meta?.periodStatus === 'CLOSED' ? '当前期间已结账，不允许继续生成或过账折旧批次。' : '当前期间未结账，可先试算后生成折旧批次。' }}</p>
              <p>当月新增资产自下月开始折旧；当月减少资产当月仍计提折旧。</p>
              <p>折旧过账成功后会自动生成总账凭证，并记录固定资产凭证关联。</p>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="处置与变动" name="movement">
        <div class="fa-grid two-col">
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>资产变动单</h2><el-button v-if="canChange" type="primary" @click="openChangeDialog()">新增变动单</el-button></div></template>
            <el-table v-loading="loading.changeBills" :data="changeBills" row-key="id">
              <el-table-column prop="billNo" label="单号" min-width="150" />
              <el-table-column label="单据类型" min-width="120"><template #default="{ row }">{{ optionLabel(meta?.changeTypeOptions, row.billType) }}</template></el-table-column>
              <el-table-column prop="billDate" label="单据日期" min-width="110" />
              <el-table-column label="状态" min-width="90"><template #default="{ row }"><el-tag effect="plain">{{ billStatusLabel(row.status) }}</el-tag></template></el-table-column>
              <el-table-column label="金额" min-width="100"><template #default="{ row }">{{ moneyText(row.totalAmount) }}</template></el-table-column>
              <el-table-column label="凭证号" min-width="140"><template #default="{ row }">{{ canViewVoucherLink ? row.voucherLink?.voucherNo || '-' : '无权限' }}</template></el-table-column>
              <el-table-column label="操作" width="88" fixed="right"><template #default="{ row }"><el-button v-if="canChange && row.status === 'DRAFT' && row.id" type="primary" link @click="postChangeBill(row.id)">过账</el-button></template></el-table-column>
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header><div class="fa-card-head"><h2>资产处置单</h2><el-button v-if="canDispose" type="primary" plain @click="openDisposalDialog()">新增处置单</el-button></div></template>
            <el-table v-loading="loading.disposalBills" :data="disposalBills" row-key="id">
              <el-table-column prop="billNo" label="单号" min-width="150" />
              <el-table-column prop="billDate" label="单据日期" min-width="110" />
              <el-table-column label="净值" min-width="100"><template #default="{ row }">{{ moneyText(row.totalNetAmount) }}</template></el-table-column>
              <el-table-column label="状态" min-width="90"><template #default="{ row }"><el-tag effect="plain">{{ billStatusLabel(row.status) }}</el-tag></template></el-table-column>
              <el-table-column label="凭证号" min-width="140"><template #default="{ row }">{{ canViewVoucherLink ? row.voucherLink?.voucherNo || '-' : '无权限' }}</template></el-table-column>
              <el-table-column label="操作" width="88" fixed="right"><template #default="{ row }"><el-button v-if="canDispose && row.status === 'DRAFT' && row.id" type="primary" link @click="postDisposalBill(row.id)">过账</el-button></template></el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="cardDialogVisible" :title="editingCardId ? '编辑资产卡片' : '新增资产卡片'" width="860px">
      <div class="form-grid">
        <label><span>资产编码</span><el-input v-model="cardForm.assetCode" :disabled="Boolean(editingCardId)" :maxlength="FA_CODE_MAX_LENGTH" show-word-limit /></label>
        <label><span>资产名称</span><el-input v-model="cardForm.assetName" :maxlength="FA_NAME_MAX_LENGTH" show-word-limit /></label>
        <label><span>资产类别</span><el-select v-model="cardForm.categoryId" filterable><el-option v-for="item in categories" :key="item.id" :label="`${item.categoryCode} - ${item.categoryName}`" :value="item.id" /></el-select></label>
        <label><span>启用日期</span><el-date-picker v-model="cardForm.inServiceDate" type="date" value-format="YYYY-MM-DD" /></label>
        <label><span>使用部门</span><el-select v-model="cardForm.useDeptId" filterable clearable><el-option v-for="item in meta?.departmentOptions || []" :key="item.value" :label="item.label" :value="Number(item.value)" /></el-select></label>
        <label><span>保管人</span><el-select v-model="cardForm.keeperUserId" filterable clearable><el-option v-for="item in meta?.employeeOptions || []" :key="item.value" :label="item.label" :value="Number(item.value)" /></el-select></label>
        <label><span>原值</span><money-input v-model="cardForm.originalAmount" class="full" /></label>
        <label><span>累计折旧</span><money-input v-model="cardForm.accumDeprAmount" class="full" /></label>
        <label><span>净残值</span><money-input v-model="cardForm.salvageAmount" class="full" /></label>
        <label><span>使用月数</span><el-input-number v-model="cardForm.usefulLifeMonths" :controls="false" class="full" /></label>
        <label><span>已折旧月数</span><el-input-number v-model="cardForm.depreciatedMonths" :controls="false" class="full" /></label>
        <label><span>剩余月数</span><el-input-number v-model="cardForm.remainingMonths" :controls="false" class="full" /></label>
      </div>
      <template #footer><el-button @click="cardDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCard">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" :title="editingCategoryId ? '编辑资产类别' : '新增资产类别'" width="860px">
      <div class="form-grid">
        <label><span>类别编码</span><el-input v-model="categoryForm.categoryCode" :disabled="Boolean(editingCategoryId)" :maxlength="FA_CODE_MAX_LENGTH" show-word-limit /></label>
        <label><span>类别名称</span><el-input v-model="categoryForm.categoryName" :maxlength="FA_NAME_MAX_LENGTH" show-word-limit /></label>
        <label><span>共享范围</span><el-select v-model="categoryForm.shareScope"><el-option label="公司级" value="COMPANY" /><el-option label="集团级" value="GROUP" /></el-select></label>
        <label><span>折旧方法</span><el-select v-model="categoryForm.depreciationMethod"><el-option v-for="item in meta?.depreciationMethodOptions || []" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
        <label><span>默认使用月数</span><el-input-number v-model="categoryForm.usefulLifeMonths" :controls="false" class="full" /></label>
        <label><span>残值率</span><el-input-number v-model="categoryForm.residualRate" :controls="false" :step="0.0001" :precision="4" class="full" /></label>
        <label><span>资产科目</span><el-input v-model="categoryForm.assetAccount" /></label>
        <label><span>累计折旧科目</span><el-input v-model="categoryForm.accumDeprAccount" /></label>
        <label><span>折旧费用科目</span><el-input v-model="categoryForm.deprExpenseAccount" /></label>
        <label><span>处置清理科目</span><el-input v-model="categoryForm.disposalAccount" /></label>
        <label><span>处置收益科目</span><el-input v-model="categoryForm.gainAccount" /></label>
        <label><span>处置损失科目</span><el-input v-model="categoryForm.lossAccount" /></label>
        <label><span>对方科目</span><el-input v-model="categoryForm.offsetAccount" /></label>
      </div>
      <template #footer><el-button @click="categoryDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCategory">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="changeDialogVisible" title="新增资产变动单" width="720px">
      <div class="form-grid">
        <label><span>变动类型</span><el-select v-model="changeForm.billType"><el-option v-for="item in meta?.changeTypeOptions || []" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
        <label><span>单据日期</span><el-date-picker v-model="changeForm.billDate" type="date" value-format="YYYY-MM-DD" /></label>
        <label><span>资产编码</span><el-input v-model="changeLine.assetCode" :maxlength="FA_CODE_MAX_LENGTH" show-word-limit /></label>
        <label><span>资产名称</span><el-input v-model="changeLine.assetName" :maxlength="FA_NAME_MAX_LENGTH" show-word-limit /></label>
        <label><span>资产类别</span><el-select v-model="changeLine.categoryId" clearable filterable><el-option v-for="item in categories" :key="item.id" :label="`${item.categoryCode} - ${item.categoryName}`" :value="item.id" /></el-select></label>
        <label><span>变动金额</span><money-input v-model="changeLine.changeAmount" class="full" /></label>
      </div>
      <template #footer><el-button @click="changeDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveChangeBill">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="disposalDialogVisible" title="新增资产处置单" width="520px">
      <div class="form-grid one-col">
        <label><span>单据日期</span><el-date-picker v-model="disposalForm.billDate" type="date" value-format="YYYY-MM-DD" /></label>
        <label><span>资产编码</span><el-input v-model="disposalLine.assetCode" :maxlength="FA_CODE_MAX_LENGTH" show-word-limit /></label>
      </div>
      <template #footer><el-button @click="disposalDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDisposalBill">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fixedAssetApi, type FixedAssetCard, type FixedAssetCardPayload, type FixedAssetCategory, type FixedAssetCategoryPayload, type FixedAssetChangeBill, type FixedAssetChangeBillPayload, type FixedAssetChangeLinePayload, type FixedAssetDeprPreviewPayload, type FixedAssetDeprRun, type FixedAssetDisposalBill, type FixedAssetDisposalBillPayload, type FixedAssetDisposalLinePayload, type FixedAssetMeta, type FixedAssetOpeningImportResult, type FixedAssetOpeningImportRow } from '@/api'
import MoneyInput from '@/components/inputs/MoneyInput.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { formatMoney, normalizeMoneyValue } from '@/utils/money'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const FA_CODE_MAX_LENGTH = 32
const FA_NAME_MAX_LENGTH = 64

const permissionCodes = ref(readStoredUser()?.permissionCodes || [])
const activeTab = ref('ledger')
const meta = ref<FixedAssetMeta | null>(null)
const categories = ref<FixedAssetCategory[]>([])
const cards = ref<FixedAssetCard[]>([])
const changeBills = ref<FixedAssetChangeBill[]>([])
const depreciationRuns = ref<FixedAssetDeprRun[]>([])
const disposalBills = ref<FixedAssetDisposalBill[]>([])
const previewRun = ref<FixedAssetDeprRun | null>(null)
const lastImport = ref<FixedAssetOpeningImportResult | null>(null)
const openingFileInput = ref<HTMLInputElement | null>(null)
const selectedCardId = ref<number | null>(null)
const selectedCategoryId = ref<number | null>(null)
const loading = reactive({ cards: false, categories: false, changeBills: false, depreciation: false, disposalBills: false })
const saving = ref(false)
const now = new Date()
const filters = reactive({ companyId: '', bookCode: 'FINANCE', fiscalYear: now.getFullYear(), fiscalPeriod: now.getMonth() + 1, keyword: '', categoryId: undefined as number | undefined, status: '' })
const cardDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const changeDialogVisible = ref(false)
const disposalDialogVisible = ref(false)
const editingCardId = ref<number | null>(null)
const editingCategoryId = ref<number | null>(null)
const cardForm = reactive<FixedAssetCardPayload>(createCardPayload())
const categoryForm = reactive<FixedAssetCategoryPayload>(createCategoryPayload())
const changeForm = reactive<FixedAssetChangeBillPayload>(createChangePayload())
const disposalForm = reactive<FixedAssetDisposalBillPayload>(createDisposalPayload())
const financeCompany = useFinanceCompanyStore()
const COMPANY_SWITCH_GUARD_KEY = 'finance-fixed-assets'
let guardRegistered = false
const selectedCard = computed(() => cards.value.find((item) => item.id === selectedCardId.value) || null)
const selectedCategory = computed(() => categories.value.find((item) => item.id === selectedCategoryId.value) || null)
const currentCompanyName = computed(() => financeCompany.currentCompanyName)
const hasPendingEdit = computed(() => cardDialogVisible.value || categoryDialogVisible.value || changeDialogVisible.value || disposalDialogVisible.value)
const canCreate = computed(() => hasPermission('finance:fixed_assets:create', permissionCodes.value))
const canEdit = computed(() => hasPermission('finance:fixed_assets:edit', permissionCodes.value))
const canImport = computed(() => hasPermission('finance:fixed_assets:import', permissionCodes.value))
const canChange = computed(() => hasPermission('finance:fixed_assets:change', permissionCodes.value))
const canDepreciate = computed(() => hasPermission('finance:fixed_assets:depreciate', permissionCodes.value))
const canDispose = computed(() => hasPermission('finance:fixed_assets:dispose', permissionCodes.value))
const canClosePeriod = computed(() => hasPermission('finance:fixed_assets:close_period', permissionCodes.value))
const canViewVoucherLink = computed(() => hasPermission('finance:fixed_assets:view_voucher_link', permissionCodes.value))
const changeLine = computed(() => { if (!changeForm.lines[0]) changeForm.lines[0] = { assetCode: '' } as FixedAssetChangeLinePayload; return changeForm.lines[0] as FixedAssetChangeLinePayload })
const disposalLine = computed(() => { if (!disposalForm.lines[0]) disposalForm.lines[0] = { assetCode: '' } as FixedAssetDisposalLinePayload; return disposalForm.lines[0] as FixedAssetDisposalLinePayload })

onMounted(registerCompanySwitchGuard)
onActivated(registerCompanySwitchGuard)
onDeactivated(unregisterCompanySwitchGuard)

watch(
  () => financeCompany.currentCompanyId,
  async (companyId, previousCompanyId) => {
    if (!companyId) return
    filters.companyId = companyId
    if (companyId !== previousCompanyId) {
      closeDialogs()
      filters.categoryId = undefined
      filters.status = ''
      filters.keyword = ''
      previewRun.value = null
      lastImport.value = null
    }
    await refreshAll()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  unregisterCompanySwitchGuard()
})

function registerCompanySwitchGuard() {
  if (guardRegistered) return
  financeCompany.registerSwitchGuard(COMPANY_SWITCH_GUARD_KEY, confirmCompanySwitch)
  guardRegistered = true
}

function unregisterCompanySwitchGuard() {
  if (!guardRegistered) return
  financeCompany.unregisterSwitchGuard(COMPANY_SWITCH_GUARD_KEY)
  guardRegistered = false
}

async function refreshAll() {
  if (!financeCompany.currentCompanyId) return
  try {
    const res = await fixedAssetApi.getMeta({ companyId: financeCompany.currentCompanyId, fiscalYear: filters.fiscalYear, fiscalPeriod: filters.fiscalPeriod })
    meta.value = res.data
    filters.companyId = financeCompany.currentCompanyId
    filters.bookCode = res.data.defaultBookCode || filters.bookCode
    filters.fiscalYear = res.data.defaultFiscalYear || filters.fiscalYear
    filters.fiscalPeriod = res.data.defaultFiscalPeriod || filters.fiscalPeriod
    syncForms()
    if (!filters.companyId) return
    await Promise.all([loadCategories(), loadCards(), loadChangeBills(), loadDepreciationRuns(), loadDisposalBills()])
  } catch (error: unknown) {
    ElMessage.error(resolveError(error, '加载固定资产工作台失败'))
  }
}

async function loadCategories() {
  if (!filters.companyId) return
  loading.categories = true
  try { const res = await fixedAssetApi.listCategories(filters.companyId); categories.value = res.data; if (!res.data.some((item) => item.id === selectedCategoryId.value)) selectedCategoryId.value = res.data[0]?.id || null } finally { loading.categories = false }
}
async function loadCards() {
  if (!filters.companyId) return
  loading.cards = true
  try { const res = await fixedAssetApi.listCards({ companyId: filters.companyId, bookCode: filters.bookCode, keyword: filters.keyword || undefined, categoryId: filters.categoryId, status: filters.status || undefined }); cards.value = res.data; if (!res.data.some((item) => item.id === selectedCardId.value)) selectedCardId.value = res.data[0]?.id || null } finally { loading.cards = false }
}
async function loadChangeBills() { if (!filters.companyId) return; loading.changeBills = true; try { changeBills.value = (await fixedAssetApi.listChangeBills(buildPeriodContext())).data } finally { loading.changeBills = false } }
async function loadDepreciationRuns() { if (!filters.companyId) return; loading.depreciation = true; try { depreciationRuns.value = (await fixedAssetApi.listDepreciationRuns(buildPeriodContext())).data } finally { loading.depreciation = false } }
async function loadDisposalBills() { if (!filters.companyId) return; loading.disposalBills = true; try { disposalBills.value = (await fixedAssetApi.listDisposalBills(buildPeriodContext())).data } finally { loading.disposalBills = false } }

function handleCardCurrentChange(row?: FixedAssetCard) { selectedCardId.value = row?.id || null }
function handleCategoryCurrentChange(row?: FixedAssetCategory) { selectedCategoryId.value = row?.id || null }
async function openCardDialog(row?: FixedAssetCard) { syncForms(); if (row?.id) { Object.assign(cardForm, createCardPayload(), (await fixedAssetApi.getCard(row.id)).data); editingCardId.value = row.id } else { Object.assign(cardForm, createCardPayload()); editingCardId.value = null } cardDialogVisible.value = true }
async function saveCard() { saving.value = true; try { const payload = normalizeCardPayload(); if (editingCardId.value) await fixedAssetApi.updateCard(editingCardId.value, payload); else await fixedAssetApi.createCard(payload); cardDialogVisible.value = false; ElMessage.success('资产卡片保存成功'); await Promise.all([loadCards(), refreshMetaOnly()]) } catch (error: unknown) { ElMessage.error(resolveError(error, '资产卡片保存失败')) } finally { saving.value = false } }
function openCategoryDialog(row?: FixedAssetCategory) { syncForms(); Object.assign(categoryForm, createCategoryPayload(), row || {}); editingCategoryId.value = row?.id || null; categoryDialogVisible.value = true }
async function saveCategory() { saving.value = true; try { const payload = normalizeCategoryPayload(); if (editingCategoryId.value) await fixedAssetApi.updateCategory(editingCategoryId.value, payload); else await fixedAssetApi.createCategory(payload); categoryDialogVisible.value = false; ElMessage.success('资产类别保存成功'); await Promise.all([loadCategories(), refreshMetaOnly()]) } catch (error: unknown) { ElMessage.error(resolveError(error, '资产类别保存失败')) } finally { saving.value = false } }
function openChangeDialog() { syncForms(); Object.assign(changeForm, createChangePayload()); if (selectedCard.value) Object.assign(changeLine.value, { assetId: selectedCard.value.id, assetCode: selectedCard.value.assetCode, assetName: selectedCard.value.assetName, categoryId: selectedCard.value.categoryId, useDeptId: selectedCard.value.useDeptId, keeperUserId: selectedCard.value.keeperUserId }); changeDialogVisible.value = true }
async function saveChangeBill() { saving.value = true; try { await fixedAssetApi.createChangeBill({ ...normalizeChangePayload(), lines: [changeLine.value] }); changeDialogVisible.value = false; ElMessage.success('资产变动单保存成功'); await loadChangeBills() } catch (error: unknown) { ElMessage.error(resolveError(error, '资产变动单保存失败')) } finally { saving.value = false } }
async function postChangeBill(id: number) { try { await ElMessageBox.confirm('过账后将同步写入固定资产台账并生成凭证，是否继续？', '变动单过账', { type: 'warning' }); await fixedAssetApi.postChangeBill(id); ElMessage.success('资产变动单过账成功'); await Promise.all([loadChangeBills(), loadCards(), refreshMetaOnly()]) } catch (error: unknown) { if (!isCancel(error)) ElMessage.error(resolveError(error, '资产变动单过账失败')) } }
function openDisposalDialog() { syncForms(); Object.assign(disposalForm, createDisposalPayload()); if (selectedCard.value) Object.assign(disposalLine.value, { assetId: selectedCard.value.id, assetCode: selectedCard.value.assetCode }); disposalDialogVisible.value = true }
async function saveDisposalBill() { saving.value = true; try { await fixedAssetApi.createDisposalBill({ ...normalizeDisposalPayload(), lines: [disposalLine.value] }); disposalDialogVisible.value = false; ElMessage.success('资产处置单保存成功'); await loadDisposalBills() } catch (error: unknown) { ElMessage.error(resolveError(error, '资产处置单保存失败')) } finally { saving.value = false } }
async function postDisposalBill(id: number) { try { await ElMessageBox.confirm('处置过账后将更新资产状态并自动生成凭证，是否继续？', '处置单过账', { type: 'warning' }); await fixedAssetApi.postDisposalBill(id); ElMessage.success('资产处置单过账成功'); await Promise.all([loadDisposalBills(), loadCards(), refreshMetaOnly()]) } catch (error: unknown) { if (!isCancel(error)) ElMessage.error(resolveError(error, '资产处置单过账失败')) } }
async function previewDepreciation() { try { previewRun.value = (await fixedAssetApi.previewDepreciation(buildDeprPayload())).data; activeTab.value = 'depreciation'; ElMessage.success('折旧试算完成') } catch (error: unknown) { ElMessage.error(resolveError(error, '折旧试算失败')) } }
async function createDepreciationRun() { try { previewRun.value = (await fixedAssetApi.createDepreciationRun(buildDeprPayload())).data; ElMessage.success(`折旧批次已生成：${previewRun.value.runNo || '-'}`); await loadDepreciationRuns() } catch (error: unknown) { ElMessage.error(resolveError(error, '折旧批次生成失败')) } }
async function postDepreciationRun(id: number) { try { await ElMessageBox.confirm('折旧过账后将更新累计折旧并生成凭证，是否继续？', '折旧过账', { type: 'warning' }); await fixedAssetApi.postDepreciationRun(id); previewRun.value = null; ElMessage.success('折旧过账成功'); await Promise.all([loadDepreciationRuns(), loadCards(), refreshMetaOnly()]) } catch (error: unknown) { if (!isCancel(error)) ElMessage.error(resolveError(error, '折旧过账失败')) } }
async function closePeriod() { try { await ElMessageBox.confirm(`确认结账 ${filters.fiscalYear}-${String(filters.fiscalPeriod).padStart(2, '0')} 吗？`, '期间结账', { type: 'warning' }); await fixedAssetApi.closePeriod(buildPeriodContext()); ElMessage.success('固定资产期间结账成功'); await refreshAll() } catch (error: unknown) { if (!isCancel(error)) ElMessage.error(resolveError(error, '固定资产期间结账失败')) } }
async function downloadOpeningTemplate() { try { const res = await fixedAssetApi.getOpeningTemplate(buildPeriodContext()); const blob = new Blob([res.data.templateContent], { type: res.data.contentType || 'text/csv;charset=utf-8' }); const url = window.URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = res.data.fileName || '固定资产期初导入模板.csv'; link.click(); window.URL.revokeObjectURL(url); ElMessage.success('模板下载完成') } catch (error: unknown) { ElMessage.error(resolveError(error, '模板下载失败')) } }
function triggerOpeningUpload() { openingFileInput.value?.click() }
async function onOpeningFilePicked(event: Event) { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return; try { const rows = parseOpeningCsv(await file.text()); lastImport.value = (await fixedAssetApi.importOpening({ ...buildPeriodContext(), rows })).data; activeTab.value = 'opening'; ElMessage.success(`导入完成：成功 ${lastImport.value.successRows} / 总计 ${lastImport.value.totalRows}`); await Promise.all([loadCards(), refreshMetaOnly()]) } catch (error: unknown) { ElMessage.error(resolveError(error, '期初导入失败')) } finally { input.value = '' } }
async function refreshMetaOnly() { meta.value = (await fixedAssetApi.getMeta({ companyId: financeCompany.currentCompanyId || filters.companyId, fiscalYear: filters.fiscalYear, fiscalPeriod: filters.fiscalPeriod })).data }

function parseOpeningCsv(content: string): FixedAssetOpeningImportRow[] { const lines = content.replace(/\uFEFF/g, '').split(/\r?\n/).map((item) => item.trim()).filter(Boolean); if (lines.length < 2) throw new Error('CSV 文件内容为空'); const headers = parseCsvLine(lines[0] || ''); const required = ['assetCode', 'assetName', 'categoryCode', 'inServiceDate', 'originalAmount', 'accumDeprAmount', 'salvageAmount', 'usefulLifeMonths', 'depreciatedMonths', 'remainingMonths']; const indexMap = new Map(headers.map((item, index) => [item, index])); const missing = required.filter((item) => !indexMap.has(item)); if (missing.length) throw new Error(`导入模板缺少字段：${missing.join(', ')}`); return lines.slice(1).map((line, index) => { const cells = parseCsvLine(line); const read = (name: string) => cells[indexMap.get(name) ?? -1] || ''; const row = { rowNo: index + 1, assetCode: requireTextLength(read('assetCode'), '资产编码', FA_CODE_MAX_LENGTH, index + 1), assetName: requireTextLength(read('assetName'), '资产名称', FA_NAME_MAX_LENGTH, index + 1), categoryCode: requireTextLength(read('categoryCode'), '类别编码', FA_CODE_MAX_LENGTH, index + 1), acquireDate: read('acquireDate') || undefined, inServiceDate: read('inServiceDate'), originalAmount: normalizeRequiredMoney(read('originalAmount')), accumDeprAmount: normalizeRequiredMoney(read('accumDeprAmount')), salvageAmount: normalizeRequiredMoney(read('salvageAmount')), usefulLifeMonths: toInteger(read('usefulLifeMonths')), depreciatedMonths: toInteger(read('depreciatedMonths')), remainingMonths: toInteger(read('remainingMonths')), useDeptId: toOptionalInteger(read('useDeptId')), keeperUserId: toOptionalInteger(read('keeperUserId')), status: read('status') || 'IN_USE', workTotal: toOptionalNumber(read('workTotal')), workUsed: toOptionalNumber(read('workUsed')), remark: read('remark') || undefined }; return row }) }
function parseCsvLine(line: string) { const result: string[] = []; let current = ''; let inQuotes = false; for (let i = 0; i < line.length; i += 1) { const char = line[i]; const next = line[i + 1]; if (char === '"') { if (inQuotes && next === '"') { current += '"'; i += 1 } else { inQuotes = !inQuotes } } else if (char === ',' && !inQuotes) { result.push(current.trim()); current = '' } else { current += char } } result.push(current.trim()); return result }
function syncForms() { cardForm.companyId = filters.companyId; cardForm.bookCode = filters.bookCode; cardForm.useCompanyId = filters.companyId; categoryForm.companyId = filters.companyId; categoryForm.bookCode = filters.bookCode; changeForm.companyId = filters.companyId; changeForm.bookCode = filters.bookCode; changeForm.fiscalYear = filters.fiscalYear; changeForm.fiscalPeriod = filters.fiscalPeriod; disposalForm.companyId = filters.companyId; disposalForm.bookCode = filters.bookCode; disposalForm.fiscalYear = filters.fiscalYear; disposalForm.fiscalPeriod = filters.fiscalPeriod }
function buildPeriodContext() { return { companyId: filters.companyId, bookCode: filters.bookCode, fiscalYear: filters.fiscalYear, fiscalPeriod: filters.fiscalPeriod } }
function buildDeprPayload(): FixedAssetDeprPreviewPayload { return { ...buildPeriodContext(), remark: `折旧试算-${filters.fiscalYear}-${String(filters.fiscalPeriod).padStart(2, '0')}` } }
function normalizeCardPayload(): FixedAssetCardPayload { return { ...cardForm, companyId: filters.companyId, bookCode: filters.bookCode, useCompanyId: filters.companyId, assetCode: requireTextLength(cardForm.assetCode, '资产编码', FA_CODE_MAX_LENGTH), assetName: requireTextLength(cardForm.assetName, '资产名称', FA_NAME_MAX_LENGTH), originalAmount: normalizeRequiredMoney(cardForm.originalAmount), accumDeprAmount: normalizeRequiredMoney(cardForm.accumDeprAmount), salvageAmount: normalizeRequiredMoney(cardForm.salvageAmount), usefulLifeMonths: Number(cardForm.usefulLifeMonths || 0), depreciatedMonths: Number(cardForm.depreciatedMonths || 0), remainingMonths: Number(cardForm.remainingMonths || 0), status: cardForm.status || 'IN_USE', canDepreciate: cardForm.canDepreciate ?? true } }
function normalizeCategoryPayload(): FixedAssetCategoryPayload { return { ...categoryForm, companyId: filters.companyId, bookCode: filters.bookCode, categoryCode: requireTextLength(categoryForm.categoryCode, '类别编码', FA_CODE_MAX_LENGTH), categoryName: requireTextLength(categoryForm.categoryName, '类别名称', FA_NAME_MAX_LENGTH), usefulLifeMonths: Number(categoryForm.usefulLifeMonths || 0), residualRate: Number(categoryForm.residualRate || 0), depreciable: categoryForm.depreciable ?? true, status: categoryForm.status || 'ACTIVE' } }
function normalizeChangePayload(): FixedAssetChangeBillPayload { return { ...changeForm, ...buildPeriodContext(), billDate: changeForm.billDate || todayText(), lines: changeForm.lines.map((line) => ({ ...line, assetCode: requireTextLength(line.assetCode, '资产编码', FA_CODE_MAX_LENGTH), assetName: requireOptionalTextLength(line.assetName, '资产名称', FA_NAME_MAX_LENGTH), changeAmount: normalizeOptionalMoney(line.changeAmount), newValue: normalizeOptionalMoney(line.newValue), newSalvageAmount: normalizeOptionalMoney(line.newSalvageAmount) })) } }
function normalizeDisposalPayload(): FixedAssetDisposalBillPayload { return { ...disposalForm, ...buildPeriodContext(), billDate: disposalForm.billDate || todayText(), lines: disposalForm.lines.map((line) => ({ ...line, assetCode: requireTextLength(line.assetCode, '资产编码', FA_CODE_MAX_LENGTH) })) } }
function createCardPayload(): FixedAssetCardPayload { return { companyId: filters.companyId, assetCode: '', assetName: '', categoryId: 0, bookCode: filters.bookCode, useCompanyId: filters.companyId, inServiceDate: todayText(), originalAmount: '0.00', accumDeprAmount: '0.00', salvageAmount: '0.00', usefulLifeMonths: 36, depreciatedMonths: 0, remainingMonths: 36, status: 'IN_USE', canDepreciate: true } }
function createCategoryPayload(): FixedAssetCategoryPayload { return { companyId: filters.companyId, categoryCode: '', categoryName: '', shareScope: 'COMPANY', depreciationMethod: meta.value?.depreciationMethodOptions?.[0]?.value || 'STRAIGHT_LINE', usefulLifeMonths: 36, residualRate: 0.05, depreciable: true, status: 'ACTIVE', bookCode: filters.bookCode, assetAccount: '', accumDeprAccount: '', deprExpenseAccount: '', disposalAccount: '', gainAccount: '', lossAccount: '', offsetAccount: '' } }
function createChangePayload(): FixedAssetChangeBillPayload { return { companyId: filters.companyId, billType: meta.value?.changeTypeOptions?.[0]?.value || 'ADD', bookCode: filters.bookCode, fiscalYear: filters.fiscalYear, fiscalPeriod: filters.fiscalPeriod, billDate: todayText(), lines: [{ assetCode: '' }] } }
function createDisposalPayload(): FixedAssetDisposalBillPayload { return { companyId: filters.companyId, bookCode: filters.bookCode, fiscalYear: filters.fiscalYear, fiscalPeriod: filters.fiscalPeriod, billDate: todayText(), lines: [{ assetCode: '' }] } }
function optionLabel(options: Array<{ value: string; label: string }> | undefined, value?: string) { return options?.find((item) => item.value === value)?.label || value || '-' }
function cardStatusLabel(value?: string) { return optionLabel(meta.value?.cardStatusOptions, value) }
function billStatusLabel(value?: string) { return ({ DRAFT: '草稿', POSTED: '已过账', VOID: '已作废', CLOSED: '已结账', OPEN: '开放中' } as Record<string, string>)[value || ''] || value || '-' }
function resolveError(error: unknown, fallback: string) { return error instanceof Error && error.message ? error.message : fallback }
function isCancel(error: unknown) { return error === 'cancel' || error === 'close' }
function closeDialogs() { cardDialogVisible.value = false; categoryDialogVisible.value = false; changeDialogVisible.value = false; disposalDialogVisible.value = false; editingCardId.value = null; editingCategoryId.value = null; syncForms() }
function trimText(value?: string) { return String(value || '').trim() }
function requireTextLength(value: string | undefined, label: string, maxLength: number, rowNo?: number) { const text = trimText(value); if (text.length > maxLength) throw new Error(`${rowNo ? `第 ${rowNo} 行` : ''}${label}最多 ${maxLength} 个字符`); return text }
function requireOptionalTextLength(value: string | undefined, label: string, maxLength: number, rowNo?: number) { const text = trimText(value); if (!text) return undefined; if (text.length > maxLength) throw new Error(`${rowNo ? `第 ${rowNo} 行` : ''}${label}最多 ${maxLength} 个字符`); return text }
function todayText() { const date = new Date(); const year = date.getFullYear(); const month = String(date.getMonth() + 1).padStart(2, '0'); const day = String(date.getDate()).padStart(2, '0'); return `${year}-${month}-${day}` }
function moneyText(value: number | string | null | undefined) { return formatMoney(value || '0.00') }
function normalizeRequiredMoney(value?: string) { return normalizeMoneyValue(value, { fallback: '0.00' }) }
function normalizeOptionalMoney(value?: string) { return value ? normalizeMoneyValue(value) : undefined }
function toNumber(value: string) { const n = Number(value); return Number.isFinite(n) ? n : 0 }
function toInteger(value: string) { const n = Number.parseInt(value, 10); return Number.isFinite(n) ? n : 0 }
function toOptionalNumber(value: string) { return value ? toNumber(value) : undefined }
function toOptionalInteger(value: string) { return value ? toInteger(value) : undefined }

async function confirmCompanySwitch() {
  if (!hasPendingEdit.value) {
    return true
  }
  try {
    await ElMessageBox.confirm('切换公司后将关闭当前固定资产编辑窗口，并按新公司重新加载台账，是否继续？', '切换公司', {
      type: 'warning',
      confirmButtonText: '继续切换',
      cancelButtonText: '取消'
    })
    return true
  } catch {
    return false
  }
}
</script>

<style scoped>
.fa-page { display: flex; min-height: 100%; flex-direction: column; gap: 16px; background: linear-gradient(180deg, #f4f7fb 0%, #edf2f8 100%); padding: 18px; }
.fa-toolbar, .fa-header-card, .fa-filter-card { border-radius: 24px; border: 1px solid #d8e2ee; background: rgba(255,255,255,.92); box-shadow: 0 12px 30px rgba(15,23,42,.05); }
.fa-toolbar { padding: 14px 16px; }
.fa-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.fa-hidden { display: none; }
.fa-header-card { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; padding: 20px; }
.fa-header-card h1 { margin: 0; font-size: 28px; color: #173454; }
.fa-header-card p { margin: 8px 0 0; color: #647b92; }
.fa-chips { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.fa-chip { border-radius: 16px; background: #f6f9fc; padding: 12px 14px; }
.fa-chip span { display: block; color: #6a8094; font-size: 12px; }
.fa-chip strong { color: #173454; font-size: 18px; }
.fa-filter-card { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 12px; padding: 16px; }
.fa-filter-card label, .form-grid label { display: flex; min-width: 0; flex-direction: column; gap: 8px; }
.fa-filter-card span, .form-grid span { color: #6a8094; font-size: 12px; }
.fa-static-field { display: flex; min-height: 40px; align-items: center; border: 1px solid #d8e2ee; border-radius: 14px; background: linear-gradient(180deg, #f8fbff 0%, #eef5fb 100%); padding: 0 12px; color: #173454; font-weight: 600; }
.fa-filter-card .search { grid-column: span 2; }
.fa-grid { display: grid; gap: 16px; }
.fa-grid.two-col { grid-template-columns: minmax(0, 1fr) 320px; }
.fa-card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.fa-card-head h2 { margin: 0; color: #173454; font-size: 18px; }
.fa-summary { display: grid; gap: 10px; }
.fa-summary.compact { margin-top: 16px; }
.fa-summary div { display: flex; justify-content: space-between; gap: 12px; border-radius: 14px; background: #f6f9fc; padding: 10px 12px; }
.fa-summary span { color: #6a8094; }
.fa-summary strong { color: #173454; text-align: right; }
.fa-tips { display: grid; gap: 10px; color: #855f11; }
.fa-tips p { margin: 0; border-radius: 14px; background: linear-gradient(135deg, rgba(255,246,228,.95), rgba(250,237,205,.95)); padding: 10px 12px; }
.preview-card { margin-bottom: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.form-grid.one-col { grid-template-columns: 1fr; }
.full, .full :deep(.el-input__wrapper) { width: 100%; }
:deep(.el-card__header) { border-bottom: none; padding-bottom: 0; }
:deep(.el-card__body) { padding-top: 16px; }
:deep(.el-table) { border-radius: 16px; overflow: hidden; }
@media (max-width: 1280px) { .fa-header-card, .fa-grid.two-col { grid-template-columns: 1fr; } .fa-filter-card { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
@media (max-width: 900px) { .fa-filter-card, .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .fa-filter-card .search { grid-column: span 2; } }
@media (max-width: 640px) { .fa-page { padding: 12px; } .fa-filter-card, .form-grid, .fa-chips { grid-template-columns: 1fr; } .fa-filter-card .search { grid-column: span 1; } }
</style>
