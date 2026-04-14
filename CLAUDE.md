# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

Ansible Playbook 可视化开发系统 — 用于开发和管理 Ansible Playbook 的 Web 应用。系统不执行 Playbook，只提供可视化开发环境。单租户，内部团队使用。

前后端同一仓库：`backend/`（Spring Boot）+ `frontend/`（React）。

## 常用命令

### 后端（backend/）

```bash
cd backend
mvn spring-boot:run                    # 启动后端（端口 8080）
mvn test                               # 运行单元测试
mvn verify                             # 运行集成测试（Testcontainers + PostgreSQL）
mvn test -Dtest=UserServiceTest         # 运行单个测试类
mvn test -Dtest=UserServiceTest#testMethod  # 运行单个测试方法
mvn spotless:apply                     # 代码格式化
mvn checkstyle:check                   # 代码格式检查
mvn pmd:check                         # 代码规范检查
mvn spotbugs:check                    # Bug/安全扫描
```

### 前端（frontend/）

```bash
cd frontend
npm install                            # 安装依赖
npm run dev                            # 启动开发服务器（端口 5173，代理 /api 到 8080）
npm run build                          # 生产构建
npm run lint                           # ESLint 检查
npm run format                         # Prettier 格式化
npm run test                           # Vitest 测试
npm run test:watch                     # 测试 watch 模式
```

### 环境要求

- Java 21+、Maven 3.8+、Node.js 18+、PostgreSQL
- 数据库默认配置：`ansible_dev`，用户名/密码 `ansible/ansible`（可通过 `DB_USERNAME`/`DB_PASSWORD` 环境变量覆盖）
- Hibernate ddl-auto: update（自动建表）

## 架构

```
React 18 + Ant Design 5 (Vite, TypeScript, SPA)
        │ HTTP/REST + JWT Bearer Token
Spring Boot 3.x + Java 21 (Maven 单模块)
        │ Spring Data JPA
     PostgreSQL
```

### 后端包结构（com.ansible）

按业务模块组织，每个模块包含 entity/repository/service/controller 四层：

- `common/` — BaseEntity（id, createdAt, updatedAt, createdBy）、Result 统一响应包装、GlobalExceptionHandler、枚举
- `security/` — SecurityConfig（JWT 无状态）、JwtTokenProvider、JwtAuthenticationFilter、ProjectAccessChecker（权限校验）
- `user/` — 用户注册/登录/JWT
- `project/` — 项目 CRUD + 成员管理（ADMIN/MEMBER 角色）
- 后续模块：host/、role/、variable/、environment/、tag/、playbook/

**关键模式：**
- DTO 请求/响应映射（无 MapStruct，Service 层手动转换）
- Jakarta Validation 注解校验请求 DTO
- `@Transactional` 用于 Service 层业务逻辑
- 密码 BCrypt 加密，JWT 8 小时过期

### 前端结构

- `api/` — Axios 实例（`request.ts` 自动注入 JWT Bearer Token，401/403 自动跳转登录）+ 各模块 API
- `stores/` — Zustand 状态管理：`authStore`（Token 存 localStorage）、`projectStore`（当前项目上下文）
- `pages/` — 按模块组织页面组件
- `components/Layout/` — `MainLayout`（认证路由）、`ProjectLayout`（项目嵌套路由）
- `types/` — TypeScript 类型定义

**路由：** `/login`、`/register`（公开）→ `/projects`（列表）→ `/projects/:id/*`（项目内嵌套路由：settings、members 等）

### 权限模型

- PROJECT_ADMIN：项目管理员，完整权限
- PROJECT_MEMBER：项目成员，只读或受限操作
- 通过 `ProjectAccessChecker` 在后端校验

## 开发规范

### 细粒度迭代（核心规范）

每次只实现一个功能单元，闭环后再进行下一个。一个功能单元的完成标准：
1. 功能代码完成
2. Service 层单元测试通过
3. Controller 层集成测试通过（Testcontainers）
4. 代码格式/规范/安全扫描全部通过
5. **严禁批量实现功能后再补测试**

### 测试规范

- **后端单元测试**：Service 层用 Mockito mock Repository
- **后端集成测试**：Controller 层用 `@SpringBootTest` + Testcontainers（PostgreSQL）
- **前端测试**：Vitest + React Testing Library

### 代码质量

所有代码需通过：Checkstyle、Spotless、PMD、SpotBugs（后端）；ESLint、Prettier、TypeScript strict（前端）

## 设计文档

- 完整设计文档：`docs/superpowers/specs/2026-04-10-ansible-playbook-system-design.md`
- 实施计划按阶段存放在：`docs/superpowers/plans/`

## 实施进度

- Phase 1 ✅ 用户认证（注册/登录/JWT）
- Phase 2 ✅ 项目管理 + 成员管理
- Phase 3 ✅ 主机组、主机、Role CRUD
- Phase 4 ✅ Task、Handler CRUD
- 后续：RoleVariable、RoleDefaultVariable、Template、File、Variable、Environment、Tag、Playbook
