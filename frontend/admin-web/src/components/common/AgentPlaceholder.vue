<template>
  <div class="placeholder-container">
    <div class="placeholder-content">
      <div class="placeholder-icon">
        <component :is="iconComponent" class="icon" />
      </div>
      <h2>{{ title }}</h2>
      <p class="description">{{ description }}</p>
      <div class="placeholder-meta">
        <el-tag type="info">Phase {{ phase }}</el-tag>
        <el-tag type="warning">开发中</el-tag>
      </div>
      <div v-if="features.length" class="feature-preview">
        <h3>规划功能</h3>
        <ul>
          <li v-for="(feature, index) in features" :key="index">{{ feature }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Notebook, Search, Reading } from '@element-plus/icons-vue'

const props = defineProps<{
  title: string
  description: string
  phase: number
  iconType?: 'user' | 'notebook' | 'search' | 'reading'
  features?: string[]
}>()

const iconComponent = computed(() => {
  switch (props.iconType) {
    case 'user':
      return User
    case 'notebook':
      return Notebook
    case 'search':
      return Search
    case 'reading':
      return Reading
    default:
      return Notebook
  }
})

const features = computed(() => props.features || [])
</script>

<style scoped>
.placeholder-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 500px;
  padding: 40px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.placeholder-content {
  max-width: 600px;
  text-align: center;
  background: white;
  padding: 48px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(39, 48, 71, 0.08);
}

.placeholder-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  margin-bottom: 24px;
}

.placeholder-icon .icon {
  width: 40px;
  height: 40px;
  color: white;
}

.placeholder-content h2 {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 700;
  color: #273047;
}

.description {
  margin: 0 0 24px;
  font-size: 16px;
  color: rgba(39, 48, 71, 0.7);
  line-height: 1.6;
}

.placeholder-meta {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 32px;
}

.feature-preview {
  text-align: left;
  padding: 24px;
  background: #f8fafc;
  border-radius: 16px;
  margin-top: 24px;
}

.feature-preview h3 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #273047;
}

.feature-preview ul {
  margin: 0;
  padding-left: 24px;
}

.feature-preview li {
  margin-bottom: 8px;
  color: rgba(39, 48, 71, 0.8);
  line-height: 1.6;
}

.feature-preview li:last-child {
  margin-bottom: 0;
}
</style>
