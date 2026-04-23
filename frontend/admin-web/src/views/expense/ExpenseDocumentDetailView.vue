<template>
  <div
    v-if="isPrintMode"
    class="expense-print-page expense-print-page--detail"
    data-testid="detail-print-mode"
  >
    <div v-loading="detailLoading || printLoading" class="expense-print-page__state">
      <ExpenseDocumentPrintSheet
        v-if="detail && !printLoadError"
        :detail="detail"
        :expense-details="printExpenseDetails"
        :vendor-option-map="vendorOptionMap"
        :payee-option-map="payeeOptionMap"
        :payee-account-option-map="payeeAccountOptionMap"
      />
      <el-empty v-else :description="printLoadError || detailLoadError || '暂无可打印单据数据'" :image-size="96" />
    </div>
  </div>
  <div v-else class="expense-wb-page expense-wb-page--detail detail-page space-y-6">
    <section class="expense-wb-hero detail-hero" data-testid="detail-hero">
      <div class="expense-wb-hero__content detail-hero__content">
        <div class="detail-hero__main">
          <button type="button" class="expense-wb-backlink detail-hero__backlink" data-testid="detail-back-button" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </button>
          <h1 class="expense-wb-hero__title detail-hero__title">{{ detail?.documentTitle || route.params.documentCode }}</h1>
        </div>

        <div class="detail-hero__amount" data-testid="detail-hero-amount">
          <span class="detail-hero__amount-label">金额</span>
          <strong class="detail-hero__amount-value">{{ amountText }}</strong>
        </div>
      </div>
    </section>

    <div v-loading="detailLoading" class="detail-layout grid grid-cols-1 gap-6 xl:grid-cols-[3fr_1fr]">
      <template v-if="detail">
        <div class="detail-main-scroll space-y-6" data-testid="detail-main-scroll">
          <el-card class="expense-wb-panel">
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">单据表单</p>
                  <p class="mt-1 text-sm text-slate-500">根据提交时保存的表单快照回看单据内容。</p>
                </div>
                <el-tag effect="plain">金额：{{ amountText }}</el-tag>
              </div>
            </template>

            <ExpenseFormReadonlyRenderer
              v-if="detail"
              :schema="detail.formSchemaSnapshot"
              :form-data="detail.formData"
              :company-options="detail.companyOptions"
              :department-options="detail.departmentOptions"
              :vendor-option-map="vendorOptionMap"
              :payee-option-map="payeeOptionMap"
              :payee-account-option-map="payeeAccountOptionMap"
            />
            <el-empty v-else description="暂无单据数据" :image-size="96" />
          </el-card>

          <el-card class="expense-wb-panel" data-testid="related-bindings-card">
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">{{ relatedCardTitle }}</p>
                  <p class="mt-1 text-sm text-slate-500">{{ relatedCardDescription }}</p>
                </div>
                <div class="binding-card-header-actions">
                  <el-tag effect="plain">{{ relatedDocumentBindings.length }} {{ bindingCountSuffix }}</el-tag>
                  <button
                    type="button"
                    class="binding-card-toggle"
                    data-testid="related-bindings-toggle"
                    @click="relatedBindingsExpanded = !relatedBindingsExpanded"
                  >
                    {{ relatedBindingsExpanded ? collapseText : expandText }}
                  </button>
                </div>
              </div>
            </template>

            <div v-if="relatedBindingsExpanded" class="space-y-5">
              <div class="space-y-3">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-slate-800">{{ relatedOutboundTitle }}</p>
                  <el-tag size="small" effect="plain">{{ outboundRelatedBindings.length }} {{ bindingCountSuffix }}</el-tag>
                </div>
                <div v-if="outboundRelatedBindings.length" class="space-y-3">
                  <div
                    v-for="item in outboundRelatedBindings"
                    :key="`related-outbound-${item.fieldKey || 'field'}-${item.documentCode}`"
                    class="expense-wb-detail-card"
                    data-testid="related-binding-item"
                  >
                    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <div class="space-y-2">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-base font-semibold text-slate-800">{{ item.documentTitle || item.documentCode }}</p>
                          <el-tag size="small" effect="plain">{{ item.templateTypeLabel || businessDocumentLabel }}</el-tag>
                          <el-tag v-if="item.statusLabel" size="small" effect="plain">{{ item.statusLabel }}</el-tag>
                        </div>
                        <p class="text-sm text-slate-500">
                          {{ documentCodeLabel }}{{ item.documentCode }} {{ bindingInlineSeparator }} {{ submitterLabel }}{{ item.submitterName || '-' }}
                        </p>
                        <p class="text-xs leading-6 text-slate-500">
                          {{ sourceFieldLabel }}{{ item.fieldKey || '-' }}
                        </p>
                      </div>
                      <div class="expense-wb-compact-actions">
                        <el-button
                          plain
                          :data-testid="`open-bound-document-${item.documentCode}`"
                          @click="openBoundDocument(item.documentCode)"
                        >
                          {{ viewBoundDocumentLabel }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="binding-card-inline-empty" data-testid="related-outbound-empty">{{ relatedOutboundEmptyText }}</p>
              </div>

              <div class="space-y-3">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-slate-800">{{ relatedInboundTitle }}</p>
                  <el-tag size="small" effect="plain">{{ inboundRelatedBindings.length }} {{ bindingCountSuffix }}</el-tag>
                </div>
                <div v-if="inboundRelatedBindings.length" class="space-y-3">
                  <div
                    v-for="item in inboundRelatedBindings"
                    :key="`related-inbound-${item.fieldKey || 'field'}-${item.documentCode}`"
                    class="expense-wb-detail-card"
                    data-testid="related-binding-item"
                  >
                    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <div class="space-y-2">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-base font-semibold text-slate-800">{{ item.documentTitle || item.documentCode }}</p>
                          <el-tag size="small" effect="plain">{{ item.templateTypeLabel || businessDocumentLabel }}</el-tag>
                          <el-tag v-if="item.statusLabel" size="small" effect="plain">{{ item.statusLabel }}</el-tag>
                        </div>
                        <p class="text-sm text-slate-500">
                          {{ documentCodeLabel }}{{ item.documentCode }} {{ bindingInlineSeparator }} {{ submitterLabel }}{{ item.submitterName || '-' }}
                        </p>
                        <p class="text-xs leading-6 text-slate-500">
                          {{ bindingFieldLabel }}{{ item.fieldKey || '-' }}
                        </p>
                      </div>
                      <div class="expense-wb-compact-actions">
                        <el-button
                          plain
                          :data-testid="`open-bound-document-${item.documentCode}`"
                          @click="openBoundDocument(item.documentCode)"
                        >
                          {{ viewBoundDocumentLabel }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="binding-card-inline-empty" data-testid="related-inbound-empty">{{ relatedInboundEmptyText }}</p>
              </div>
            </div>
          </el-card>

          <el-card class="expense-wb-panel" data-testid="writeoff-bindings-card">
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">{{ writeOffCardTitle }}</p>
                  <p class="mt-1 text-sm text-slate-500">{{ writeOffCardDescription }}</p>
                </div>
                <div class="binding-card-header-actions">
                  <el-tag effect="plain">{{ writeOffDocumentBindings.length }} {{ bindingCountSuffix }}</el-tag>
                  <button
                    type="button"
                    class="binding-card-toggle"
                    data-testid="writeoff-bindings-toggle"
                    @click="writeOffBindingsExpanded = !writeOffBindingsExpanded"
                  >
                    {{ writeOffBindingsExpanded ? collapseText : expandText }}
                  </button>
                </div>
              </div>
            </template>

            <div v-if="writeOffBindingsExpanded" class="space-y-5">
              <div class="space-y-3">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-slate-800">{{ writeOffOutboundTitle }}</p>
                  <el-tag size="small" effect="plain">{{ outboundWriteOffBindings.length }} {{ bindingCountSuffix }}</el-tag>
                </div>
                <div v-if="outboundWriteOffBindings.length" class="space-y-3">
                  <div
                    v-for="item in outboundWriteOffBindings"
                    :key="`writeoff-outbound-${item.fieldKey || 'field'}-${item.documentCode}`"
                    class="expense-wb-detail-card"
                    data-testid="writeoff-binding-item"
                  >
                    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <div class="space-y-2">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-base font-semibold text-slate-800">{{ item.documentTitle || item.documentCode }}</p>
                          <el-tag size="small" effect="plain">{{ item.templateTypeLabel || businessDocumentLabel }}</el-tag>
                          <el-tag size="small" effect="plain">{{ item.effectiveStatusLabel || unknownStatusLabel }}</el-tag>
                        </div>
                        <p class="text-sm text-slate-500">
                          {{ documentCodeLabel }}{{ item.documentCode }} {{ bindingInlineSeparator }} {{ writeOffSourceLabel }}{{ writeOffSourceKindLabel(item.writeOffSourceKind) }}
                        </p>
                        <p class="text-xs leading-6 text-slate-500">
                          {{ requestedAmountLabel }}{{ formatBindingMoney(item.requestedAmount) }} {{ bindingInlineSeparator }} {{ effectiveAmountLabel }}{{ formatBindingMoney(item.effectiveAmount) }} {{ bindingInlineSeparator }} {{ remainingAmountLabel }}{{ formatBindingMoney(item.remainingAmount) }}
                        </p>
                      </div>
                      <div class="expense-wb-compact-actions">
                        <el-button
                          plain
                          :data-testid="`open-bound-document-${item.documentCode}`"
                          @click="openBoundDocument(item.documentCode)"
                        >
                          {{ viewBoundDocumentLabel }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="binding-card-inline-empty" data-testid="writeoff-outbound-empty">{{ writeOffOutboundEmptyText }}</p>
              </div>

              <div class="space-y-3">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-slate-800">{{ writeOffInboundTitle }}</p>
                  <el-tag size="small" effect="plain">{{ inboundWriteOffBindings.length }} {{ bindingCountSuffix }}</el-tag>
                </div>
                <div v-if="inboundWriteOffBindings.length" class="space-y-3">
                  <div
                    v-for="item in inboundWriteOffBindings"
                    :key="`writeoff-inbound-${item.fieldKey || 'field'}-${item.documentCode}`"
                    class="expense-wb-detail-card"
                    data-testid="writeoff-binding-item"
                  >
                    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <div class="space-y-2">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-base font-semibold text-slate-800">{{ item.documentTitle || item.documentCode }}</p>
                          <el-tag size="small" effect="plain">{{ item.templateTypeLabel || businessDocumentLabel }}</el-tag>
                          <el-tag size="small" effect="plain">{{ item.effectiveStatusLabel || unknownStatusLabel }}</el-tag>
                        </div>
                        <p class="text-sm text-slate-500">
                          {{ documentCodeLabel }}{{ item.documentCode }} {{ bindingInlineSeparator }} {{ writeOffSourceLabel }}{{ writeOffSourceKindLabel(item.writeOffSourceKind) }}
                        </p>
                        <p class="text-xs leading-6 text-slate-500">
                          {{ requestedAmountLabel }}{{ formatBindingMoney(item.requestedAmount) }} {{ bindingInlineSeparator }} {{ effectiveAmountLabel }}{{ formatBindingMoney(item.effectiveAmount) }} {{ bindingInlineSeparator }} {{ remainingAmountLabel }}{{ formatBindingMoney(item.remainingAmount) }}
                        </p>
                      </div>
                      <div class="expense-wb-compact-actions">
                        <el-button
                          plain
                          :data-testid="`open-bound-document-${item.documentCode}`"
                          @click="openBoundDocument(item.documentCode)"
                        >
                          {{ viewBoundDocumentLabel }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="binding-card-inline-empty" data-testid="writeoff-inbound-empty">{{ writeOffInboundEmptyText }}</p>
              </div>
            </div>
          </el-card>

          <el-card v-if="detail?.expenseDetails?.length" class="expense-wb-panel">
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">费用明细</p>
                  <p class="mt-1 text-sm text-slate-500">这里展示随单据一并提交并归档的费用明细快照，点击任一明细可在当前页展开其发票工作区。</p>
                </div>
                <el-tag effect="plain">{{ detail?.expenseDetails?.length || 0 }} 条</el-tag>
              </div>
            </template>

            <div class="space-y-4">
              <div
                v-for="item in detail?.expenseDetails || []"
                :key="item.detailNo"
                class="expense-wb-detail-card expense-wb-detail-card--clickable"
                :class="{ 'expense-wb-detail-card--selected': activeExpenseDetailNo === item.detailNo }"
                data-testid="expense-detail-card"
                @click="selectExpenseDetail(item.detailNo)"
              >
                <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-base font-semibold text-slate-800">{{ item.detailTitle || item.detailNo }}</p>
                      <el-tag effect="plain">
                        {{ resolveExpenseDetailTypeLabel(item.detailType, item.detailTypeLabel) }}
                      </el-tag>
                      <el-tag v-if="item.enterpriseModeLabel" type="warning" effect="plain">{{ item.enterpriseModeLabel }}</el-tag>
                      <el-tag v-if="activeExpenseDetailNo === item.detailNo" type="primary" effect="plain">发票工作区已展开</el-tag>
                    </div>
                    <p class="mt-2 text-sm text-slate-500">
                      明细编号：{{ item.detailNo }} ｜ 排序：{{ item.sortOrder || '-' }} ｜ 创建时间：{{ item.createdAt || '-' }}
                    </p>
                  </div>

                  <div class="expense-wb-compact-actions">
                    <el-button plain @click.stop="selectExpenseDetail(item.detailNo)">查看发票</el-button>
                    <el-button plain @click.stop="openExpenseDetail(item.detailNo)">查看明细</el-button>
                  </div>
                </div>
              </div>

              <div v-if="activeExpenseDetailNo" class="expense-document-invoice-shell">
                <div class="expense-wb-summary-strip">
                  <div class="expense-wb-summary-grid">
                    <div class="expense-wb-summary-item">
                      <span class="expense-wb-summary-item__label">当前明细</span>
                      <span class="expense-wb-summary-item__value">{{ activeExpenseDetail?.detailTitle || activeExpenseDetailSummary?.detailTitle || activeExpenseDetailNo }}</span>
                    </div>
                    <div class="expense-wb-summary-item">
                      <span class="expense-wb-summary-item__label">明细编号</span>
                      <span class="expense-wb-summary-item__value">{{ activeExpenseDetail?.detailNo || activeExpenseDetailNo }}</span>
                    </div>
                    <div class="expense-wb-summary-item">
                      <span class="expense-wb-summary-item__label">加载状态</span>
                      <span class="expense-wb-summary-item__value">
                        {{
                          expenseDetailLoadingNo === activeExpenseDetailNo && !activeExpenseDetail
                            ? '加载中'
                            : activeExpenseDetailError
                              ? '加载失败'
                              : '已就绪'
                        }}
                      </span>
                    </div>
                  </div>
                </div>

                <div class="mt-6">
                  <ExpenseInvoiceWorkbench
                    :schema="activeExpenseDetail?.schemaSnapshot || emptyExpenseDetailSchema"
                    :form-data="activeExpenseDetail?.formData || {}"
                    :detail-title="activeExpenseDetail?.detailTitle || activeExpenseDetailSummary?.detailTitle || ''"
                    :detail-no="activeExpenseDetail?.detailNo || activeExpenseDetailNo"
                    :loading="expenseDetailLoadingNo === activeExpenseDetailNo && !activeExpenseDetail"
                    :error-message="activeExpenseDetailError"
                    result-mode="verification-placeholder"
                  />
                </div>
              </div>
            </div>
          </el-card>

          <el-card
            v-if="detail?.bankPayment || detail?.bankReceipts?.length"
            class="expense-wb-panel"
            data-testid="detail-bank-section"
          >
            <template #header>
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-lg font-semibold text-slate-800">银行付款 / 银行回单</p>
                  <p class="mt-1 text-sm text-slate-500">这里展示银企直连付款状态，以及已回传到单据里的银行回单附件。</p>
                </div>
                <el-tag effect="plain">{{ detail?.bankPayment?.paymentStatusLabel || '暂无状态' }}</el-tag>
              </div>
            </template>

            <div class="space-y-5">
              <div v-if="detail?.bankPayment" class="expense-wb-summary-strip">
                <div class="expense-wb-summary-grid">
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">付款状态</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.paymentStatusLabel || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">直连账户</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.companyBankAccountName || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">回单状态</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.receiptStatusLabel || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">支付时间</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.paidAt || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">银行流水号</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.bankFlowNo || '-' }}</span>
                  </div>
                  <div class="expense-wb-summary-item">
                    <span class="expense-wb-summary-item__label">支付方式</span>
                    <span class="expense-wb-summary-item__value">{{ detail.bankPayment.manualPaid ? '手动支付' : '银行回调' }}</span>
                  </div>
                </div>
              </div>

              <div>
                <div class="mb-3 flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-slate-800">银行回单</p>
                  <el-tag size="small" effect="plain">{{ detail?.bankReceipts?.length || 0 }} 份</el-tag>
                </div>
                <div v-if="detail?.bankReceipts?.length" class="space-y-3">
                  <div
                    v-for="receipt in detail.bankReceipts"
                    :key="receipt.attachmentId || receipt.fileName"
                    class="expense-wb-detail-card"
                  >
                    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                      <div class="space-y-2">
                        <div class="flex flex-wrap items-center gap-2">
                          <p class="text-base font-semibold text-slate-800">{{ receipt.fileName }}</p>
                          <el-tag effect="plain">{{ receipt.receivedAt || '待生成' }}</el-tag>
                        </div>
                        <p class="text-sm text-slate-500">
                          {{ receipt.contentType || '未知类型' }} · {{ formatAttachmentSize(receipt.fileSize) }}
                        </p>
                      </div>
                      <div class="expense-wb-compact-actions">
                        <el-button
                          v-if="receipt.previewUrl"
                          plain
                          tag="a"
                          target="_blank"
                          :href="buildAuthorizedAttachmentPreviewUrl(receipt.previewUrl)"
                        >
                          预览回单
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="暂无银行回单" :image-size="72" />
              </div>
            </div>
          </el-card>
        </div>

        <el-card class="expense-wb-panel">
          <template #header>
            <div>
              <p class="text-lg font-semibold text-slate-800">审批流程</p>
              <p class="mt-1 text-sm text-slate-500">真实任务状态与审批轨迹</p>
            </div>
          </template>

          <div class="approval-scroll space-y-5">
            <div class="expense-wb-summary-strip">
              <div class="expense-wb-summary-grid">
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">当前节点</span>
                  <span class="expense-wb-summary-item__value">{{ detail.currentNodeName || '未开始' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">模板名称</span>
                  <span class="expense-wb-summary-item__value">{{ detail.templateName || '-' }}</span>
                </div>
                <div class="expense-wb-summary-item">
                  <span class="expense-wb-summary-item__label">当前状态</span>
                  <span class="expense-wb-summary-item__value">{{ detail.statusLabel || '-' }}</span>
                </div>
              </div>
            </div>

            <div
              v-if="isManualApproverSelectionPending"
              class="rounded-[24px] border border-amber-200 bg-amber-50 p-5 space-y-4"
              data-testid="manual-approver-selection-card"
            >
              <div class="space-y-1">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="text-sm font-semibold text-slate-800">当前节点手动选择审批人</p>
                  <el-tag size="small" type="warning" effect="plain">待处理</el-tag>
                </div>
                <p class="text-sm text-slate-600">
                  当前流程停留在“{{ detail.manualApproverSelectionNodeName || detail.manualApproverSelectionNodeKey }}”节点，
                  需要由提单人指定本节点审批人后继续流转。
                </p>
              </div>
              <template v-if="canSubmitManualApproverSelection">
                <el-select
                  v-model="manualApproverForm.userIds"
                  class="w-full"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  filterable
                  clearable
                  placeholder="请选择当前节点审批人"
                  data-testid="manual-approver-selection-select"
                >
                  <el-option
                    v-for="item in manualApproverOptions"
                    :key="String(item.value || '')"
                    :label="item.label"
                    :value="Number(item.value)"
                  />
                </el-select>
                <div class="flex justify-end">
                  <el-button
                    type="primary"
                    :loading="manualApproverSubmitting"
                    @click="submitManualApproverSelection"
                  >
                    提交审批人
                  </el-button>
                </div>
              </template>
              <p v-else class="text-xs leading-6 text-slate-500">
                当前节点等待提单人完成手动选人；你可查看全流程轨迹，但不能代为提交。
              </p>
            </div>

            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <p class="text-sm font-semibold text-slate-800">&#30495;&#23454;&#20219;&#21153;&#29366;&#24577;</p>
                <el-tag size="small" effect="plain">{{ approvalNodeStatuses.length }} &#26465;</el-tag>
              </div>

              <div
                v-if="approvalNodeStatuses.length"
                class="approval-node-status-list"
                data-testid="approval-node-status-list"
              >
                <div
                  v-for="item in approvalNodeStatuses"
                  :key="item.nodeKey"
                  class="approval-node-status-card"
                  :class="{
                    'approval-node-status-card--pending': item.status === 'PENDING' || item.status === 'PAYMENT_PENDING' || item.status === 'MANUAL_SELECTION_PENDING',
                    'approval-node-status-card--future': item.status === 'NOT_REACHED'
                  }"
                  data-testid="approval-node-status-item"
                >
                  <div class="approval-node-status-card__content">
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-sm font-semibold text-slate-800">{{ item.nodeName || item.nodeKey }}</p>
                      <el-tag size="small" effect="plain" :type="approvalStatusTagType(item.status)">
                        {{ item.statusLabel || approvalStatusLabel(item.status) }}
                      </el-tag>
                    </div>
                    <p v-if="item.description" class="text-xs leading-6 text-slate-500">{{ item.description }}</p>
                    <p v-else-if="item.assigneeNames?.length" class="text-xs leading-6 text-slate-500">
                      &#22788;&#29702;&#20154;&#65306;{{ item.assigneeNames.join('\u3001') }}
                    </p>
                  </div>
                  <span v-if="item.occurredAt" class="approval-node-status-card__time">{{ item.occurredAt }}</span>
                </div>
              </div>
              <el-empty v-else description="&#26242;&#26080;&#30495;&#23454;&#20219;&#21153;&#29366;&#24577;" :image-size="72" />
            </div>

            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <p class="text-sm font-semibold text-slate-800">&#23457;&#25209;&#36712;&#36857;</p>
                <el-tag size="small" effect="plain">{{ approvalTimelineItems.length }} &#26465;</el-tag>
              </div>

              <el-timeline v-if="approvalTimelineItems.length" data-testid="approval-timeline-list">
                <el-timeline-item
                  v-for="item in approvalTimelineItems"
                  :key="item.key"
                  :timestamp="item.timestamp"
                  placement="top"
                  data-testid="approval-timeline-item"
                >
                  <div class="space-y-2">
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-sm font-semibold text-slate-800">{{ item.title }}</p>
                      <el-tag v-if="item.statusLabel" size="small" effect="plain" :type="approvalStatusTagType(item.status)">
                        {{ item.statusLabel }}
                      </el-tag>
                    </div>
                    <p v-if="item.description" class="text-xs leading-6 text-slate-500">{{ item.description }}</p>
                    <div v-if="item.attachmentNames?.length" class="flex flex-wrap gap-2">
                      <el-tag
                        v-for="name in item.attachmentNames || []"
                        :key="name"
                        size="small"
                        effect="plain"
                        type="info"
                      >
                        {{ name }}
                      </el-tag>
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="&#26242;&#26080;&#23457;&#25209;&#36712;&#36857;" :image-size="72" />
            </div>
          </div>
        </el-card>
      </template>

      <el-card v-else class="expense-wb-panel xl:col-span-2">
        <el-empty :description="detailLoadError || '暂无单据数据'" :image-size="96" />
      </el-card>
    </div>

    <div v-if="actionItems.length" class="detail-floating-bar">
      <div class="detail-floating-inner">
        <p v-if="disabledActionHint" class="detail-floating-hint">{{ disabledActionHint }}</p>
        <div class="detail-floating-actions" data-testid="detail-floating-actions">
          <div
            v-if="secondaryActionItems.length"
            class="detail-floating-actions__group detail-floating-actions__group--secondary"
            data-testid="detail-floating-secondary-actions"
          >
            <el-button
              v-for="action in secondaryActionItems"
              :key="action.key"
              :type="action.primary ? action.type || 'primary' : undefined"
              :plain="!action.primary"
              :disabled="action.disabled"
              class="detail-floating-button"
              @click="handleActionClick(action)"
            >
              {{ action.label }}
            </el-button>
          </div>
          <div
            v-if="primaryActionItems.length"
            class="detail-floating-actions__group detail-floating-actions__group--primary"
            data-testid="detail-floating-primary-actions"
          >
            <el-button
              v-for="action in primaryActionItems"
              :key="action.key"
              :type="action.primary ? action.type || 'primary' : undefined"
              :plain="!action.primary"
              :disabled="action.disabled"
              class="detail-floating-button"
              :class="{
                'detail-floating-button--colored': action.primary,
                'detail-floating-button--approve': action.key === 'approve'
              }"
              @click="handleActionClick(action)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="commentDialogVisible" title="发表评论" width="560px">
      <div class="space-y-4">
        <el-input
          v-model="commentForm.comment"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="输入评论内容"
        />
        <div class="space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div class="flex flex-wrap items-center gap-3">
            <el-button plain @click="pickCommentFiles">添加附件名</el-button>
            <p class="text-xs text-slate-500">本次只保存附件文件名，不上传真实文件内容。</p>
          </div>
          <div v-if="commentForm.attachmentFileNames.length" class="flex flex-wrap gap-2">
            <el-tag
              v-for="name in commentForm.attachmentFileNames"
              :key="name"
              closable
              effect="plain"
              @close="removeCommentAttachment(name)"
            >
              {{ name }}
            </el-tag>
          </div>
          <el-empty v-else description="暂未添加附件名" :image-size="60" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="commentDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="commentSubmitting" @click="submitComment">发表评论</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="taskActionDialogVisible" :title="taskActionDialogTitle" width="560px">
      <div class="space-y-4">
        <el-input
          v-model="taskActionForm.comment"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          :placeholder="taskActionDialogPlaceholder"
        />
        <el-form-item v-if="taskActionMode === 'reject' && rejectTargetOptions.length" label="驳回到节点" class="!mb-0">
          <el-select v-model="taskActionForm.targetNodeKey" class="w-full" clearable placeholder="请选择目标审批节点">
            <el-option
              v-for="node in rejectTargetOptions"
              :key="node.nodeKey"
              :label="node.optionLabel || node.nodeName || node.nodeKey"
              :value="node.nodeKey"
            />
          </el-select>
        </el-form-item>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeTaskActionDialog">取消</el-button>
          <el-button type="primary" :loading="taskActionSubmitting" @click="submitTaskAction">{{ taskActionDialogConfirm }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="userActionDialogVisible" :title="userActionDialogTitle" width="520px">
      <div class="space-y-4">
        <el-form-item :label="userActionDialogLabel" required>
          <el-select
            v-model="userActionForm.targetUserId"
            class="w-full"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="搜索并选择处理人"
            :remote-method="loadActionUsers"
            :loading="userOptionsLoading"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.userId"
              :label="item.deptName ? `${item.name}（${item.deptName}）` : item.name"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="userActionForm.remark"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            :placeholder="userActionDialogPlaceholder"
          />
        </el-form-item>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="closeUserActionDialog">取消</el-button>
          <el-button type="primary" :loading="userActionSubmitting" @click="submitUserAction">
            {{ userActionDialogConfirm }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <input
      ref="commentFileInput"
      type="file"
      class="hidden"
      multiple
      @change="handleCommentFileChange"
    >
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  expenseApi,
  expenseApprovalApi,
  type ExpenseActionUserOption,
  type ExpenseApprovalNodeStatus,
  type ExpenseApprovalTimelineItem,
  type ExpenseDetailInstanceDetail,
  type ExpenseDocumentDetail,
  type ExpenseDocumentNavigation,
  type ProcessFlowNode,
  type ProcessFormDesignSchema,
  type ProcessFormOption
} from '@/api'
import type { ExpenseDocumentRelationBinding, ExpenseDocumentWriteOffBinding } from '@/api/modules/expense-types'
import ExpenseFormReadonlyRenderer from './components/ExpenseFormReadonlyRenderer.vue'
import ExpenseInvoiceWorkbench from './components/ExpenseInvoiceWorkbench.vue'
import ExpenseDocumentPrintSheet from './components/ExpenseDocumentPrintSheet.vue'
import { buildAuthorizedAttachmentPreviewUrl } from './expenseInvoicePreview'
import { useReadonlyPayeeLookups } from './useReadonlyPayeeLookups'
import {
  resolveDisabledExpenseDetailActionHint,
  resolveExpenseDetailActions,
  type ExpenseDetailActionItem as ActionItem,
  type ExpenseDetailActionKey as ActionKey
} from './expenseDetailActionMatrix'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { formatMoney } from '@/utils/money'
import { buildExpenseDetailPrintHref, isExpenseDetailPrintMode, loadExpenseDocumentPrintBundle, openExpensePrintWindow } from './expensePrintSupport'

type UserActionMode = 'transfer' | 'add-sign' | ''
type TaskActionMode = 'approve' | 'reject' | ''

type RejectTargetOption = {
  nodeKey: string
  nodeName: string
  optionLabel: string
  isSubmitter?: boolean
}

const route = useRoute()
const router = useRouter()
const detailLoading = ref(false)
const navigationLoading = ref(false)
const detail = ref<ExpenseDocumentDetail | null>(null)
const relatedBindingsExpanded = ref(false)
const writeOffBindingsExpanded = ref(false)
const detailLoadError = ref('')
const printLoading = ref(false)
const printLoadError = ref('')
const printExpenseDetails = ref<ExpenseDetailInstanceDetail[]>([])
const navigation = ref<ExpenseDocumentNavigation>({})
const activeExpenseDetailNo = ref('')
const expenseDetailLoadingNo = ref('')
const expenseDetailCache = ref<Record<string, ExpenseDetailInstanceDetail>>({})
const expenseDetailErrors = ref<Record<string, string>>({})
const { vendorOptionMap, payeeOptionMap, payeeAccountOptionMap, syncReadonlyPayeeLookups, syncReadonlyPayeeLookupsBatch } = useReadonlyPayeeLookups()
const storedUser = (readStoredUser() || {}) as { userId?: number; permissionCodes?: string[] }
const commentDialogVisible = ref(false)
const commentSubmitting = ref(false)
const commentFileInput = ref<HTMLInputElement | null>(null)
const commentForm = ref({
  comment: '',
  attachmentFileNames: [] as string[]
})
const taskActionDialogVisible = ref(false)
const taskActionMode = ref<TaskActionMode>('')
const taskActionSubmitting = ref(false)
const taskActionForm = ref({
  comment: '',
  targetNodeKey: ''
})
const manualApproverSubmitting = ref(false)
const manualApproverForm = ref({
  userIds: [] as number[]
})
const userActionDialogVisible = ref(false)
const userActionMode = ref<UserActionMode>('')
const userActionSubmitting = ref(false)
const userOptionsLoading = ref(false)
const userOptions = ref<ExpenseActionUserOption[]>([])
const userActionForm = ref({
  targetUserId: undefined as number | undefined,
  remark: ''
})
const emptyExpenseDetailSchema: ProcessFormDesignSchema = { layoutMode: 'TWO_COLUMN', blocks: [] }
let detailRequestVersion = 0
let navigationRequestVersion = 0
let lastPrintedDocumentCode = ''

const amountText = computed(() => `¥ ${formatDetailMoney(detail.value?.totalAmount)}`)
const isPrintMode = computed(() => isExpenseDetailPrintMode(route.query))
const activeExpenseDetail = computed(() => (
  activeExpenseDetailNo.value ? expenseDetailCache.value[activeExpenseDetailNo.value] || null : null
))
const activeExpenseDetailError = computed(() => (
  activeExpenseDetailNo.value ? expenseDetailErrors.value[activeExpenseDetailNo.value] || '' : ''
))
const activeExpenseDetailSummary = computed(() => (
  detail.value?.expenseDetails?.find((item) => item.detailNo === activeExpenseDetailNo.value) || null
))
const currentUserId = computed(() => Number(storedUser.userId || 0))
const permissionCodes = computed(() => storedUser.permissionCodes || [])
const approvableTasks = computed(() =>
  (detail.value?.currentTasks || []).filter((task) => task.assigneeUserId === currentUserId.value && task.nodeType === 'APPROVAL')
)
const currentApprovalNode = computed<ProcessFlowNode | null>(() => {
  const taskNodeKey = approvableTasks.value[0]?.nodeKey || detail.value?.currentNodeKey || ''
  if (!taskNodeKey) {
    return null
  }
  const flowNodes = Array.isArray(detail.value?.flowSnapshot?.nodes) ? detail.value?.flowSnapshot?.nodes || [] : []
  return flowNodes.find((node) => node.nodeKey === taskNodeKey) || null
})
const currentApprovalSpecialSettings = computed(() => new Set(
  Array.isArray(currentApprovalNode.value?.config?.specialSettings)
    ? currentApprovalNode.value?.config?.specialSettings || []
    : []
))
const canModifyCurrentTask = computed(() => (
  currentApprovalSpecialSettings.value.has('ALLOW_EDIT_FORM_MODULE')
  || currentApprovalSpecialSettings.value.has('ALLOW_EDIT_PAY_ACCOUNT')
))
const rejectTargetOptions = computed<RejectTargetOption[]>(() => {
  if (!currentApprovalSpecialSettings.value.has('REJECT_TO_ANY_NODE')) {
    return []
  }
  const currentNodeKey = currentApprovalNode.value?.nodeKey || detail.value?.currentNodeKey || ''
  const options: RejectTargetOption[] = [
    {
      nodeKey: '__SUBMITTER__',
      nodeName: '驳回到提单人',
      optionLabel: formatRejectTargetLabel('驳回到提单人', detail.value?.submitterName),
      isSubmitter: true
    }
  ]
  const upstreamApprovalNodes = (detail.value?.approvalNodeStatuses || [])
    .filter((item) =>
      item.nodeType === 'APPROVAL'
      && item.nodeKey !== currentNodeKey
      && item.status !== 'NOT_REACHED'
      && item.status !== 'PENDING'
      && item.status !== 'MANUAL_SELECTION_PENDING'
    )
    .map((item) => ({
      nodeKey: item.nodeKey,
      nodeName: item.nodeName || item.nodeKey,
      optionLabel: formatRejectTargetLabel(item.nodeName || item.nodeKey, item.assigneeNames)
    }))
  return [...options, ...upstreamApprovalNodes]
})
function formatRejectTargetLabel(nodeName: string, assigneeNames?: string[] | string) {
  const names = Array.isArray(assigneeNames)
    ? assigneeNames.filter((item) => Boolean(String(item || '').trim()))
    : [String(assigneeNames || '').trim()].filter(Boolean)
  if (!names.length) {
    return nodeName
  }
  return `${nodeName}（${names.join('、')}）`
}
const canApprovalView = computed(() =>
  hasPermission('expense:approval:view', permissionCodes.value)
  || hasPermission('expense:approval:approve', permissionCodes.value)
  || hasPermission('expense:approval:reject', permissionCodes.value)
)
const isSubmitter = computed(() => detail.value?.submitterUserId === currentUserId.value)
const isManualApproverSelectionPending = computed(() => Boolean(detail.value?.manualApproverSelectionPending))
const canSubmitManualApproverSelection = computed(() => isSubmitter.value && isManualApproverSelectionPending.value)
const manualApproverOptions = computed<ProcessFormOption[]>(() => detail.value?.manualApproverOptions || [])
const isActiveApprover = computed(() => approvableTasks.value.length > 0)
const canResubmitEdit = computed(() => {
  const status = detail.value?.status || ''
  return isSubmitter.value && (status === 'DRAFT' || status === 'REJECTED')
})
const isFlowParticipant = computed(() => {
  if (!detail.value) {
    return false
  }
  if (isSubmitter.value || isActiveApprover.value) {
    return true
  }
  const userId = currentUserId.value
  return detail.value.actionLogs.some((log) => {
    if (log.actorUserId === userId) {
      return true
    }
    const approverUserIds = Array.isArray(log.payload?.approverUserIds) ? log.payload.approverUserIds : []
    return approverUserIds.some((item) => Number(item) === userId)
  })
})
const canComment = computed(() => isSubmitter.value || isFlowParticipant.value)
const relatedDocumentBindings = computed<ExpenseDocumentRelationBinding[]>(() => detail.value?.relatedDocumentBindings || [])
const outboundRelatedBindings = computed<ExpenseDocumentRelationBinding[]>(() => relatedDocumentBindings.value.filter((item) => item.direction === 'OUTBOUND'))
const inboundRelatedBindings = computed<ExpenseDocumentRelationBinding[]>(() => relatedDocumentBindings.value.filter((item) => item.direction === 'INBOUND'))
const writeOffDocumentBindings = computed<ExpenseDocumentWriteOffBinding[]>(() => detail.value?.writeOffDocumentBindings || [])
const outboundWriteOffBindings = computed<ExpenseDocumentWriteOffBinding[]>(() => writeOffDocumentBindings.value.filter((item) => item.direction === 'OUTBOUND'))
const inboundWriteOffBindings = computed<ExpenseDocumentWriteOffBinding[]>(() => writeOffDocumentBindings.value.filter((item) => item.direction === 'INBOUND'))
const bindingCountSuffix = '条'
const bindingInlineSeparator = '\u00b7'
const expandText = '展开'
const collapseText = '收起'
const businessDocumentLabel = '业务单据'
const viewBoundDocumentLabel = '查看单据'
const relatedCardTitle = '关联单据'
const relatedCardDescription = '展示当前单据主动关联与被其它单据反向引用的真实业务关系。'
const relatedOutboundTitle = '当前单据主动关联'
const relatedInboundTitle = '被其它单据关联'
const writeOffCardTitle = '核销单据'
const writeOffCardDescription = '展示当前单据主动核销与被其它单据反向核销的真实金额和生效状态。'
const writeOffOutboundTitle = '当前单据主动核销'
const writeOffInboundTitle = '被其它单据核销'
const documentCodeLabel = '单据编号：'
const submitterLabel = '发起人：'
const sourceFieldLabel = '来源字段：'
const bindingFieldLabel = '关联字段：'
const writeOffSourceLabel = '核销来源：'
const requestedAmountLabel = '请求核销：'
const effectiveAmountLabel = '已生效：'
const remainingAmountLabel = '剩余金额：'
const unknownStatusLabel = '状态未知'
const relatedOutboundEmptyText = '暂无主动关联记录'
const relatedInboundEmptyText = '暂无反向关联记录'
const writeOffOutboundEmptyText = '暂无主动核销记录'
const writeOffInboundEmptyText = '暂无反向核销记录'
const statusBucket = computed<'pending' | 'exception' | 'terminal' | 'other'>(() => {
  const status = detail.value?.status || ''
  if (status === 'PENDING_APPROVAL') {
    return 'pending'
  }
  if (status === 'EXCEPTION') {
    return 'exception'
  }
  if (
    status === 'APPROVED'
    || status === 'COMPLETED'
    || status === 'PAID'
    || status === 'PENDING_PAYMENT'
    || status === 'PAYING'
    || status === 'PAYMENT_COMPLETED'
    || status === 'PAYMENT_FINISHED'
    || status === 'PAYMENT_EXCEPTION'
  ) {
    return 'terminal'
  }
  return 'other'
})

const approvalNodeStatuses = computed<ExpenseApprovalNodeStatus[]>(() => detail.value?.approvalNodeStatuses || [])
const approvalTimelineItems = computed<ExpenseApprovalTimelineItem[]>(() => detail.value?.approvalTimeline || [])
const actionItems = computed<ActionItem[]>(() => {
  if (!detail.value) {
    return []
  }
  return resolveExpenseDetailActions({
    statusBucket: statusBucket.value,
    isSubmitter: isSubmitter.value,
    canResubmitEdit: canResubmitEdit.value,
    isActiveApprover: isActiveApprover.value,
    canModify: canModifyCurrentTask.value,
    isFlowParticipant: isFlowParticipant.value,
    canComment: canComment.value,
    canApprovalView: canApprovalView.value,
    prevDocumentCode: navigation.value.prevDocumentCode,
    nextDocumentCode: navigation.value.nextDocumentCode
  })
})
const taskActionDialogTitle = computed(() => taskActionMode.value === 'approve' ? '通过审批' : '驳回审批')
const taskActionDialogConfirm = computed(() => taskActionMode.value === 'approve' ? '通过' : '驳回')
const taskActionDialogPlaceholder = computed(() => taskActionMode.value === 'approve' ? '请输入审批意见（可空）' : '请输入驳回原因')
const secondaryActionItems = computed(() => actionItems.value.filter((item) => !item.primary))
const primaryActionItems = computed(() => actionItems.value.filter((item) => item.primary))
const disabledActionHint = computed(() => resolveDisabledExpenseDetailActionHint(actionItems.value))
const userActionDialogTitle = computed(() => userActionMode.value === 'transfer' ? '转交审批任务' : '发起前加签')
const userActionDialogLabel = computed(() => userActionMode.value === 'transfer' ? '转交给' : '加签人')
const userActionDialogConfirm = computed(() => userActionMode.value === 'transfer' ? '确认转交' : '确认加签')
const userActionDialogPlaceholder = computed(() => userActionMode.value === 'transfer' ? '可选填写转交说明' : '可选填写加签说明')

watch(
  () => [route.params.documentCode, route.query.print],
  () => {
    void loadDetail()
  },
  { immediate: true }
)

function goBack() {
  void navigateBackWithFallback('/expense/list')
}

function resolveReturnToPath() {
  return typeof route.query.returnTo === 'string' && route.query.returnTo.trim() ? route.query.returnTo.trim() : ''
}

function buildReturnToQuery(extraQuery: Record<string, string> = {}) {
  const returnTo = resolveReturnToPath()
  return returnTo ? { ...extraQuery, returnTo } : extraQuery
}

function buildCurrentPageReturnToQuery(extraQuery: Record<string, string> = {}) {
  return route.fullPath ? { ...extraQuery, returnTo: route.fullPath } : extraQuery
}

async function navigateBackWithFallback(fallbackPath: string) {
  const returnTo = resolveReturnToPath()
  if (returnTo) {
    await router.push(returnTo)
    return
  }
  if (window.history.length > 1) {
    await router.back()
    return
  }
  await router.push(fallbackPath)
}

function openExpenseDetail(detailNo: string) {
  void router.push({
    name: 'expense-document-expense-detail',
    params: {
      documentCode: String(route.params.documentCode || ''),
      detailNo
    },
    query: buildCurrentPageReturnToQuery()
  })
}

function openBoundDocument(documentCode?: string) {
  if (!documentCode) {
    return
  }
  void router.push({
    path: `/expense/documents/${encodeURIComponent(documentCode)}`,
    query: buildCurrentPageReturnToQuery()
  })
}

function syncBindingPanelExpansion(nextDetail?: ExpenseDocumentDetail | null) {
  const source = nextDetail || null
  relatedBindingsExpanded.value = Boolean(source?.relatedDocumentBindings?.length)
  writeOffBindingsExpanded.value = Boolean(source?.writeOffDocumentBindings?.length)
}

async function selectExpenseDetail(detailNo: string) {
  if (!detailNo) {
    return
  }

  if (activeExpenseDetailNo.value === detailNo) {
    activeExpenseDetailNo.value = ''
    return
  }

  activeExpenseDetailNo.value = detailNo
  if (expenseDetailCache.value[detailNo] || expenseDetailLoadingNo.value === detailNo) {
    return
  }

  const nextErrors = { ...expenseDetailErrors.value }
  delete nextErrors[detailNo]
  expenseDetailErrors.value = nextErrors
  expenseDetailLoadingNo.value = detailNo

  try {
    const res = await expenseApi.getExpenseDetail(String(route.params.documentCode || ''), detailNo)
    expenseDetailCache.value = {
      ...expenseDetailCache.value,
      [detailNo]: res.data
    }
  } catch (error: unknown) {
    expenseDetailErrors.value = {
      ...expenseDetailErrors.value,
      [detailNo]: resolveErrorMessage(error, '加载费用明细发票信息失败')
    }
  } finally {
    if (expenseDetailLoadingNo.value === detailNo) {
      expenseDetailLoadingNo.value = ''
    }
  }
}

async function loadDetail() {
  const requestVersion = ++detailRequestVersion
  detailLoading.value = true
  printLoading.value = isPrintMode.value
  navigationRequestVersion += 1
  navigationLoading.value = false
  detailLoadError.value = ''
  printLoadError.value = ''
  detail.value = null
  syncBindingPanelExpansion(null)
  manualApproverForm.value = {
    userIds: []
  }
  printExpenseDetails.value = []
  navigation.value = {}
  activeExpenseDetailNo.value = ''
  expenseDetailLoadingNo.value = ''
  expenseDetailCache.value = {}
  expenseDetailErrors.value = {}
  try {
    const documentCode = String(route.params.documentCode || '')
    if (!documentCode) {
      throw new Error('\u7f3a\u5c11\u5355\u636e\u7f16\u53f7')
    }

    if (isPrintMode.value) {
      const bundle = await loadExpenseDocumentPrintBundle(documentCode)
      if (requestVersion !== detailRequestVersion) {
        return
      }
      detail.value = bundle.detail
      syncBindingPanelExpansion(bundle.detail)
      printExpenseDetails.value = bundle.expenseDetails
      await syncReadonlyPayeeLookupsBatch([
        bundle.detail.formSchemaSnapshot,
        ...bundle.expenseDetails.map((item) => item.schemaSnapshot)
      ])
      await triggerPrint(documentCode)
    } else {
      const res = await expenseApi.getDetail(documentCode)
      if (requestVersion !== detailRequestVersion) {
        return
      }
      detail.value = res.data
      syncBindingPanelExpansion(res.data)
      void syncReadonlyPayeeLookups(res.data.formSchemaSnapshot)
      void loadNavigation(res.data.documentCode, requestVersion)
    }
  } catch (error: unknown) {
    if (requestVersion === detailRequestVersion) {
      const message = resolveErrorMessage(
        error,
        isPrintMode.value ? '\u52a0\u8f7d\u6253\u5370\u6570\u636e\u5931\u8d25' : '\u52a0\u8f7d\u5355\u636e\u8be6\u60c5\u5931\u8d25'
      )
      if (isPrintMode.value) {
        printLoadError.value = message
      } else {
        detailLoadError.value = message
      }
      ElMessage.error(message)
    }
  } finally {
    if (requestVersion === detailRequestVersion) {
      detailLoading.value = false
      printLoading.value = false
    }
  }
}

async function loadNavigation(documentCode: string, requestVersion: number) {
  const navigationVersion = ++navigationRequestVersion
  if (!documentCode || !canApprovalView.value) {
    navigation.value = {}
    navigationLoading.value = false
    return
  }
  navigationLoading.value = true
  try {
    const res = await expenseApi.getNavigation(documentCode)
    if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
      navigation.value = res.data
    }
  } catch {
    if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
      navigation.value = {}
    }
  } finally {
    if (requestVersion === detailRequestVersion && navigationVersion === navigationRequestVersion) {
      navigationLoading.value = false
    }
  }
}

function approvalStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    NOT_REACHED: '未到达',
    PENDING: '审批中',
    MANUAL_SELECTION_PENDING: '待手动选择审批人',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    AUTO_SKIPPED: '已自动跳过',
    EXCEPTION: '异常',
    PAYMENT_PENDING: '待支付',
    PAYMENT_COMPLETED: '已支付',
    PAYMENT_EXCEPTION: '支付异常'
  }
  return labels[status || ''] || '处理中'
}

function approvalStatusTagType(status?: string) {
  switch (status) {
    case 'PENDING':
    case 'PAYMENT_PENDING':
    case 'MANUAL_SELECTION_PENDING':
      return 'warning'
    case 'APPROVED':
    case 'PAYMENT_COMPLETED':
      return 'success'
    case 'REJECTED':
    case 'EXCEPTION':
    case 'PAYMENT_EXCEPTION':
      return 'danger'
    case 'AUTO_SKIPPED':
      return 'info'
    default:
      return 'info'
  }
}

function openTaskActionDialog(action: 'approve' | 'reject') {
  taskActionMode.value = action
  taskActionForm.value = {
    comment: action === 'approve' ? '通过' : '驳回',
    targetNodeKey: action === 'reject' && rejectTargetOptions.value.length ? '__SUBMITTER__' : ''
  }
  taskActionDialogVisible.value = true
}

function closeTaskActionDialog() {
  taskActionDialogVisible.value = false
  taskActionMode.value = ''
  taskActionForm.value = {
    comment: '',
    targetNodeKey: ''
  }
}

async function handleTaskAction(action: 'approve' | 'reject') {
  if (!detail.value || !approvableTasks.value.length) {
    return
  }
  const permissionCode = action === 'approve' ? 'expense:approval:approve' : 'expense:approval:reject'
  if (!hasPermission(permissionCode, permissionCodes.value)) {
    ElMessage.warning('当前账号没有处理该审批的权限')
    return
  }
  openTaskActionDialog(action)
}

async function submitTaskAction() {
  const action = taskActionMode.value
  const task = approvableTasks.value[0]
  if (!action || !task) {
    return
  }
  taskActionSubmitting.value = true
  try {
    const api = action === 'approve' ? expenseApprovalApi.approve : expenseApprovalApi.reject
    const payload = {
      comment: taskActionForm.value.comment || '',
      ...(action === 'reject' && taskActionForm.value.targetNodeKey && taskActionForm.value.targetNodeKey !== '__SUBMITTER__'
        ? { targetNodeKey: taskActionForm.value.targetNodeKey }
        : {})
    }
    const res = await api(task.id, payload)
    closeTaskActionDialog()
    await refreshAfterAction(res.data)
    ElMessage.success(action === 'approve' ? '审批已通过' : '审批已驳回')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, action === 'approve' ? '审批通过失败' : '审批驳回失败'))
  } finally {
    taskActionSubmitting.value = false
  }
}

async function submitManualApproverSelection() {
  const documentCode = detail.value?.documentCode || ''
  const nodeKey = detail.value?.manualApproverSelectionNodeKey || ''
  if (!documentCode || !nodeKey) {
    return
  }
  if (!manualApproverForm.value.userIds.length) {
    ElMessage.warning('请至少选择一位审批人')
    return
  }
  manualApproverSubmitting.value = true
  try {
    await expenseApi.submitManualApproverSelection(documentCode, {
      nodeKey,
      userIds: manualApproverForm.value.userIds
    })
    ElMessage.success('手动审批人已提交')
    await loadDetail()
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '提交手动审批人失败'))
  } finally {
    manualApproverSubmitting.value = false
  }
}

async function handleActionClick(action: ActionItem) {
  if (action.disabled) {
    ElMessage.warning(action.reason || '\u5f53\u524d\u52a8\u4f5c\u6682\u4e0d\u53ef\u7528')
    return
  }

  switch (action.key) {
    case 'resubmit':
      await openResubmitPage()
      return
    case 'recall':
      await handleRecall()
      return
    case 'print':
      handlePrint()
      return
    case 'download':
      ElMessage.info('\u529f\u80fd\u5efa\u8bbe\u4e2d')
      return
    case 'comment':
      openCommentDialog()
      return
    case 'remind':
      await handleRemind()
      return
    case 'approve':
      await handleTaskAction('approve')
      return
    case 'reject':
      await handleTaskAction('reject')
      return
    case 'prev':
      await navigateDetail(navigation.value.prevDocumentCode)
      return
    case 'next':
      await navigateDetail(navigation.value.nextDocumentCode)
      return
    case 'modify':
      await openModifyPage()
      return
    case 'add-sign':
    case 'transfer':
      await openUserActionDialog(action.key)
      return
  }
}

function handlePrint() {
  const documentCode = detail.value?.documentCode || String(route.params.documentCode || '')
  if (!documentCode) {
    ElMessage.warning('\u7f3a\u5c11\u5355\u636e\u7f16\u53f7\uff0c\u65e0\u6cd5\u6253\u5f00\u6253\u5370\u9875')
    return
  }
  const openedWindow = openExpensePrintWindow(buildExpenseDetailPrintHref(router, documentCode))
  if (!openedWindow) {
    ElMessage.error('\u672a\u80fd\u6253\u5f00\u6253\u5370\u7a97\u53e3\uff0c\u8bf7\u68c0\u67e5\u6d4f\u89c8\u5668\u5f39\u7a97\u62e6\u622a\u8bbe\u7f6e')
  }
}

async function triggerPrint(documentCode: string) {
  if (!documentCode || lastPrintedDocumentCode === documentCode || !detail.value || printLoadError.value) {
    return
  }
  lastPrintedDocumentCode = documentCode
  await nextTick()
  window.print()
}

async function handleRecall() {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('召回后会回到草稿编辑页，并沿用当前单号重新提交，确认继续吗？', '召回单据', {
      type: 'warning',
      confirmButtonText: '确认召回',
      cancelButtonText: '取消'
    })
    await expenseApi.recall(detail.value.documentCode)
    ElMessage.success('单据已召回，正在进入重提编辑页')
    await router.push({
      name: 'expense-document-resubmit',
      params: { documentCode: detail.value.documentCode },
      query: buildReturnToQuery()
    })
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '召回单据失败'))
  }
}

async function handleRemind() {
  if (!detail.value) {
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('可选填写催办备注', '催办审批', {
      inputType: 'textarea',
      inputPlaceholder: '例如：这笔单据今天需要完成处理',
      confirmButtonText: '发送催办',
      cancelButtonText: '取消'
    })
    const res = await expenseApi.remind(detail.value.documentCode, { remark: value || '' })
    await refreshAfterAction(res.data)
    ElMessage.success('已向当前审批人发送催办')
  } catch (error: unknown) {
    if (error === 'cancel' || String(error).includes('cancel')) {
      return
    }
    ElMessage.error(resolveErrorMessage(error, '催办失败'))
  }
}

function openCommentDialog() {
  commentForm.value = {
    comment: '',
    attachmentFileNames: []
  }
  commentDialogVisible.value = true
}

async function submitComment() {
  if (!detail.value) {
    return
  }
  if (!commentForm.value.comment.trim() && commentForm.value.attachmentFileNames.length === 0) {
    ElMessage.warning('请先输入评论或添加附件名')
    return
  }
  commentSubmitting.value = true
  try {
    const res = await expenseApi.comment(detail.value.documentCode, {
      comment: commentForm.value.comment.trim(),
      attachmentFileNames: commentForm.value.attachmentFileNames
    })
    commentDialogVisible.value = false
    await refreshAfterAction(res.data)
    ElMessage.success('评论已发布')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, '发表评论失败'))
  } finally {
    commentSubmitting.value = false
  }
}

function pickCommentFiles() {
  commentFileInput.value?.click()
}

function handleCommentFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (files.length === 0) {
    return
  }
  const merged = new Set([
    ...commentForm.value.attachmentFileNames,
    ...files.map((file) => file.name).filter(Boolean)
  ])
  commentForm.value.attachmentFileNames = Array.from(merged)
  target.value = ''
}

function removeCommentAttachment(name: string) {
  commentForm.value.attachmentFileNames = commentForm.value.attachmentFileNames.filter((item) => item !== name)
}

async function openModifyPage() {
  const task = approvableTasks.value[0]
  if (!task) {
    ElMessage.warning('当前没有可修改的待办任务')
    return
  }
  await router.push({
    name: 'expense-approval-task-modify',
    params: { taskId: task.id }
  })
}

async function openUserActionDialog(actionKey: 'add-sign' | 'transfer') {
  const task = approvableTasks.value[0]
  if (!task) {
    ElMessage.warning('当前没有可处理的待办任务')
    return
  }
  userActionMode.value = actionKey
  userActionForm.value = {
    targetUserId: undefined,
    remark: ''
  }
  userActionDialogVisible.value = true
  await loadActionUsers('')
}

function closeUserActionDialog() {
  userActionDialogVisible.value = false
  userActionMode.value = ''
  userActionForm.value = {
    targetUserId: undefined,
    remark: ''
  }
}

async function loadActionUsers(keyword: string) {
  userOptionsLoading.value = true
  try {
    const res = await expenseApprovalApi.listActionUsers(keyword)
    userOptions.value = res.data
  } finally {
    userOptionsLoading.value = false
  }
}

async function submitUserAction() {
  const task = approvableTasks.value[0]
  if (!task) {
    ElMessage.warning('当前没有可处理的待办任务')
    return
  }
  if (!userActionForm.value.targetUserId) {
    ElMessage.warning('请先选择目标处理人')
    return
  }
  userActionSubmitting.value = true
  try {
    const mode = userActionMode.value
    const payload = {
      targetUserId: userActionForm.value.targetUserId,
      remark: userActionForm.value.remark.trim()
    }
    const res = mode === 'transfer'
      ? await expenseApprovalApi.transfer(task.id, payload)
      : await expenseApprovalApi.addSign(task.id, payload)
    closeUserActionDialog()
    await refreshAfterAction(res.data)
    ElMessage.success(mode === 'transfer' ? '审批任务已转交' : '已发起加签')
  } catch (error: unknown) {
    ElMessage.error(resolveErrorMessage(error, userActionMode.value === 'transfer' ? '转交审批失败' : '加签失败'))
  } finally {
    userActionSubmitting.value = false
  }
}

async function navigateDetail(documentCode?: string) {
  if (!documentCode) {
    ElMessage.warning('已经没有更多单据了')
    return
  }
  await router.push(`/expense/documents/${encodeURIComponent(documentCode)}`)
}

async function refreshAfterAction(nextDetail?: ExpenseDocumentDetail) {
  if (nextDetail) {
    detailLoadError.value = ''
    detail.value = nextDetail
    syncBindingPanelExpansion(nextDetail)
    navigation.value = {}
    void syncReadonlyPayeeLookups(nextDetail.formSchemaSnapshot)
    await loadNavigation(nextDetail.documentCode, detailRequestVersion)
    return
  }
  await loadDetail()
}

function asString(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

function formatDetailMoney(value: unknown) {
  try {
    return formatMoney(value as string | number | null | undefined)
  } catch {
    return '0.00'
  }
}

async function openResubmitPage() {
  const documentCode = detail.value?.documentCode || String(route.params.documentCode || '')
  if (!documentCode) {
    ElMessage.warning('缺少单据编码，无法打开编辑页')
    return
  }
  await router.push({
    path: `/expense/documents/${encodeURIComponent(documentCode)}/resubmit`,
    query: buildReturnToQuery(detail.value?.status === 'DRAFT' ? { entry: 'draft' } : {})
  })
}

function formatBindingMoney(value: unknown) {
  return `¥ ${formatDetailMoney(value)}`
}

function writeOffSourceKindLabel(kind?: string) {
  switch (kind) {
    case 'LOAN':
      return '借款单'
    case 'PREPAY_REPORT':
      return '预付报销单'
    default:
      return '-'
  }
}

function formatAttachmentSize(value?: number) {
  if (!value || Number.isNaN(Number(value))) {
    return '大小未知'
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function resolveExpenseDetailTypeLabel(detailType?: string, fallback?: string) {
  if (detailType === 'ENTERPRISE_TRANSACTION') return '企业往来'
  if (detailType === 'NORMAL_REIMBURSEMENT') return '普通报销'
  return fallback || '费用明细'
}
</script>

<style scoped>
.expense-print-page {
  min-height: 100vh;
  background: #f4f7fb;
  padding: 24px;
}

.expense-print-page__state {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-page {
  padding-bottom: 132px;
}

.detail-hero {
  padding: 18px 22px;
  border-radius: 28px;
}

.detail-hero::before {
  top: -120px;
  right: -56px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 72%);
}

.detail-hero::after {
  bottom: -170px;
  left: -80px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(186, 230, 253, 0.2) 0%, rgba(186, 230, 253, 0) 74%);
}

.detail-hero__content {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.detail-hero__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-hero__backlink {
  font-size: 13px;
}

.detail-hero__title {
  margin: 0;
  font-size: clamp(22px, 2.4vw, 28px);
  line-height: 1.18;
  word-break: break-word;
}

.detail-hero__amount {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  min-width: 176px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  padding: 12px 16px;
  backdrop-filter: blur(12px);
}

.detail-hero__amount-label {
  font-size: 12px;
  color: rgba(224, 242, 254, 0.88);
}

.detail-hero__amount-value {
  font-size: clamp(22px, 2.2vw, 28px);
  line-height: 1.1;
  font-weight: 700;
  color: #ffffff;
  white-space: nowrap;
}

.detail-main-scroll,
.approval-scroll {
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding-right: 6px;
}

.detail-main-scroll::-webkit-scrollbar,
.approval-scroll::-webkit-scrollbar {
  width: 8px;
}

.detail-main-scroll::-webkit-scrollbar-thumb,
.approval-scroll::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.7);
  border-radius: 999px;
}

.detail-main-scroll::-webkit-scrollbar-track,
.approval-scroll::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.7);
  border-radius: 999px;
}

.binding-card-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.binding-card-toggle {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  line-height: 1.5;
  color: rgb(37 99 235);
  cursor: pointer;
}

.binding-card-toggle:hover {
  color: rgb(29 78 216);
}

.binding-card-inline-empty {
  margin: 0;
  padding: 12px 14px;
  border: 1px dashed rgba(203, 213, 225, 0.9);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.82);
  font-size: 13px;
  line-height: 1.6;
  color: rgb(100 116 139);
}

.approval-node-status-list {
  display: grid;
  gap: 12px;
}

.approval-node-status-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
}

.approval-node-status-card--pending {
  border-color: rgba(251, 191, 36, 0.6);
  background: rgba(255, 251, 235, 0.96);
}

.approval-node-status-card--future {
  border-style: dashed;
}

.approval-node-status-card__content {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.approval-node-status-card__time {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 1.5;
  color: rgb(100 116 139);
}

.detail-floating-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 24px;
  z-index: 30;
  display: flex;
  justify-content: center;
  padding: 0 20px;
}

.detail-floating-inner {
  width: min(1120px, 100%);
  border: 1px solid rgba(219, 234, 254, 0.92);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.96) 100%);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(16px);
  padding: 20px 24px;
}

.detail-floating-hint {
  margin-bottom: 14px;
  font-size: 16px;
  line-height: 1.45;
  color: rgb(180 83 9);
}

.detail-floating-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
}

.detail-floating-actions__group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.detail-floating-actions__group--secondary {
  flex: 0 1 auto;
  min-width: auto;
}

.detail-floating-actions__group--primary {
  flex: 0 1 auto;
}

:deep(.detail-floating-button) {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 14px;
  font-size: 15px;
  line-height: 1.1;
}

:deep(.detail-floating-button span) {
  font-size: inherit;
}

@media (max-width: 1279px) {
  .detail-page {
    padding-bottom: 168px;
  }

  .detail-hero {
    padding: 16px 18px;
  }

  .detail-hero__content {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }

  .detail-hero__title {
    font-size: 22px;
  }

  .detail-hero__amount {
    min-width: 0;
    align-items: flex-start;
  }

  .detail-main-scroll,
  .approval-scroll {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }

  .detail-floating-bar {
    bottom: 16px;
    padding: 0 12px;
  }

  .detail-floating-inner {
    padding: 18px 16px;
  }

  .detail-floating-hint {
    margin-bottom: 12px;
    font-size: 14px;
  }

  .detail-floating-actions {
    gap: 10px;
  }

  .detail-floating-actions__group {
    gap: 10px;
  }

  .detail-floating-actions__group--secondary,
  .detail-floating-actions__group--primary {
    flex-basis: 100%;
    min-width: 0;
  }

  .detail-floating-actions__group--secondary,
  .detail-floating-actions__group--primary {
    justify-content: flex-end;
  }

  :deep(.detail-floating-button) {
    min-height: 34px;
    padding: 0 14px;
    font-size: 14px;
  }
}

@media print {
  .expense-print-page {
    background: #ffffff;
    padding: 0;
  }

  .expense-print-page__state {
    max-width: none;
  }
}
</style>
