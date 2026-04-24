# 代码评审修复计划设计

> 日期：2026-04-24
> 来源：code-review-2026-04-24.md（5 Agent 并行评审，38 项发现）

## 修复策略

按 P0→P1→P2→P3 优先级批次推进。每个批次内部按依赖关系排序，无依赖的任务拆分为并行流（dispatching-parallel-agents）。每个批次完成后运行全量测试验证。

---

## 依赖关系分析

### 跨批次依赖

- **C8（统一 API 返回模式）→ H9, H12, H14, H13, Store 测试**：前端 API 层重构是后续所有前端修复的基础。C8 必须先完成，否则后续修改会在错误的 API 模式上工作。
- **C1-C5（级联删除）→ 独立**：与其他所有修复无依赖。
- **C6, C7（权限校验）→ 独立**：与其他修复无依赖。
- **H8（JWT 密钥缓存）→ P1 第10项（JWT 测试）**：先修代码再写测试，避免测试写完又改。

### 批次内依赖

- P0 Stream A 内：C1-C5 需先建级联删除基础设施（ProjectCleanupService），再逐模块接入
- P1 Stream B 内：4 个 N+1 查询互相独立，可完全并行
- P1 Stream A 内：H8（JWT 密钥缓存）完成后，再写 JWT/Filter 测试（代码先改再测）
- P2 Stream A 内：H10（代码分割）和 H13（代码去重）可并行；H11 和 H9 可并行
- P3 所有项互相独立

---

## P0 — 立即修复（4 组，2 并行流）

### Stream A：后端关键修复（C1-C7）

**C1-C5：级联删除**

方案：创建 `ProjectCleanupService` 统一管理级联删除逻辑，各 Service 的 delete 方法调用它。

删除链路：
1. **Project 删除** → 清理 Member, HostGroup(+Host, Variable(HG)), Role(+Task, Handler, Template, RoleFile, RoleVariable, RoleDefaultVariable), Variable(PROJECT), Environment(+EnvConfig, Variable(ENV)), Tag(+TaskTag, PlaybookTag)
2. **HostGroup 删除** → 清理 Host, Variable(scope=HOSTGROUP)
3. **Role 删除** → 清理 Task(+TaskTag), Handler, Template, RoleFile, RoleVariable, RoleDefaultVariable
4. **Environment 删除** → 清理 EnvConfig, Variable(scope=ENVIRONMENT)（现有代码已清理 EnvConfig，需补充 Variable）
5. **Tag 删除** → 清理 TaskTag, PlaybookTag

实现方式：在 Service 层编排级联调用（非数据库外键），保持与现有 `ddl-auto: update` 模式一致。每个 delete 方法加 `@Transactional`。

需要补充的 Repository 方法：
- `TaskTagRepository.deleteByTagId(Long tagId)`
- `PlaybookTagRepository.deleteByTagId(Long tagId)`
- `TaskRepository.deleteByRoleId(Long roleId)`
- `HandlerRepository.deleteByRoleId(Long roleId)`
- `TemplateRepository.deleteByRoleId(Long roleId)`
- `RoleFileRepository.deleteByRoleId(Long roleId)`
- `RoleVariableRepository.deleteByRoleId(Long roleId)`
- `RoleDefaultVariableRepository.deleteByRoleId(Long roleId)`
- `HostRepository.deleteByHostGroupId(Long hostGroupId)`
- `VariableRepository.deleteByScopeAndScopeId(VariableScope scope, Long scopeId)`

**C6：VariableService 权限校验**

修复：`getVariable`/`updateVariable`/`deleteVariable` 中，先查 Variable 获取 scopeId，再根据 scope 反查 projectId：
- `scope == PROJECT` → scopeId 就是 projectId
- `scope == HOSTGROUP` → 通过 hostGroupRepository 查出 projectId
- `scope == ENVIRONMENT` → 通过 environmentRepository 查出 projectId

然后 `checkMembership(projectId, userId)` 和 `checkOwnerOrAdmin(projectId, ...)`。

**C7：RoleFileService/TemplateService 权限**

- `RoleFileService.getFileContent` / `getFileName`：通过 file 的 roleId → role.projectId，调用 `checkMembership(projectId, userId)`
- `TemplateService.getTemplateName`：通过 template 的 roleId → role.projectId，调用 `checkMembership(projectId, userId)`

### Stream B：前端 API 返回模式统一（C8）

当前状态：
- `auth.ts`/`user.ts` 声明返回 `Promise<ApiResult<T>>`，调用方访问 `res.data.code`/`res.data.data`
- 其他模块在内部 `return res.data`，返回 `Promise<T>`，调用方直接拿到数据

统一方案：让 `auth.ts`/`user.ts` 也采用解包模式，与其他模块一致：
1. 删除 `auth.ts`/`user.ts` 中的 `ApiResult` 接口定义
2. 修改 `auth.ts`/`user.ts` 的 API 函数：`const res = await request.post<T>(...); return res.data;`
3. 修改所有调用方（Login, Register, Header user menu 等），去掉 `.data` 解包

---

## P1 — 尽快修复（6 组，3 并行流）

### Stream A：后端安全与事务（H1, H8, H3）

**H1：认证端点速率限制**

使用 Spring Boot 内置方案（Bucket4j 或简单计数器 Filter）：
- 添加 `RateLimitFilter`：对 `/api/auth/login` 和 `/api/auth/register` 按 IP 限流（如 10 次/分钟）
- 返回 429 Too Many Requests
- 注册到 SecurityConfig 的 filter chain

**H8：JWT 密钥缓存**

在 `JwtTokenProvider` 中用 `@PostConstruct` 初始化 `SecretKey` 为实例字段：
```java
private SecretKey signingKey;

@PostConstruct
void init() {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
}
```
移除 `key()` 方法，所有调用改为直接使用 `signingKey`。

**H3：TagService @Transactional**

给 `createTag`、`updateTag`、`deleteTag` 添加 `@Transactional` 注解。

### Stream B：后端 N+1 查询优化（H4-H7）

**H4：ProjectService.getMyProjects**

方案：新增 Repository 方法 `findAllByMemberUserIdWithRole`（JPQL JOIN FETCH），一次查询返回 Project + Member role。

**H5：ProjectMemberService.listMembers**

方案：新增 Repository 方法（或 JPQL），JOIN FETCH User 信息，避免逐条查询。

**H6：PlaybookService.listPlaybooks（4N+1）**

方案：批量查询替代循环查询。一次查所有 Playbook 的 Role 关联，一次查所有 Role 的 Task/Handler/Template 等，在内存中组装。

**H7：EnvironmentService.listEnvironments**

方案：JPQL JOIN FETCH Environment + EnvConfig，减少查询次数。

### Stream C：前端错误处理（H12, H14）

**H12：补充 try/catch**

为以下位置添加 try/catch + 用户提示（message.error）：
- `RoleTasks.tsx` handleSubmit, handleDelete
- `ProjectSettings.tsx` handleUpdate, handleDelete
- 其他缺少 catch 的 handleSubmit/handleDelete

**H14：clipboard 错误处理**

`RoleTasks.tsx` 和 `PlaybookBuilder.tsx` 的 `handleCopyYaml`：添加 try/catch，失败时 `message.error('复制失败')`。

### Stream A（续）：JWT 安全测试（依赖 H8 完成后）

**P1 第10项：JwtTokenProvider / JwtAuthenticationFilter 测试**

新增测试类：
- `JwtTokenProviderTest`（单元测试）：token 生成、解析、验证、过期处理
- `JwtAuthenticationFilterTest`（单元测试）：filter chain 行为、各种 header 情况

---

## P2 — 计划改进（6 组，2 并行流）

### Stream A：前端架构（H10, H11, H9, H13）

**H10：路由级代码分割**

将 `App.tsx` 中的 13 个静态 import 改为 `React.lazy(() => import(...))` + `<Suspense>` 包裹。

**H11：401/403 改用 React Router 导航**

在 `request.ts` 的 error interceptor 中，用 `window.location.href = '/login'` 替换为通过事件或外部 navigate 函数调用 React Router 导航，避免全页面刷新丢失状态。

方案：在 `request.ts` 中导出一个 `setNavigate` 函数，在 App 层调用注入 React Router 的 `navigate` 实例。

**H9：下载接口认证**

`template.ts` 和 `roleFile.ts` 的下载 URL 当前不携带 JWT。改为使用 `fetch` + `Authorization` header 下载文件，用 Blob URL 触发浏览器下载。或改用短期 token 方案。

**H13：重复代码抽取**

将 `buildArgsJson` 和 `parseArgsToForm` 从 `RoleTasks.tsx`、`RoleHandlers.tsx`、`BlockTasksEditor.tsx` 提取到 `utils/argsParser.ts` 共享模块。

### Stream B：测试补全（Store, API, Validation）

**前端 Store 测试**

新增 `authStore.test.ts`：login/logout/token 持久化/状态更新。
新增 `projectStore.test.ts`：项目切换/状态更新。

**前端 API 层测试**

为剩余 9 个未测模块补测试（playbook, task, role, handler, template, roleFile, tag, variable, environment）。Mock axios，验证调用参数和返回值。

**Bean Validation 边界测试**

后端新增测试：空请求体、超长字符串、非法格式等边界条件。

---

## P3 — 有余力时处理（6 组，2 并行流）

### Stream A：后端（M1, M10, DTO, 用户ID）

**M1：安全审计日志**

添加 `AuditLogService`，在以下场景记录日志：
- 登录失败、Token 验证失败、权限校验失败

**M10：数据库索引**

为高频查询字段添加 `@Index` 注解：
- `hostGroupId`（Host）、`roleId`（Task, Handler, Template, RoleFile, RoleVariable, RoleDefaultVariable）、`playbookId`（PlaybookRole）、`tagId`（TaskTag）、`scope+scopeId`（Variable）

**DTO 风格统一**

将 class DTO 逐步迁移为 record（Java 21 特性）。可在修改 DTO 时顺带重构。

**用户 ID 提取逻辑**

创建自定义参数解析器 `@CurrentUserId`，替代 Controller 中重复的 `userRepository.findByUsername(...)` 模式。

### Stream B：前端 + 工具（M13-M16, 可访问性, 依赖）

**M13：下载 JWT 不放 URL**

配合 H9 的下载改造一并处理。

**M14：路径解析改用 React Router**

`ProjectLayout` 中用 `useParams()` 替代硬编码 `pathParts[3]`。

**M15：VariableManager 批量请求**

将 `2 + roles.length * 2` 个请求合并为后端批量接口，或前端并发控制。

**M16：Modal confirmLoading**

为 `RoleList.tsx`、`TagManager.tsx` 等的提交按钮添加 `confirmLoading` 状态。

**可访问性：** 交互式 div 添加 `role`/`tabIndex`/`onKeyDown`。

**依赖升级：** React 18→19、ESLint 8→9、Vite 5→6（评估后决定）。

---

## 每个批次的完成标准

每个批次完成后必须满足：
1. 所有修改的功能代码通过格式检查（Spotless/ESLint）
2. 所有新增/修改的测试通过
3. 全量后端测试 `mvn test` 通过
4. 全量前端测试 `npm run test` 通过
5. 后端质量门禁通过：`checkstyle:check` + `pmd:check` + `spotbugs:check`
6. 前端质量门禁通过：`npm run lint` + `tsc --noEmit`

---

## 风险与注意事项

- **C8 API 统一** 是前端所有后续修复的前置依赖，必须最先完成且充分测试
- **级联删除** 涉及大量 Repository 新方法，需确保 delete 方法批量执行而非逐条
- **N+1 优化** 的 JPQL 改写需验证返回数据正确性，特别是分页场景
- **代码分割** 可能暴露懒加载顺序问题，需测试所有路由切换
- **依赖升级** 放在最后，避免引入破坏性变更影响其他修复
