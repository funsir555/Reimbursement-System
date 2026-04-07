# `finexgz.xyz` 阿里云轻量服务器部署手册

这份手册把仓库里的 Linux 部署脚本串成一条可以直接照着做的上线路径，目标是把当前项目以最低成本部署到阿里云中国内地轻量应用服务器，并使用正式域名 `admin.finexgz.xyz` 对外提供访问。

## 1. 目标架构

- 服务器：阿里云轻量应用服务器，Ubuntu 22.04 LTS
- 域名：`admin.finexgz.xyz`
- 对外端口：`22`、`80`、`443`
- 本机端口：
  - `127.0.0.1:8080` -> gateway
  - `127.0.0.1:8081` -> auth-service
  - `127.0.0.1:3306` -> MySQL 8
- 静态资源：Nginx 托管 `/opt/finex/www/admin`
- 后端产物：`/opt/finex/backend/*.jar`

## 2. 先在阿里云控制台完成的动作

### 2.1 购买服务器

- 产品：轻量应用服务器
- 地域：杭州或上海优先
- 配置：`2核4G / 50G系统盘 / 3M~5M带宽`
- 镜像：`Ubuntu 22.04 LTS`

记录这 4 项信息：

- 公网 IP
- 实例登录密码
- 地域
- 操作系统版本

### 2.2 安全策略

只开放：

- `22`：SSH
- `80`：HTTP
- `443`：HTTPS

不要开放：

- `3306`
- `8080`
- `8081`

### 2.3 备案与域名

- 域名 `finexgz.xyz` 保持实名认证完成状态
- 在阿里云备案控制台发起备案
- 备案等待期间可以先用公网 IP 验证部署，不要把正式域名大范围对外使用

## 3. 上传代码到服务器

下面示例假设把仓库放到 `/opt/finex/repo`。

```bash
mkdir -p /opt/finex
cd /opt/finex
git clone <你的仓库地址> repo
cd /opt/finex/repo
```

如果暂时没有远程仓库，也可以本地打包后用 `scp` 或 SFTP 上传整个项目目录。

## 4. 初始化服务器环境

以 root 执行：

```bash
cd /opt/finex/repo
bash deploy/linux/scripts/bootstrap_ubuntu.sh
```

这个脚本现在会安装：

- Java 17
- Maven
- Node.js 20
- Nginx
- MySQL 8
- Certbot
- `finex` 系统用户

## 5. 初始化数据库

### 5.1 创建数据库和账号，并自动执行首发 SQL

以 root 执行：

```bash
cd /opt/finex/repo
DB_PASSWORD='请改成你的数据库密码' bash deploy/linux/scripts/init_mysql.sh
```

默认会自动创建：

- 数据库：`finex_db`
- 用户：`finex_user@localhost`

默认会按顺序执行：

1. `backend/sql/init.sql`
2. `backend/sql/init_custom_archive.sql`
3. `backend/sql/init_expense_type_tree.sql`
4. `backend/sql/init_async_task.sql`
5. `backend/sql/init_finance_gl.sql`
6. `backend/sql/init_process_flow_design.sql`
7. `backend/sql/migrate_system_settings.sql`

如果后续菜单或权限显示不完整，再执行：

```bash
cd /opt/finex/repo
RUN_PERMISSION_REFRESH=1 DB_PASSWORD='与你当前配置一致的数据库密码' bash deploy/linux/scripts/init_mysql.sh
```

### 5.2 校验数据库对象

```bash
mysql -ufinex_user -p finex_db -e "SHOW TABLES LIKE 'sys_sync_%';"
```

至少应看到：

- `sys_sync_connector`
- `sys_sync_job`
- `sys_sync_job_detail`

## 6. 准备生产配置文件

### 6.1 后端配置

```bash
cd /opt/finex/repo
cp backend/.env.production.example backend/.env.production
```

至少修改这些值：

```env
FINEX_DB_URL=jdbc:mysql://127.0.0.1:3306/finex_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
FINEX_DB_USERNAME=finex_user
FINEX_DB_PASSWORD=你的数据库密码
FINEX_JWT_SECRET=至少32位随机字符串
FINEX_AUTH_SERVICE_PORT=8081
FINEX_GATEWAY_PORT=8080
FINEX_AUTH_SERVICE_URL=http://127.0.0.1:8081
```

### 6.2 前端配置

```bash
cd /opt/finex/repo
cp frontend/admin-web/.env.production.example frontend/admin-web/.env.production
```

保持：

```env
VITE_API_BASE_URL=/api
```

## 7. 构建和发布应用

```bash
cd /opt/finex/repo
bash deploy/linux/scripts/deploy_finex.sh
```

脚本完成后，重点检查：

- `/opt/finex/backend/auth-service.jar`
- `/opt/finex/backend/gateway.jar`
- `/opt/finex/backend/.env.production`
- `/opt/finex/www/admin`

## 8. 安装 systemd 和 Nginx

```bash
cd /opt/finex/repo
FINEX_DOMAIN=admin.finexgz.xyz bash deploy/linux/scripts/install_runtime.sh
```

这个脚本会：

- 安装 `finex-auth.service`
- 安装 `finex-gateway.service`
- 渲染 Nginx 站点配置
- 启动并设置 `nginx`、`finex-auth`、`finex-gateway` 开机自启

## 9. 做第一次服务验收

### 9.1 看服务状态

```bash
systemctl status finex-auth --no-pager
systemctl status finex-gateway --no-pager
systemctl status nginx --no-pager
```

### 9.2 看本机接口

```bash
curl http://127.0.0.1:8080/api/auth/me
curl http://127.0.0.1/healthz
```

未登录时 `/api/auth/me` 返回鉴权失败也正常，重点是网关应当有响应，不是连接失败。

### 9.3 看日志

```bash
tail -n 50 /opt/finex/logs/auth-service.log
tail -n 50 /opt/finex/logs/gateway.log
```

## 10. 域名解析

在阿里云 DNS 里增加一条 A 记录：

- 主机记录：`admin`
- 记录值：`你的服务器公网 IP`

然后等待解析生效，验证：

```bash
curl -I http://admin.finexgz.xyz
```

## 11. 启用 HTTPS

备案通过后执行：

```bash
cd /opt/finex/repo
CERTBOT_EMAIL='你的邮箱' FINEX_DOMAIN=admin.finexgz.xyz bash deploy/linux/scripts/enable_https.sh
```

验证：

```bash
curl -I https://admin.finexgz.xyz
```

浏览器打开 `https://admin.finexgz.xyz` 不应出现证书警告。

## 12. 配置企业微信同步

登录系统后台后，进入“系统设置 -> 组织架构 -> 同步连接配置”，只配置 `WECOM`：

- 企业 ID：`CorpID`
- 通讯录 Secret：企业微信通讯录 Secret
- 自动同步：按需开启
- 同步间隔：按需填写分钟数

如果企业微信启用了可信 IP：

- 把阿里云服务器公网 IP 加进去

如果企业微信配置可信域名：

- 填 `admin.finexgz.xyz`

## 13. 业务验收清单

### 13.1 页面访问

- 能打开 `https://admin.finexgz.xyz`
- 登录成功
- 系统设置页能正常加载

### 13.2 组织同步

- 能保存 `CorpID + 通讯录 Secret`
- 点击“立即同步”后出现同步日志
- `sys_department` 写入企微部门映射
- `sys_user` 写入企微成员映射

### 13.3 安全检查

- 公网只能访问 `22/80/443`
- `3306/8080/8081` 对公网不可见

## 14. 后续更新方式

以后代码更新时，默认流程：

```bash
cd /opt/finex/repo
git pull
bash deploy/linux/scripts/deploy_finex.sh
systemctl restart finex-auth finex-gateway
systemctl restart nginx
```

## 15. 建议保留的运维动作

- 每天做一次数据库备份
- 每次大版本上线前做一份服务器快照
- 保存好这几个核心信息：
  - 服务器公网 IP
  - MySQL root 登录方式
  - `FINEX_DB_PASSWORD`
  - `FINEX_JWT_SECRET`
  - 企业微信 `CorpID`
  - 企业微信通讯录 `Secret`