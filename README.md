# Nine Tower — Ansible Playbook 可视化开发系统

用于**开发和管理 Ansible Playbook** 的 Web 系统。提供可视化的 Playbook 开发环境，覆盖主机组、Role、Task、Handler、Template、文件、变量、环境、标签和 Playbook 的完整生命周期。系统只生成 YAML，不执行 Playbook。

单租户，面向内部团队使用。

## 功能

- **用户与项目**：注册 / 登录 / JWT；项目 CRUD + 成员管理（ADMIN / MEMBER）
- **主机管理**：主机组 & 主机，SSH 凭据 AES-256 加密存储，API 返回脱敏
- **Role**：Task（含 `block / rescue / always` 嵌套与拖拽排序）、Handler（带 notify 关联视图）、Template（.j2 在线 CodeMirror 编辑）、Role Files（目录树 + 上传下载）、vars / defaults
- **变量系统**：项目 / 主机组 / 环境级变量 + Role vars / defaults 四级优先级，跨 scope 同名时标注胜出值
- **Tag / Environment**：Tag 关联 Task；Environment 含可编辑的配置项
- **Playbook 构建器**：可视化组合主机组 + Role（上下排序）+ Tag + Environment + extraVars，实时预览 / 导出合并后的 YAML

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA |
| 数据库 | PostgreSQL |
| 前端 | React 18 · TypeScript · Vite · Ant Design 5 · Zustand · React Router 6 |
| 编辑器 | CodeMirror 6（YAML / Jinja2） |
| 测试 | JUnit 5 + Mockito · Testcontainers · Vitest + React Testing Library · Playwright |

## 架构

```
┌────────────────────────────────────────────────┐
│  React 18 + Ant Design 5（Vite, SPA）          │
└──────────────────┬─────────────────────────────┘
                   │ HTTP / REST + JWT Bearer Token
┌──────────────────▼─────────────────────────────┐
│  Spring Boot 3.x + Java 21（Maven 单模块）     │
│  com.ansible.{module}.{entity|repository|      │
│                         service|controller}    │
└──────────────────┬─────────────────────────────┘
                   │ Spring Data JPA
┌──────────────────▼─────────────────────────────┐
│  PostgreSQL                                    │
└────────────────────────────────────────────────┘
```

## 快速开始

### 环境要求

- Java 21+
- Maven 3.8+
- Node.js 18+（开发机可用 20/22 LTS）
- PostgreSQL（需预先创建数据库 `ansible_dev`，默认账号 `ansible/ansible`）

### 启动

```bash
# 后端（默认 8080，dev profile 提供 JWT/加密密钥默认值）
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端（默认 5173，/api 代理到 8080）
cd frontend
npm install
npm run dev
```

打开 <http://localhost:5173> 即可访问。

### 生产环境变量

生产启动**必须**通过环境变量提供：

| 变量 | 说明 |
|---|---|
| `JWT_SECRET` | JWT 签名密钥（≥ 256 bits） |
| `ENCRYPTION_KEY` | AES-256 加密密钥（base64 编码） |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号（可选，默认 `ansible/ansible`） |

缺少 `JWT_SECRET` 或 `ENCRYPTION_KEY` 时应用会 fail-fast 拒绝启动。

## 常用命令

### 后端（`cd backend`）

| 命令 | 说明 |
|---|---|
| `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | 本地启动 |
| `mvn test` | 运行单元测试 |
| `mvn verify` | 单元 + Testcontainers 集成测试 + Checkstyle / PMD / SpotBugs |
| `mvn spotless:apply` | 代码格式化（Google Java Format） |
| `mvn dependency-check:check` | OWASP 依赖安全扫描（本地按需） |

### 前端（`cd frontend`）

| 命令 | 说明 |
|---|---|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 生产构建（`tsc` + `vite build`） |
| `npm run test` | Vitest 单元 / 组件测试 |
| `npm run test:e2e` | Playwright 端到端测试（需先启动 backend + frontend） |
| `npm run lint` | ESLint（`--max-warnings 0`） |
| `npm run format` | Prettier |

## 目录结构

```
nine-tower/
├── backend/                       # Spring Boot 后端（Maven 单模块）
│   └── src/main/java/com/ansible/
│       ├── common/                # BaseEntity / Result / GlobalExceptionHandler / 枚举
│       ├── security/              # SecurityConfig / JWT / ProjectAccessChecker
│       ├── user/                  # 用户 + 认证
│       ├── project/               # 项目 + 成员
│       ├── host/                  # 主机组 + 主机
│       ├── role/                  # Role + Task + Handler + Template + RoleFile + Vars/Defaults
│       ├── variable/              # 项目/主机组/环境级变量
│       ├── environment/           # Environment + EnvConfig
│       ├── tag/                   # 标签 + TaskTag
│       └── playbook/              # Playbook + YAML 生成器
├── frontend/
│   ├── src/
│   │   ├── api/                   # Axios 实例 + 各模块 API
│   │   ├── stores/                # authStore / projectStore（Zustand）
│   │   ├── pages/                 # 按模块组织的页面
│   │   ├── components/            # 共享组件（Layout / Editor 等）
│   │   └── types/                 # TypeScript 类型
│   └── e2e/                       # Playwright 测试
├── docs/superpowers/
│   ├── specs/                     # 设计文档
│   └── plans/                     # 分阶段实施计划
├── .github/workflows/ci.yml       # 后端 + 前端 CI
└── CLAUDE.md                      # Claude Code 协作指引
```

## 测试

| 维度 | 框架 | 规模 |
|---|---|---|
| 后端单元测试 | JUnit 5 + Mockito | 18 个 Service 测试类 |
| 后端集成测试 | `@SpringBootTest` + Testcontainers（PostgreSQL） | 18 个 Controller 测试类 |
| 前端组件 / 工具 | Vitest + React Testing Library | 23 文件 / 112 用例 |
| E2E | Playwright（Chromium） | 12 文件 / 18 用例 |

```bash
# 后端全量
cd backend && mvn verify

# 前端单元
cd frontend && npm run test

# 前端 E2E（需要 backend + frontend 都在运行）
cd frontend && npm run test:e2e
```

## 权限模型

| 角色 | 权限 |
|---|---|
| `PROJECT_ADMIN` | 项目完整权限：删项目、改成员、覆盖所有数据 |
| `PROJECT_MEMBER` | 使用项目所有功能，编辑 / 删除仅限自己创建的数据 |

- 用户只能访问自己参与的项目
- 无系统级超级管理员
- 所有 API 依次校验：JWT 有效 → 项目成员 → （编辑/删除）数据创建者或 PROJECT_ADMIN

## CI

GitHub Actions 在 push 到 `main` 和每个 PR 上并行运行两个 job：

- **Backend**：JDK 21 + `mvn verify`（Checkstyle / PMD / SpotBugs + 单元 + Testcontainers 集成测试）
- **Frontend**：Node 20 + `npm ci` + lint + vitest + build

OWASP dependency-check 在 CI 中跳过（避免 NVD 限流），建议按需在本地或独立定时任务中运行。

## 文档

- 完整设计文档：[`docs/superpowers/specs/2026-04-10-ansible-playbook-system-design.md`](docs/superpowers/specs/2026-04-10-ansible-playbook-system-design.md)
- 分阶段实施计划：[`docs/superpowers/plans/`](docs/superpowers/plans/)
- Claude Code 协作指引：[`CLAUDE.md`](CLAUDE.md)
