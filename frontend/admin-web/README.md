# FinEx 财务报销系统 - 前端项目

> 基于 Vue 3 + TypeScript + Element Plus 的现代化财务报销系统前端

## 📸 界面预览

### 登录页
- 左右分栏设计，左侧品牌展示 + 右侧登录表单
- 支持密码登录和验证码登录
- 现代化渐变背景 + 玻璃拟态效果

### 首页仪表板
- 统计卡片展示关键数据
- 快捷操作入口
- 最近报销单列表
- 待审批和异常提醒

### 报销管理
- 报销单列表展示
- 搜索筛选功能
- 分页展示

### 发票管理
- 发票库展示
- 验真状态管理
- 发票上传功能

## 🛠 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型语言 |
| Element Plus | 2.5+ | UI组件库 |
| Tailwind CSS | 3.x | CSS框架 |
| Vite | 5.x | 构建工具 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理 |

## 🚀 快速开始

### 1. 安装依赖
```bash
cd frontend/admin-web
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```

### 3. 浏览器访问
打开 http://localhost:5173

### 4. 构建生产版本
```bash
npm run build
```

## 📁 项目结构

```
src/
├── assets/           # 静态资源
├── components/       # 公共组件
│   └── NewExpenseDialog.vue    # 新建报销弹窗
├── layouts/          # 布局组件
│   └── MainLayout.vue           # 主布局（侧边栏+顶部导航）
├── router/           # 路由配置
│   └── index.ts                 # 路由定义
├── styles/           # 样式文件
│   └── main.css                 # 全局样式
├── views/            # 页面视图
│   ├── LoginView.vue            # 登录页
│   ├── DashboardView.vue        # 首页仪表板
│   ├── expense/
│   │   └── ExpenseListView.vue  # 报销列表
│   └── invoice/
│       └── InvoiceListView.vue  # 发票列表
├── App.vue           # 根组件
└── main.ts           # 入口文件
```

## 📖 文档

- [项目说明文档](./项目说明.md) - 详细的项目结构和文件说明，适合前端新手

## 📝 功能清单

- [x] 登录页（密码/验证码登录）
- [x] 首页仪表板
- [x] 报销管理（列表、新建）
- [x] 发票管理（列表、上传）
- [ ] 审批流程
- [ ] 银企直连
- [ ] 凭证管理
- [ ] 系统设置

## 🔧 开发规范

### 命名规范
- 组件名：PascalCase（如 `NewExpenseDialog.vue`）
- 文件名：kebab-case（如 `expense-list.vue`）
- 变量名：camelCase（如 `currentPage`）

### 代码风格
- 使用 TypeScript 类型
- 组件使用 `<script setup>` 语法
- 使用组合式函数复用逻辑

## 📄 许可证

MIT
