<template>
  <div v-if="canSubmit" class="share-submit-entry">
    <el-button :disabled="!agentId || !publishedVersion" @click="submit">
      提交企业共享{{ publishedVersion ? ` · v${publishedVersion.versionNo}` : '' }}
    </el-button>
    <span v-if="hasUnpublishedVersion" class="share-submit-hint">
      当前画布为 v{{ latestVersionNo }}，尚未发布
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { digitalEmployeeApi } from '@/api/modules/digital-employee'
import type { ArchiveAgentVersionRecord } from '@/api'
import { hasAnyPermission, readStoredUser } from '@/utils/permissions'

const props = defineProps<{
  agentId?: number
  versions: ArchiveAgentVersionRecord[]
  latestVersionNo?: number
  publishedVersionNo?: number
}>()
const canSubmit = hasAnyPermission(['agent:workbench:share_submit'], readStoredUser()?.permissionCodes || [])
const publishedVersion = computed(() => props.versions.find(version =>
  version.published && version.versionNo === props.publishedVersionNo))
const hasUnpublishedVersion = computed(() => props.latestVersionNo !== undefined
  && props.publishedVersionNo !== undefined && props.latestVersionNo > props.publishedVersionNo)

async function submit() {
  if (!props.agentId || !publishedVersion.value) return
  try {
    await digitalEmployeeApi.submitShare(props.agentId, publishedVersion.value.id)
    ElMessage.success('固定版本已提交企业共享审核')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '共享提交失败') }
}
</script>

<style scoped>
.share-submit-entry {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.share-submit-hint {
  color: var(--el-color-warning);
  font-size: 12px;
  line-height: 1.2;
}
</style>
