# 财务记账与报销一体化系统

## 项目简介
基于阿里云的财务记账与报销一体化系统，支持发票查重验真、银企直连、凭证生成、审批流程等功能。

## 技术栈
- **前端**: Vue3 + TypeScript + Element Plus
- **后端**: Java 17 + Spring Boot 3.x + Spring Cloud Alibaba
- **数据库**: MySQL 8.0 + Redis + MongoDB
- **基础设施**: 阿里云 ECS/RDS/OSS

## 项目结构
```
financial-expense-system/
├── docs/               # 文档
├── frontend/           # 前端项目
├── backend/            # 后端服务
├── infrastructure/     # 部署配置
└── third-party/        # 第三方SDK
```

## 开发进度
- [ ] 基础架构搭建
- [ ] 用户与组织架构
- [ ] 发票管理模块
- [ ] 报销流程模块
- [ ] 银企直连
- [ ] 凭证生成
- [ ] 企微/钉钉集成

## 启动项目

### 后端
```bash
cd backend
docker-compose up -d  # 启动依赖服务
./mvnw clean install
./mvnw spring-boot:run
```

### 前端
```bash
cd frontend/admin-web
npm install
npm run dev
```

## 文档
- [架构设计](./docs/architecture/README.md)
- [API文档](./docs/api/README.md)
- [部署文档](./docs/deploy/README.md)

## License
MIT
