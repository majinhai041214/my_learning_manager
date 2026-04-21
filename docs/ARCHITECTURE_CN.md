# 网站整体架构设计

## 1. 架构目标

新版网站的整体架构需要满足以下目标：

- 支持个人博客长期写作与展示
- 支持算法学习专区的专题化组织
- 支持每日打卡、笔记上传、代码上传、批注等后续能力
- 适配中国大陆访问场景
- 支持从本地开发平滑过渡到自建服务器部署
- 保持第一版实现简单，但后续可扩展

## 2. 总体架构概览

网站整体采用：

- 前端：`Nuxt`
- 后端：`Spring Boot`
- 反向代理与 HTTPS：`Caddy`
- 数据库存储：V1 使用 `SQLite`
- 文件存储：V1 使用服务器本地磁盘

整体逻辑分为四层：

1. 表现层：Nuxt 前端页面与交互
2. 服务层：Spring Boot 提供 API 与业务逻辑
3. 数据层：SQLite + 本地上传目录
4. 部署层：Caddy + Systemd + Linux 服务器

## 3. 系统架构图

```text
用户浏览器
   │
   ▼
Caddy
   ├─ /            -> Nuxt 前端站点
   └─ /api/*       -> Spring Boot 后端接口
                        ├─ 博客内容接口
                        ├─ 打卡接口
                        ├─ 笔记接口
                        ├─ 批注接口
                        └─ 上传接口
                              │
                              ├─ SQLite
                              └─ 本地文件目录 uploads/
```

## 4. 项目目录架构

当前工作区：

```text
F:\website
├─ frontend          # Nuxt 前端项目
├─ backend           # Spring Boot 后端项目
├─ deploy            # 部署配置、脚本、Caddy、systemd 模板
├─ docs              # 项目文档、架构文档、决策记录
├─ uploads           # 本地开发阶段的上传测试目录
├─ PROJECT_PROGRESS.md
├─ PROJECT_PROGRESS_CN.md
└─ README.md
```

建议后续保持的职责划分：

- `frontend`
  - 页面
  - 组件
  - 布局
  - 前端交互逻辑
  - SEO / 页面展示

- `backend`
  - API 控制器
  - 服务层
  - 数据模型
  - 文件上传逻辑
  - 数据读写

- `deploy`
  - Caddyfile
  - 部署脚本
  - systemd 服务文件
  - 上线说明

- `docs`
  - 技术栈决策
  - 架构设计
  - 备案与服务器记录
  - 接口文档

- `uploads`
  - 本地开发测试上传文件
  - 后续映射到服务器真实上传目录

## 5. 前端架构

### 5.1 前端职责

Nuxt 前端负责：

- 网站首页展示
- 博客页展示
- 算法学习区展示
- 关于页与导航
- 后续接入打卡、笔记、代码、批注等界面

### 5.2 前端分层

建议继续保持以下结构：

```text
frontend/
├─ app/
│  ├─ components/    # 可复用组件
│  ├─ layouts/       # 页面布局
│  ├─ pages/         # 路由页面
│  └─ app.vue        # 应用入口
├─ app/assets/
│  └─ css/           # 全局样式
├─ nuxt.config.ts
└─ package.json
```

### 5.3 前端模块规划

#### 首页模块

- 网站定位说明
- 核心模块入口
- 技术栈与路线说明

#### 博客模块

- 文章列表
- 标签分类
- 搜索
- 单篇文章详情页

#### 算法学习模块

- 打卡面板
- 学习路线图
- 笔记列表
- 代码列表
- 批注视图

#### 关于模块

- 站点定位
- 个人方向
- 长期建设路线

## 6. 后端架构

### 6.1 后端职责

Spring Boot 后端负责：

- 提供 API
- 处理打卡数据
- 管理笔记元数据
- 管理批注数据
- 提供上传能力
- 管理后续内容写入与读取

### 6.2 后端建议分层

```text
backend/src/main/java/com/majinhai/website
├─ controller        # API 接口层
├─ service           # 业务逻辑层
├─ repository        # 数据访问层
├─ model             # 实体与 DTO
├─ config            # 配置类
└─ WebsiteBackendApplication.java
```

### 6.3 后端 V1 API 规划

#### 健康检查

- `GET /api/health`

#### 博客相关

- `GET /api/posts`
- `GET /api/posts/{id}`

#### 打卡相关

- `GET /api/checkins`
- `POST /api/checkins`

#### 笔记相关

- `GET /api/notes`
- `GET /api/notes/{id}`
- `POST /api/notes`

#### 批注相关

- `GET /api/annotations`
- `POST /api/annotations`

#### 上传相关

- `POST /api/uploads`

## 7. 数据架构

### 7.1 V1 数据方案

第一版建议：

- 结构化数据：`SQLite`
- 上传文件：本地目录

原因：

- 成本低
- 部署简单
- 适合个人项目初期阶段

### 7.2 数据实体建议

#### Post

- id
- title
- slug
- summary
- content
- tags
- createdAt
- updatedAt

#### CheckIn

- id
- date
- title
- summary
- category
- duration
- createdAt

#### Note

- id
- title
- category
- filename
- path
- contentType
- createdAt

#### CodeFile

- id
- filename
- language
- category
- path
- createdAt

#### Annotation

- id
- noteId
- lineNumber
- content
- createdAt

## 8. 文件上传架构

V1 阶段建议采用本地文件目录：

```text
uploads/
├─ notes/
├─ code/
├─ images/
└─ temp/
```

服务器端建议对应为：

```text
/var/www/my-site/uploads
├─ notes/
├─ code/
├─ images/
└─ temp/
```

后续如果文件量变大，可演进到：

- 腾讯云 COS
- 阿里云 OSS

## 9. 部署架构

### 9.1 生产环境结构

```text
浏览器
   │
   ▼
Caddy
   ├─ 静态资源 / Nuxt 前端
   └─ /api -> Spring Boot
                ├─ SQLite
                └─ uploads 目录
```

### 9.2 服务器目录建议

```text
/var/www/my-site
├─ frontend
├─ backend
├─ uploads
├─ data
├─ logs
└─ backups
```

### 9.3 部署职责

#### Caddy

- 提供 HTTPS
- 处理域名
- 反向代理 `/api`
- 服务前端静态内容

#### Spring Boot

- 提供业务 API
- 操作数据库
- 管理文件上传

#### Systemd

- 保持后端常驻
- 支持重启与开机启动

## 10. 环境划分

### 本地开发环境

- 前端：`npm run dev`
- 后端：`mvn spring-boot:run`
- 数据：本地 SQLite
- 上传：`F:\website\uploads`

### 生产环境

- 前端：Nuxt 构建产物 / 或 Node 运行
- 后端：Spring Boot Jar
- 数据：服务器 SQLite
- 上传：服务器本地目录
- 域名：正式域名
- 反向代理：Caddy

## 11. 当前推荐的开发顺序

### 第一阶段：前端结构完善

- 完整首页
- 完整博客页
- 完整算法学习页
- 文章详情页模板

### 第二阶段：后端基础能力

- 健康检查
- 上传接口
- 打卡接口
- 笔记接口
- 批注接口

### 第三阶段：前后端打通

- 前端接 API
- 接入真实打卡数据
- 接入真实笔记与代码列表
- 接入批注

### 第四阶段：部署上线

- 服务器购买
- 域名注册
- ICP 备案
- Caddy 配置
- 服务上线

## 12. V1 边界

V1 不建议一开始就做：

- 多用户系统
- 复杂后台权限
- 微服务
- Redis / MQ
- 对象存储
- 全量 CMS

V1 先解决：

- 能展示
- 能记录
- 能上传
- 能部署
- 能长期迭代

## 13. 后续演进路线

V2 可以考虑：

- 后台管理页面
- 文章编辑工作流
- 更完整的笔记与批注体验
- 标签与搜索
- 图片上传与管理

V3 可以考虑：

- 接入对象存储
- 内容审核与权限
- 统计分析
- 更完善的 SEO
- 自动备份

## 14. 当前结论

当前整体项目架构已经明确为：

- `Nuxt` 负责前端展示与页面交互
- `Spring Boot` 负责业务 API 和数据处理
- `SQLite + 本地上传目录` 作为第一版存储方案
- `Caddy` 负责域名、HTTPS 和反向代理
- 网站按照“博客 + 算法学习 + 内容管理”的方向持续扩展

这套架构适合当前阶段：

- 成本可控
- 技术上你熟悉
- 部署路径清晰
- 后续演进空间足够
