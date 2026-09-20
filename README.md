# 宠安智能诊所（Smart PetClinic）

宠安智能诊所是一个前后端分离的宠物诊疗管理系统，覆盖宠物主人建档、宠物档案、兽医与排班、预约就诊、电子病历、疫苗记录、公告反馈、系统权限和 AI 诊疗助手等业务。

项目适合用于 Java 后端学习、全栈练习和作品集展示。默认配置面向本地开发环境，不应直接作为生产环境配置使用。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.4、Spring Security、Spring Validation |
| 数据访问 | MyBatis-Plus 3.5、MySQL 8、H2（测试） |
| 认证授权 | JWT、RBAC、方法级权限校验 |
| AI | Spring AI、OpenAI 兼容协议、DeepSeek（可选） |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router |
| UI 与图表 | Element Plus、ECharts |
| 部署 | Docker Compose、Nginx |
| 接口文档 | Springdoc OpenAPI / Swagger UI |

## 系统架构

```mermaid
flowchart LR
    Browser[Vue 3 浏览器客户端]
    Web[Vite 开发服务器或 Nginx]
    API[Spring Boot REST API]
    DB[(MySQL)]
    AI[DeepSeek API 可选]

    Browser --> Web
    Web -->|/api| API
    API --> DB
    API -.启用 AI 时.-> AI
```

前端保存 JWT，并通过 `Authorization: Bearer <token>` 调用后端。后端根据用户角色和权限码执行最终授权，前端权限树负责菜单及按钮展示。

## 已实现功能

- 账号密码登录、JWT 会话恢复和当前用户查询
- 宠物主人自助注册；自动创建 `OWNER` 用户、主人档案和角色关系，注册后直接登录
- 用户、角色、权限和动态菜单管理
- 主人档案、宠物档案、宠物类型管理
- 兽医、兽医专长和排班时段管理
- 预约创建、查询、取消和完成
- 电子病历与疫苗接种记录
- 公告发布、撤回及意见反馈处理
- 数据看板与操作日志
- AI 对话、历史会话及工具调用记录
- 参数校验、统一响应、全局异常处理和 OpenAPI 文档
- 后端单元测试、接口测试及 H2 集成测试

## 角色说明

| 角色 | 用途 |
| --- | --- |
| `ADMIN` | 系统管理及全部业务管理 |
| `STAFF` | 诊所日常业务、档案、排班和诊疗处理 |
| `OWNER` | 查看个人宠物与预约、提交反馈、使用面向主人的功能 |

自助注册固定创建 `OWNER` 账号，不接收前端传入的角色或账号类型，避免通过注册接口提升权限。管理员和员工账号由系统用户管理功能创建。

## 快速启动：Docker Compose

### 环境要求

- Docker Desktop 或 Docker Engine + Compose Plugin
- 端口 `5173`、`8080`、`3306` 未被占用

### 启动

```powershell
docker compose up --build
```

服务地址：

| 服务 | 地址 |
| --- | --- |
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| 健康检查 | http://localhost:8080/actuator/health |
| MySQL | `localhost:3306`，数据库 `petclinic` |

首次创建 `petclinic_mysql` 数据卷时，MySQL 会自动执行：

1. `backend/src/main/resources/db/schema.sql`
2. `backend/src/main/resources/db/data.sql`

修改初始化 SQL 后，已有数据卷不会自动重新执行脚本。如确实需要清空本地演示数据并重新初始化，可以使用：

```powershell
docker compose down -v
docker compose up --build
```

> `docker compose down -v` 会永久删除该 Compose 项目的 MySQL 数据卷，仅应在确认不需要保留数据时使用。

## 本地开发启动

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.x
- Node.js 20+
- npm 10+

### 1. 初始化数据库

默认连接信息：

```text
数据库：petclinic
用户名：root
密码：root
```

在 MySQL 客户端中依次执行：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/data.sql
```

也可以只启动 Compose 中的 MySQL：

```powershell
docker compose up -d mysql
```

如果使用应用自动初始化，请注意该操作会删除并重建项目表：

```powershell
cd backend
$env:APP_DATABASE_INITIALIZE="true"
mvn spring-boot:run
```

初始化完成后停止应用，并清除环境变量再重新启动：

```powershell
Remove-Item Env:APP_DATABASE_INITIALIZE
mvn spring-boot:run
```

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

### 3. 启动前端

在另一个终端中执行：

```powershell
cd frontend
npm ci
npm run dev
```

前端默认监听 `http://localhost:5173`，开发服务器会将 `/api` 代理到 `http://127.0.0.1:8080`。

## 演示账号与注册

初始化数据提供以下账号，密码均为 `123456`：

| 账号 | 类型 |
| --- | --- |
| `admin` | 管理员 |
| `staff` | 诊所员工 |
| `owner_a` | 宠物主人 |
| `owner_b` | 宠物主人 |

也可以在登录页点击“立即注册”创建新的宠物主人账号。注册需要填写姓名、登录账号、手机号和两次密码，邮箱与地址可选。

注册接口示例：

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "owner_test",
  "password": "123456",
  "confirmPassword": "123456",
  "displayName": "测试主人",
  "phone": "13812345678",
  "email": "owner_test@example.com",
  "address": "上海市浦东新区"
}
```

注册成功返回 HTTP `201`、JWT 和用户资料。

## AI 诊疗助手（可选）

未配置模型时，普通业务和系统启动不受影响。若需要启用 AI 功能：

1. 将根目录 `.env.example` 复制为 `.env`。
2. 填写真实的 `DEEPSEEK_API_KEY`。
3. 保持 `AI_CHAT_MODEL=openai`，因为 Spring AI 通过 OpenAI 兼容协议访问 DeepSeek。
4. 重新启动后端或执行 `docker compose up --build`。

```dotenv
AI_CHAT_MODEL=openai
DEEPSEEK_API_KEY=your-api-key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-pro
```

不要提交 `.env` 或真实 API Key。AI 模块源码位于 `backend/src/main/java/com/zzy/petclinic/ai`。

## 配置项

后端支持以下主要环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_URL` | 本地 `petclinic` MySQL URL | JDBC 地址 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root` | 数据库密码 |
| `APP_DATABASE_INITIALIZE` | `false` | 启动时重建并初始化表，具有破坏性 |
| `JWT_SECRET` | 内置演示密钥 | JWT 签名密钥，生产环境必须替换 |
| `JWT_EXPIRATION_MINUTES` | `480` | Token 有效期，单位分钟 |
| `AI_CHAT_MODEL` | `none` | 设为 `openai` 时启用 AI ChatModel |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | OpenAI 兼容接口地址 |
| `DEEPSEEK_MODEL` | `deepseek-v4-pro` | 模型名称 |

## 接口约定

除登录、注册、Swagger 和健康检查外，其余接口均需要 JWT。请求示例：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

统一响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-09-19T12:00:00"
}
```

分页响应的 `data`：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

主要接口前缀：

| 模块 | 前缀 |
| --- | --- |
| 认证与注册 | `/api/auth` |
| 主人、宠物 | `/api/owners`、`/api/pets` |
| 宠物类型、专长 | `/api/pet-types`、`/api/specialties` |
| 兽医、排班 | `/api/vets`、`/api/slots` |
| 预约、病历、疫苗 | `/api/visits`、`/api/medical-records`、`/api/vaccinations` |
| 公告、反馈 | `/api/notices`、`/api/feedback` |
| 看板、日志 | `/api/dashboard`、`/api/operation-logs` |
| 用户、角色、权限 | `/api/system/users`、`/api/system/roles`、`/api/system/permissions` |
| AI 助手 | `/api/ai` |

字段、状态码和完整请求模型以 Swagger UI 为准。

## 工程结构

```text
PetClinic/
├─ backend/
│  ├─ src/main/java/com/zzy/petclinic/
│  │  ├─ rbac/                 # 认证、授权、用户、角色和权限
│  │  ├─ owner/ pet/ catalog/  # 主人、宠物和基础目录
│  │  ├─ vet/ schedule/ visit/ # 兽医、排班和预约
│  │  ├─ medical/ vaccination/ # 病历和疫苗
│  │  ├─ notice/ feedback/     # 公告和反馈
│  │  ├─ dashboard/ audit/     # 看板和操作日志
│  │  ├─ ai/                   # AI 对话与工具调用
│  │  └─ common/ config/       # 公共模型、异常和配置
│  ├─ src/main/resources/
│  │  ├─ db/                   # 建表和演示数据 SQL
│  │  └─ mapper/               # MyBatis XML
│  └─ src/test/                # 单元、接口和集成测试
├─ frontend/
│  └─ src/
│     ├─ api/ stores/ router/  # 请求、状态与路由
│     ├─ components/ layout/   # 公共组件和布局
│     ├─ views/                # 登录、注册及业务页面
│     └─ styles/ types/        # 全局样式和类型
├─ docs/                       # 补充技术文档
├─ docker-compose.yml
└─ .env.example
```

## 测试与构建

后端完整测试：

```powershell
cd backend
mvn test
```

当前测试套件共 `117` 项，覆盖认证授权、用户角色、宠物、预约事务、AI 会话和 Mapper 集成等场景。

前端类型检查：

```powershell
cd frontend
npm run test
```

前端生产构建：

```powershell
cd frontend
npm run build
```

## 安全说明

- 演示账号、默认数据库密码和默认 JWT 密钥仅用于本地学习。
- 部署到公开环境前必须替换数据库密码与 `JWT_SECRET`，并通过 HTTPS 提供服务。
- 自助注册目前未包含短信/邮箱验证、验证码、限流和账号找回流程。
- Swagger UI 默认开启；生产环境应根据需要关闭或增加访问控制。
- AI Key 只能通过环境变量或密钥管理服务注入，不能写入源码或提交到 Git。
