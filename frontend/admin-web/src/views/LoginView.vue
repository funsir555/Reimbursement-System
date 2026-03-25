<template>
  <div class="login-container min-h-screen flex">
    <!-- 左侧品牌展示区 -->
    <div class="hidden lg:flex lg:w-1/2 gradient-bg-blue relative overflow-hidden">
      <!-- 装饰性背景元素 -->
      <div class="absolute inset-0">
        <div class="absolute top-20 left-20 w-72 h-72 bg-white/10 rounded-full blur-3xl"></div>
        <div class="absolute bottom-20 right-20 w-96 h-96 bg-white/10 rounded-full blur-3xl"></div>
        <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] border border-white/10 rounded-full"></div>
      </div>
      
      <!-- 内容区 -->
      <div class="relative z-10 flex flex-col justify-center px-16 w-full">
        <div class="animate-slide-in">
          <!-- Logo -->
          <div class="flex items-center gap-3 mb-8">
            <div class="w-12 h-12 bg-white rounded-xl flex items-center justify-center shadow-lg">
              <el-icon :size="28" class="text-blue-600"><Money /></el-icon>
            </div>
            <span class="text-2xl font-bold text-white">FinEx</span>
          </div>
          
          <!-- 主标题 -->
          <h1 class="text-5xl font-bold text-white leading-tight mb-6">
            智能财务<br>报销管理系统
          </h1>
          <p class="text-blue-100 text-lg mb-12 max-w-md">
            让报销更简单，让财务更高效。一站式解决企业费用管理难题。
          </p>
          
          <!-- 功能特点 -->
          <div class="space-y-4">
            <div class="flex items-center gap-3 text-white/90">
              <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
                <el-icon><Check /></el-icon>
              </div>
              <span>发票自动识别与查重验真</span>
            </div>
            <div class="flex items-center gap-3 text-white/90">
              <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
                <el-icon><Check /></el-icon>
              </div>
              <span>智能审批流程，高效协作</span>
            </div>
            <div class="flex items-center gap-3 text-white/90">
              <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
                <el-icon><Check /></el-icon>
              </div>
              <span>银企直连，一键付款</span>
            </div>
            <div class="flex items-center gap-3 text-white/90">
              <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
                <el-icon><Check /></el-icon>
              </div>
              <span>自动生成财务凭证</span>
            </div>
          </div>
        </div>
        
        <!-- 3D插图区域 -->
        <div class="mt-12 animate-fade-in" style="animation-delay: 0.3s">
          <div class="relative">
            <div class="w-full h-48 bg-gradient-to-r from-white/20 to-white/5 rounded-2xl backdrop-blur-sm border border-white/20 flex items-center justify-center">
              <div class="text-center text-white/80">
                <el-icon :size="48" class="mb-2"><TrendCharts /></el-icon>
                <p class="text-sm">财务数据可视化</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右侧登录表单区 -->
    <div class="w-full lg:w-1/2 bg-gray-50 flex items-center justify-center p-8">
      <div class="w-full max-w-md animate-fade-in">
        <!-- 移动端 Logo -->
        <div class="lg:hidden flex items-center gap-3 mb-8 justify-center">
          <div class="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center">
            <el-icon :size="24" class="text-white"><Money /></el-icon>
          </div>
          <span class="text-xl font-bold text-gray-800">FinEx</span>
        </div>
        
        <div class="bg-white rounded-2xl shadow-xl p-8">
          <!-- 登录方式切换 -->
          <div class="flex gap-6 mb-8 border-b border-gray-100">
            <button 
              @click="loginType = 'password'"
              class="pb-3 text-base font-medium transition-colors relative"
              :class="loginType === 'password' ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
            >
              密码登录
              <span 
                v-if="loginType === 'password'"
                class="absolute bottom-0 left-0 w-full h-0.5 bg-blue-600 rounded-full"
              ></span>
            </button>
            <button 
              @click="loginType = 'code'"
              class="pb-3 text-base font-medium transition-colors relative"
              :class="loginType === 'code' ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
            >
              验证码登录
              <span 
                v-if="loginType === 'code'"
                class="absolute bottom-0 left-0 w-full h-0.5 bg-blue-600 rounded-full"
              ></span>
            </button>
          </div>
          
          <!-- 密码登录表单 -->
          <el-form v-if="loginType === 'password'" :model="loginForm" class="space-y-5">
            <el-form-item class="mb-0">
              <el-input 
                v-model="loginForm.username" 
                placeholder="请输入手机号/邮箱"
                size="large"
                class="h-12"
              >
                <template #prefix>
                  <el-icon class="text-gray-400"><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item class="mb-0">
              <el-input 
                v-model="loginForm.password" 
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                size="large"
                class="h-12"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon class="text-gray-400"><Lock /></el-icon>
                </template>
                <template #suffix>
                  <el-icon 
                    class="cursor-pointer text-gray-400 hover:text-gray-600"
                    @click="showPassword = !showPassword"
                  >
                    <View v-if="showPassword" />
                    <Hide v-else />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <div class="flex items-center justify-between">
              <el-checkbox v-model="rememberMe" class="text-sm">记住我</el-checkbox>
              <a href="#" class="text-sm text-blue-600 hover:text-blue-700">忘记密码？</a>
            </div>
            
            <el-button 
              type="primary" 
              size="large" 
              class="w-full h-12 text-base font-medium"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>
          
          <!-- 验证码登录表单 -->
          <el-form v-else :model="codeForm" class="space-y-5">
            <el-form-item class="mb-0">
              <el-input 
                v-model="codeForm.phone" 
                placeholder="请输入手机号"
                size="large"
                class="h-12"
              >
                <template #prefix>
                  <el-icon class="text-gray-400"><Iphone /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item class="mb-0">
              <div class="flex gap-3">
                <el-input 
                  v-model="codeForm.code" 
                  placeholder="请输入验证码"
                  size="large"
                  class="h-12 flex-1"
                  @keyup.enter="handleLogin"
                >
                  <template #prefix>
                    <el-icon class="text-gray-400"><Message /></el-icon>
                  </template>
                </el-input>
                <el-button 
                  :disabled="countdown > 0"
                  size="large"
                  class="h-12 px-6"
                  @click="sendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            
            <el-button 
              type="primary" 
              size="large" 
              class="w-full h-12 text-base font-medium"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>
          
          <!-- 其他登录方式 -->
          <div class="mt-8">
            <div class="relative">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-gray-200"></div>
              </div>
              <div class="relative flex justify-center text-sm">
                <span class="px-2 bg-white text-gray-500">其他登录方式</span>
              </div>
            </div>
            
            <div class="mt-6 flex justify-center gap-6">
              <button class="w-10 h-10 rounded-full bg-green-50 flex items-center justify-center hover:bg-green-100 transition-colors">
                <svg class="w-5 h-5 text-green-600" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.045c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.49.49 0 0 1 .177-.554C23.126 18.199 24 16.58 24 14.786c0-3.26-3.064-5.891-7.062-5.928z"/>
                </svg>
              </button>
              <button class="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center hover:bg-blue-100 transition-colors">
                <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </button>
              <button class="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center hover:bg-gray-100 transition-colors">
                <svg class="w-5 h-5 text-gray-600" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.477 2 2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.879V14.89h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.989C18.343 21.129 22 16.99 22 12c0-5.523-4.477-10-10-10z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
        
        <!-- 底部链接 -->
        <div class="mt-8 text-center text-sm text-gray-500">
          <a href="#" class="hover:text-blue-600 transition-colors">账号激活</a>
          <span class="mx-2">|</span>
          <a href="#" class="hover:text-blue-600 transition-colors">申请试用</a>
          <span class="mx-2">|</span>
          <a href="#" class="hover:text-blue-600 transition-colors">简体中文</a>
        </div>
        
        <p class="mt-8 text-center text-xs text-gray-400">
          © 2025 FinEx 智能财务系统 | 客服电话：400-8888-888
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide, Iphone, Message, Check, Money, TrendCharts } from '@element-plus/icons-vue'
import { authApi } from '@/api'

const router = useRouter()

const loginType = ref<'password' | 'code'>('password')
const showPassword = ref(false)
const rememberMe = ref(false)
const loading = ref(false)
const countdown = ref(0)

const loginForm = reactive({
  username: '',
  password: ''
})

const codeForm = reactive({
  phone: '',
  code: ''
})

const handleLogin = async () => {
  // 表单验证
  if (loginType.value === 'password') {
    if (!loginForm.username.trim()) {
      ElMessage.warning('请输入用户名')
      return
    }
    if (!loginForm.password) {
      ElMessage.warning('请输入密码')
      return
    }
  } else {
    if (!codeForm.phone.trim()) {
      ElMessage.warning('请输入手机号')
      return
    }
    if (!codeForm.code.trim()) {
      ElMessage.warning('请输入验证码')
      return
    }
  }

  loading.value = true
  
  try {
    if (loginType.value === 'password') {
      // 调用真实登录接口
      const res = await authApi.loginByPassword(loginForm.username, loginForm.password)
      
      if (res.code === 200) {
        // 保存token
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('user', JSON.stringify({
          userId: res.data.userId,
          username: res.data.username,
          name: res.data.name
        }))
        
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } else {
        ElMessage.error(res.message || '登录失败')
      }
    } else {
      // 验证码登录（暂不支持）
      ElMessage.info('验证码登录功能开发中')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const sendCode = () => {
  if (!codeForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  
  countdown.value = 60
  ElMessage.success('验证码已发送')
  
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}
</script>

<style scoped>
.login-container {
  background: #f8fafc;
}
</style>
