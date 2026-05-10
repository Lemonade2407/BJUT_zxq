# ProjecTree 知享圈

> Plant your ideas, grow your future. —— 北京工业大学项目协作与组队平台

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Java 21 + Spring Boot 3.5.14 |
| ORM | MyBatis 3.0.3 + PageHelper |
| 数据库 | MySQL 8.0 |
| 缓存 | Caffeine |
| 认证 | JWT (jjwt 0.12.3) + BCrypt |
| 文件存储 | 阿里云 OSS |
| 前端框架 | Vue 3.5 + Vue Router 5 |
| 构建工具 | Vite 7 |
| 图表 | ECharts 6 + vue-echarts 8 |
| 文档解析 | mammoth (Word) + marked (Markdown) |
| 部署 | Docker Compose + Nginx |

## 快速开始

### 环境要求

- **JDK** 21+
- **Node.js** 20.19+ 或 22.12+
- **MySQL** 8.0+
- **Maven** 3.8+
- **Docker** & **Docker Compose**（生产部署）

### 本地开发

```bash
# 1. 克隆项目
git clone <repo-url>
cd BJUT_zxq

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入数据库密码、OSS 密钥、JWT 密钥

# 3. 初始化数据库
mysql -u root -p < init-db/01-init.sql

# 4. 启动后端（端口 8080）
cd server
mvn spring-boot:run

# 5. 启动前端（端口 5173）
cd Vue
npm install
npm run dev
```

访问 `http://localhost:5173`，默认管理员账号 `admin` / `123456`。

### Docker 部署

```bash
# 完整部署
docker-compose up -d

# 或使用部署脚本
chmod +x deploy.sh
./deploy.sh fast    # 增量构建部署
./deploy.sh first   # 首次全量构建
./deploy.sh restart # 仅重启服务
```

## 项目结构

```
BJUT_zxq/
├── common/                 # 公共模块（枚举、常量、通用响应）
├── pojo/                   # 实体、DTO、VO
├── server/                 # Spring Boot 主应用
│   └── src/main/java/com/bjutzxq/server/
│       ├── controller/     # REST 控制器
│       ├── service/        # 业务逻辑层
│       ├── mapper/         # MyBatis Mapper 接口
│       ├── config/         # 配置类（缓存、CORS、WebSocket）
│       ├── handler/        # 全局异常处理、WebSocket
│       ├── interceptor/    # JWT 拦截器
│       ├── annotation/     # 自定义注解（@RequireRole）
│       ├── aspect/         # AOP 权限校验
│       └── util/           # 工具类
├── Vue/                    # Vue 3 前端
│   └── src/
│       ├── api/            # API 请求模块
│       ├── components/     # 组件（按功能域分目录）
│       │   ├── admin/      # 管理后台
│       │   ├── auth/       # 登录注册
│       │   ├── home/       # 主页
│       │   ├── layout/     # 布局（Header/Footer/Toast）
│       │   ├── project/    # 项目相关
│       │   ├── team/       # 组队相关
│       │   ├── user/       # 用户相关
│       │   └── search/     # 搜索
│       ├── composables/    # 可复用逻辑
│       ├── router/         # 路由配置
│       ├── utils/          # 工具函数
│       └── assets/         # 静态资源
├── init-db/                # 数据库初始化脚本
├── docker-compose.yml      # Docker 编排
├── nginx.conf              # Nginx 配置
├── deploy.sh               # 部署脚本（Linux）
└── deploy.ps1              # 部署脚本（Windows）
```

## 功能列表

### 用户系统
- 注册/登录/退出、JWT Token 认证
- 三种角色：学生(USER)、教师(TEACHER)、管理员(ADMIN)
- 个人信息编辑、头像上传、密码修改
- 个人统计看板（ECharts 图表）

### 项目管理
- 创建项目（支持 5 种类型：课设、毕设、竞赛、个人、其他）
- 项目广场浏览、搜索、标签筛选
- 项目详情页（文档预览、代码文件树、评论、设置）
- 点赞、关注、下载（ZIP 打包）
- 文件上传（支持文件夹、覆盖模式）、OSS 存储

### 组队广场
- 发布组队需求（竞赛/项目/课设，可选具体课程）
- 成员数量管理、状态（招募中/已满员/已结束）
- 申请入队 → 组长审核（通过/拒绝）
- 入队通知（WebSocket 实时推送）
- 我的组队管理

### 管理后台
- 数据概览（用户/项目/组队统计、角色分布饼图、月度趋势折线图、热门标签柱状图）
- 用户管理（搜索、封禁、角色设置）
- 项目/标签/课程/评论 CRUD
- 组队管理

### 教师功能
- 教学班级管理（按班级/课程筛选学生项目）
- 批量下载学生项目（ZIP 打包）

## 数据库表

| 表 | 说明 |
|---|------|
| `user` | 用户（角色、状态、个人信息） |
| `project` | 项目（类型、可见性、统计计数） |
| `tag` | 标签（名称、分类、使用次数） |
| `project_tag` | 项目-标签关联 |
| `project_file` | 项目文件树（OSS 存储） |
| `comment` | 评论 |
| `star` | 点赞收藏 |
| `watch` | 关注 |
| `notification` | 通知 |
| `download_log` | 下载日志 |
| `course` | 课程字典 |
| `team` | 组队 |
| `team_application` | 组队申请 |

## API 概览

| 模块 | 前缀 | 主要端点 |
|------|------|---------|
| 认证 | `/api/auth` | login, register, me, refresh |
| 项目 | `/api/projects` | CRUD, search, trending, download |
| 文件 | `/api/projects/{id}/files` | upload, download, document |
| 评论 | `/api/projects/{id}/comments` | list, create, delete |
| 标签 | `/api/tags` | CRUD, hot, by-category |
| 课程 | `/api/course` | CRUD, active |
| 组队 | `/api/teams` | CRUD, apply, approve, reject |
| 点赞 | `/api/stars` | toggle |
| 关注 | `/api/watches` | toggle, my |
| 通知 | `/api/notifications` | list, read, delete |
| 用户统计 | `/api/auth/user/statistics` | 个人数据 |
| 管理 | `/api/admin` | users, projects, comments, teams, statistics |

## 环境变量

| 变量 | 说明 | 必填 |
|------|------|------|
| `DB_HOST` | 数据库地址 | 是 |
| `DB_PORT` | 数据库端口 | 是 |
| `DB_NAME` | 数据库名 | 是 |
| `DB_USERNAME` | 数据库用户 | 是 |
| `DB_PASSWORD` | 数据库密码 | 是 |
| `OSS_ACCESS_KEY_ID` | 阿里云 OSS Key | 是 |
| `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS Secret | 是 |
| `JWT_SECRET` | JWT 签名密钥（≥32字符） | 是 |
| `VITE_API_BASE_URL` | 前端 API 地址 | 否（默认 /api） |

## License

MIT
