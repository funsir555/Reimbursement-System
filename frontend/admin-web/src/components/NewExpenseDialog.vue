<template>
  <el-dialog
    v-model="visible"
    title="新建报销单"
    width="700px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="80px" class="mt-4">
      <!-- 基本信息 -->
      <el-form-item label="报销类型" required>
        <el-select v-model="form.type" placeholder="请选择报销类型" class="w-full">
          <el-option label="差旅费" value="travel" />
          <el-option label="交通费" value="transport" />
          <el-option label="招待费" value="entertain" />
          <el-option label="办公费" value="office" />
          <el-option label="通讯费" value="communication" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="报销事由" required>
        <el-input 
          v-model="form.reason" 
          type="textarea" 
          :rows="2"
          placeholder="请简要说明报销事由"
        />
      </el-form-item>
      
      <!-- 费用明细 -->
      <div class="bg-gray-50 rounded-lg p-4 mb-4">
        <div class="flex items-center justify-between mb-4">
          <span class="font-medium text-gray-700">费用明细</span>
          <el-button type="primary" :icon="Plus" size="small" @click="addItem">
            添加明细
          </el-button>
        </div>
        
        <div v-for="(item, index) in form.items" :key="index" class="mb-4">
          <div class="flex gap-3 items-start">
            <el-date-picker
              v-model="item.date"
              type="date"
              placeholder="发生日期"
              class="w-40"
            />
            
            <el-input v-model="item.desc" placeholder="费用说明" class="flex-1" />
            
            <el-input-number 
              v-model="item.amount" 
              :min="0" 
              :precision="2"
              placeholder="金额"
              class="w-32"
            >
              <template #prefix>¥</template>
            </el-input-number>
            
            <el-button 
              v-if="form.items.length > 1"
              type="danger" 
              :icon="Delete" 
              circle
              size="small"
              @click="removeItem(index)"
            />
          </div>
        </div>
        
        <div class="flex justify-end text-lg">
          <span class="text-gray-600">合计：</span>
          <span class="font-bold text-blue-600 ml-2">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
      </div>
      
      <!-- 发票上传 -->
      <el-form-item label="发票附件">
        <el-upload
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          accept=".jpg,.jpeg,.png,.pdf"
          multiple
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 JPG、PNG、PDF 格式，单个文件不超过 10MB
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="info" @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          提交审批
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, UploadFilled } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const submitting = ref(false)

const form = ref({
  type: '',
  reason: '',
  items: [
    { date: '', desc: '', amount: 0 }
  ]
})

const totalAmount = computed(() => {
  return form.value.items.reduce((sum, item) => sum + (item.amount || 0), 0)
})

const addItem = () => {
  form.value.items.push({ date: '', desc: '', amount: 0 })
}

const removeItem = (index: number) => {
  form.value.items.splice(index, 1)
}

const handleFileChange = () => {
  // 处理文件上传
}

const saveDraft = () => {
  ElMessage.success('已保存为草稿')
  visible.value = false
}

const submit = () => {
  submitting.value = true
  setTimeout(() => {
    submitting.value = false
    ElMessage.success('报销单已提交审批')
    visible.value = false
  }, 1500)
}
</script>
