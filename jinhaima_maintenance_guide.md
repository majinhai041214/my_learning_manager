# jinhaima.xyz 网站后期维护手册

## 1. 文档目的

这份文档用于后续维护 `jinhaima.xyz` 网站，覆盖以下内容：

- 当前服务器架构说明
- 前端单独更新流程
- 后端单独更新流程
- 整站更新流程
- Nginx、systemd、日志查看
- 常见故障排查
- 备份与维护建议

适用环境：

- 域名：`jinhaima.xyz`
- 服务器系统：Ubuntu 22.04
- 前端：Nuxt 4
- 后端：Spring Boot 3 + Java 21
- 反向代理：Nginx
- 进程管理：systemd

---

## 2. 当前部署架构

### 2.1 服务关系

```text
浏览器
  ↓
Nginx :80
  ├── /        → 127.0.0.1:3000   （Nuxt 前端）
  └── /api/    → 127.0.0.1:8080   （Spring Boot 后端）
```

### 2.2 当前服务名

- 前端服务：`website-frontend`
- 后端服务：`website-backend`
- Nginx 服务：`nginx`

### 2.3 当前项目目录

```bash
/opt/www/website/
├── frontend
├── backend
├── deploy
├── docs
├── uploads
├── logs
├── README.md
├── PROJECT_PROGRESS.md
└── PROJECT_PROGRESS_CN.md
```

### 2.4 构建产物位置

#### 前端
```bash
/opt/www/website/frontend/.output
```

#### 后端
```bash
/opt/www/website/backend/target/app.jar
```

---

## 3. 服务器常用命令速查

### 3.1 查看服务状态

```bash
sudo systemctl status website-frontend --no-pager
sudo systemctl status website-backend --no-pager
sudo systemctl status nginx --no-pager
```

### 3.2 重启服务

```bash
sudo systemctl restart website-frontend
sudo systemctl restart website-backend
sudo systemctl restart nginx
```

### 3.3 启动服务

```bash
sudo systemctl start website-frontend
sudo systemctl start website-backend
sudo systemctl start nginx
```

### 3.4 停止服务

```bash
sudo systemctl stop website-frontend
sudo systemctl stop website-backend
sudo systemctl stop nginx
```

### 3.5 查看端口监听

```bash
ss -lntp | grep -E '80|3000|8080'
```

### 3.6 查看日志

#### 前端日志
```bash
tail -f /opt/www/website/logs/frontend.log
```

#### 后端日志
```bash
tail -f /opt/www/website/logs/backend.log
```

#### Nginx 错误日志
```bash
sudo tail -f /var/log/nginx/error.log
```

---

## 4. 本地开发后推荐习惯

每次改完代码，建议先在本地：

### 4.1 提交 Git
```bash
git add .
git commit -m "更新说明"
git push origin main
```

### 4.2 再上传到服务器
这样方便：
- 回滚版本
- 对比线上线下差异
- 后续迁移到 Git 自动部署

---

## 5. 单独更新前端流程

适用于：
- 改页面
- 改组件
- 改样式
- 改前端请求逻辑
- 改 Nuxt 配置

---

### 5.1 本地准备

在本地项目目录中，只压缩 `frontend` 目录。

#### Windows PowerShell
```powershell
Compress-Archive -Path frontend -DestinationPath frontend-update.zip -Force
```

> 建议压缩前，先删除本地这些目录再打包：
>
> - `frontend/node_modules`
> - `frontend/.nuxt`
> - `frontend/.output`

这些都是构建缓存或依赖，不应该上传。

---

### 5.2 上传到服务器

```powershell
scp frontend-update.zip root@jinhaima.xyz:/opt/www/
```

---

### 5.3 服务器替换前端代码

登录服务器：

```bash
ssh root@jinhaima.xyz
```

执行：

```bash
cd /opt/www
unzip -o frontend-update.zip

rm -rf /opt/www/website/frontend
mv /opt/www/frontend /opt/www/website/

chown -R admin:admin /opt/www/website/frontend
find /opt/www/website/frontend -type d -exec chmod 755 {} \;
find /opt/www/website/frontend -type f -exec chmod 644 {} \;
```

---

### 5.4 重新构建前端

切换到 `admin` 用户：

```bash
su - admin
```

执行：

```bash
cd /opt/www/website/frontend

cat > .env <<'EOF'
NUXT_PUBLIC_API_BASE=/api
EOF

rm -rf node_modules .nuxt .output
npm install
npm run build
sudo systemctl restart website-frontend
```

---

### 5.5 验证前端是否更新成功

```bash
sudo systemctl status website-frontend --no-pager
curl -I http://127.0.0.1:3000
curl -I http://jinhaima.xyz
```

浏览器建议：
- `Ctrl + F5` 强制刷新
- 或用无痕模式打开

---

## 6. 单独更新后端流程

适用于：
- 改 Controller / Service / DTO
- 改接口逻辑
- 改文件上传逻辑
- 改后端配置

---

### 6.1 本地准备

只压缩 `backend` 目录。

#### Windows PowerShell
```powershell
Compress-Archive -Path backend -DestinationPath backend-update.zip -Force
```

> 建议压缩前先删除：
>
> - `backend/target`

它是构建产物，不要上传。

---

### 6.2 上传到服务器

```powershell
scp backend-update.zip root@jinhaima.xyz:/opt/www/
```

---

### 6.3 服务器替换后端代码

登录服务器：

```bash
ssh root@jinhaima.xyz
```

执行：

```bash
cd /opt/www
unzip -o backend-update.zip

rm -rf /opt/www/website/backend
mv /opt/www/backend /opt/www/website/

chown -R admin:admin /opt/www/website/backend
find /opt/www/website/backend -type d -exec chmod 755 {} \;
find /opt/www/website/backend -type f -exec chmod 644 {} \;
```

---

### 6.4 重新打包后端

切换用户：

```bash
su - admin
```

执行：

```bash
cd /opt/www/website/backend
mvn clean package -DskipTests
cp target/website-backend-0.0.1-SNAPSHOT.jar target/app.jar
sudo systemctl restart website-backend
```

---

### 6.5 验证后端是否更新成功

```bash
sudo systemctl status website-backend --no-pager
curl http://127.0.0.1:8080/api/notes
curl http://127.0.0.1/api/notes
```

---

## 7. 前后端一起更新流程

适用于：
- 大版本更新
- 前后端接口联调变更
- 页面与 API 同时改动

---

### 7.1 本地压缩

在本地项目目录执行：

```powershell
Compress-Archive -Path frontend,backend,deploy,docs,README.md,PROJECT_PROGRESS.md,PROJECT_PROGRESS_CN.md,.gitignore -DestinationPath website-update.zip -Force
```

---

### 7.2 上传

```powershell
scp website-update.zip root@jinhaima.xyz:/opt/www/
```

---

### 7.3 服务器解压覆盖

```bash
ssh root@jinhaima.xyz
cd /opt/www
unzip -o website-update.zip

rm -rf /opt/www/website/frontend
rm -rf /opt/www/website/backend
rm -rf /opt/www/website/deploy
rm -rf /opt/www/website/docs

mv /opt/www/frontend /opt/www/website/
mv /opt/www/backend /opt/www/website/
mv /opt/www/deploy /opt/www/website/
mv /opt/www/docs /opt/www/website/

mv /opt/www/README.md /opt/www/website/ 2>/dev/null || true
mv /opt/www/PROJECT_PROGRESS.md /opt/www/website/ 2>/dev/null || true
mv /opt/www/PROJECT_PROGRESS_CN.md /opt/www/website/ 2>/dev/null || true
mv /opt/www/.gitignore /opt/www/website/ 2>/dev/null || true

chown -R admin:admin /opt/www/website
```

---

### 7.4 重新构建并重启

```bash
su - admin

# 后端
cd /opt/www/website/backend
mvn clean package -DskipTests
cp target/website-backend-0.0.1-SNAPSHOT.jar target/app.jar
sudo systemctl restart website-backend

# 前端
cd /opt/www/website/frontend
cat > .env <<'EOF'
NUXT_PUBLIC_API_BASE=/api
EOF
rm -rf node_modules .nuxt .output
npm install
npm run build
sudo systemctl restart website-frontend
```

---

### 7.5 验证

```bash
sudo systemctl status website-backend --no-pager
sudo systemctl status website-frontend --no-pager
curl -I http://127.0.0.1
curl http://127.0.0.1/api/notes
curl -I http://jinhaima.xyz
```

---

## 8. Nginx 配置维护

### 8.1 配置文件位置

```bash
/etc/nginx/sites-available/website
```

启用位置：

```bash
/etc/nginx/sites-enabled/website
```

### 8.2 当前逻辑

- `/` → `127.0.0.1:3000`
- `/api/` → `127.0.0.1:8080/api/`

### 8.3 修改 Nginx 后标准流程

```bash
sudo nginx -t
sudo systemctl reload nginx
```

### 8.4 验证 Nginx 是否正常

```bash
curl -I http://127.0.0.1
curl -I http://jinhaima.xyz
```

---

## 9. systemd 服务维护

### 9.1 服务文件位置

#### 前端服务
```bash
/etc/systemd/system/website-frontend.service
```

#### 后端服务
```bash
/etc/systemd/system/website-backend.service
```

### 9.2 修改 service 文件后必须执行

```bash
sudo systemctl daemon-reload
sudo systemctl restart website-frontend
sudo systemctl restart website-backend
```

### 9.3 设置开机自启

```bash
sudo systemctl enable website-frontend
sudo systemctl enable website-backend
sudo systemctl enable nginx
```

---

## 10. 常见问题排查

### 10.1 页面提示“后端没有启动”，但实际上后端是好的

先验证：

```bash
curl http://127.0.0.1:8080/api/notes
curl http://127.0.0.1/api/notes
```

如果两条都通，说明：
- 后端正常
- Nginx 正常
- 问题多半在前端请求路径或浏览器缓存

---

### 10.2 前端请求变成 `/api/api/notes`

根因通常是：
- `.env` 写的是 `NUXT_PUBLIC_API_BASE=/api`
- 代码里又手动请求 `/api/notes`

结果就变成：

```text
/api + /api/notes = /api/api/notes
```

#### 正确写法
如果 `.env` 是：

```env
NUXT_PUBLIC_API_BASE=/api
```

那么代码应该请求：

```ts
/notes
```

而不是：

```ts
/api/notes
```

---

### 10.3 前端构建报 `Permission denied`

常见于：
- 上传的目录权限不对
- 对整个 `frontend` 错误执行过 `chmod 644`

修复方式：

```bash
sudo chown -R admin:admin /opt/www/website/frontend
find /opt/www/website/frontend -type d -exec chmod 755 {} \;
find /opt/www/website/frontend -type f -exec chmod 644 {} \;
```

如果是 `node_modules/.bin/nuxt` 或 `esbuild` 权限坏了，最稳的办法是：

```bash
cd /opt/www/website/frontend
rm -rf node_modules .nuxt .output
npm install
npm run build
```

---

### 10.4 Maven 打包报 `Unable to find main class`

必须确认 `backend/pom.xml` 中有：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>com.majinhai.website.WebsiteBackendApplication</mainClass>
    </configuration>
</plugin>
```

---

### 10.5 浏览器打不开网站

依次检查：

```bash
sudo systemctl status nginx --no-pager
sudo systemctl status website-frontend --no-pager
sudo systemctl status website-backend --no-pager
ss -lntp | grep -E '80|3000|8080'
```

再检查：

- 阿里云安全组是否放行 80 / 443
- Ubuntu `ufw` 是否放行 80 / 443

---

### 10.6 更新后页面没变化

先做：

- `Ctrl + F5`
- 无痕窗口打开
- 重启前端服务

```bash
sudo systemctl restart website-frontend
sudo systemctl restart nginx
```

如果仍无变化，重新构建前端：

```bash
cd /opt/www/website/frontend
rm -rf .output .nuxt node_modules
npm install
npm run build
sudo systemctl restart website-frontend
```

---

## 11. 备份建议

建议重点备份：

```bash
/opt/www/website/uploads
/opt/www/website/backend
/opt/www/website/frontend
/opt/www/website/logs
```

### 建议频率

- `uploads`：每天备份
- 整站源码：每次大更新前备份一次
- logs：按需保留

---

## 12. 建议后续优化方向

### 12.1 尽快做
1. 配 HTTPS
2. 网站底部展示 ICP 备案号
3. 网站正式上线后按要求完成公安备案

### 12.2 后续优化
1. 改成 Git 拉取部署
2. 写一键部署脚本
3. 配自动备份脚本
4. 上传文件改用 OSS
5. 加 CI/CD 自动部署

---

## 13. 最常用的一组命令

### 查看全部状态
```bash
sudo systemctl status website-frontend --no-pager
sudo systemctl status website-backend --no-pager
sudo systemctl status nginx --no-pager
```

### 重启全部服务
```bash
sudo systemctl restart website-frontend
sudo systemctl restart website-backend
sudo systemctl restart nginx
```

### 本机快速验证
```bash
curl -I http://127.0.0.1
curl -I http://jinhaima.xyz
curl http://127.0.0.1/api/notes
```

### 查看端口
```bash
ss -lntp | grep -E '80|3000|8080'
```
