# 变量管理重新设计

**日期：** 2026-04-28
**状态：** 已确认，待实施

---

## 1. 背景与问题

当前变量管理存在两套独立体系：

| 体系 | 数据表 | API 前缀 | UI 入口 |
|------|--------|----------|---------|
| 通用变量 | `variables`（scope 多态） | `/api/projects/{id}/variables` | 变量管理页面 |
| 角色变量 | `role_variables` + `role_default_variables` | `/api/roles/{id}/vars`、`/api/roles/{id}/defaults` | Role 详情页标签页 |

问题：
- 两套 API、两套 Service、两套前端类型定义，维护成本高
- 变量探测的批量保存映射不清晰（`saveAs: VARIABLE | ROLE_VARIABLE` 与 scope 不对应）
- VariableManager 页面 902 行，职责过重
- 优先级展示（前端 5 级）与实际合并逻辑（后端 6 级含 ExtraVars）不一致

---

## 2. 设计目标

1. **统一数据模型**：一张表、一套 API、一套前端类型
2. **统一 UI 入口**：所有变量在 `/projects/:id/variables` 页面管理
3. **简化变量体系**：保留 5 级 scope，对齐简化的 Ansible 变量覆盖
4. **保留变量探测**：简化交互，统一保存路径

---

## 3. 数据模型

### 3.1 新表结构

废弃 `variables`、`role_variables`、`role_default_variables` 三张表，合并为一张：

```sql
CREATE TABLE variables (
    id          BIGSERIAL PRIMARY KEY,
    scope       VARCHAR(20) NOT NULL,
    scope_id    BIGINT NOT NULL,
    key         VARCHAR(200) NOT NULL,
    value       TEXT,
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_variables_scope_key UNIQUE (scope, scope_id, key)
);
```

### 3.2 Scope 枚举

```java
public enum VariableScope {
    PROJECT,        // scope_id = projectId
    HOSTGROUP,      // scope_id = hostGroupId
    ENVIRONMENT,    // scope_id = environmentId
    ROLE_VARS,      // scope_id = roleId（对应 Ansible roles/<role>/vars/main.yml）
    ROLE_DEFAULTS   // scope_id = roleId（对应 Ansible roles/<role>/defaults/main.yml）
}
```

### 3.3 变量优先级（从低到高）

```
ROLE_DEFAULTS (0) < ROLE_VARS (1) < PROJECT (2) < HOSTGROUP (3) < ENVIRONMENT (4)
```

Playbook ExtraVars 不作为变量系统的一部分，保留为 Playbook 实体自身的字段。

### 3.4 数据迁移

1. 从旧 `variables` 表迁移数据（scope 值不变，scope_id 不变）
2. 从旧 `role_variables` 表迁移：scope = `ROLE_VARS`，scope_id = role_id
3. 从旧 `role_default_variables` 表迁移：scope = `ROLE_DEFAULTS`，scope_id = role_id
4. 确认迁移完成后，删除旧表

迁移 SQL：
```sql
-- Step 1: 创建新表
CREATE TABLE variables_new (...);

-- Step 2: 迁移通用变量
INSERT INTO variables_new (id, scope, scope_id, key, value, created_by, created_at, updated_at)
SELECT id, scope, scope_id, key, value, created_by, created_at, updated_at FROM variables;

-- Step 3: 迁移 Role Vars
INSERT INTO variables_new (scope, scope_id, key, value, created_by, created_at, updated_at)
SELECT 'ROLE_VARS', role_id, variable_key, value, created_by, created_at, updated_at FROM role_variables;

-- Step 4: 迁移 Role Defaults
INSERT INTO variables_new (scope, scope_id, key, value, created_by, created_at, updated_at)
SELECT 'ROLE_DEFAULTS', role_id, variable_key, value, created_by, created_at, updated_at FROM role_default_variables;

-- Step 5: 重命名
DROP TABLE variables;
DROP TABLE role_variables;
DROP TABLE role_default_variables;
ALTER TABLE variables_new RENAME TO variables;
```

---

## 4. 后端 API 设计

### 4.1 废弃的 API

以下 API 端点将被移除：

- `POST /api/roles/{roleId}/vars`
- `GET /api/roles/{roleId}/vars`
- `PUT /api/role-vars/{id}`
- `DELETE /api/role-vars/{id}`
- `POST /api/roles/{roleId}/defaults`
- `GET /api/roles/{roleId}/defaults`
- `PUT /api/role-defaults/{id}`
- `DELETE /api/role-defaults/{id}`

移除的 Controller：`RoleVariableController`、`RoleDefaultVariableController`。
移除的 Service：`RoleVariableService`、`RoleDefaultVariableService`。
移除的 Repository：`RoleVariableRepository`、`RoleDefaultVariableRepository`。
移除的 Entity：`RoleVariable`、`RoleDefaultVariable`。

### 4.2 统一 CRUD API

```
POST   /api/projects/{projectId}/variables
GET    /api/projects/{projectId}/variables
GET    /api/variables/{id}
PUT    /api/variables/{id}
DELETE /api/variables/{id}
```

**GET /api/projects/{projectId}/variables** 查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `scope` | 否 | 按作用域过滤 |
| `scopeId` | 否 | 按具体实体 ID 过滤（需配合 scope 使用） |

不传参数时返回项目下所有变量（包括关联主机组、环境、角色的变量）。

**POST 请求体**：
```json
{
  "scope": "ROLE_VARS",
  "scopeId": 3,
  "key": "app_port",
  "value": "8080"
}
```

**PUT 请求体**：
```json
{
  "key": "app_port",
  "value": "8080"
}
```

### 4.3 变量探测 API

```
GET  /api/projects/{projectId}/detect-variables
POST /api/projects/{projectId}/variables/batch
```

探测逻辑不变，批量保存简化：

**POST /api/projects/{projectId}/variables/batch 请求体**：
```json
[
  {"key": "app_port", "scope": "PROJECT", "value": "8080"},
  {"key": "db_host", "scope": "ROLE_VARS", "scopeId": 1, "value": "localhost"},
  {"key": "db_port", "scope": "ROLE_DEFAULTS", "scopeId": 1, "value": "3306"}
]
```

不再需要 `saveAs` 字段，直接用 `scope` 指定作用域。scopeId 在 PROJECT scope 下可选（自动使用 projectId）。

### 4.4 后端包结构

保留 `com.ansible.variable` 包，移除 role 包中变量相关代码：

```
com.ansible.variable/
├── entity/
│   ├── Variable.java          # 统一变量实体
│   └── VariableScope.java     # 5 值枚举
├── repository/
│   └── VariableRepository.java
├── service/
│   ├── VariableService.java           # 统一 CRUD
│   └── VariableDetectionService.java  # 探测（逻辑基本不变）
├── controller/
│   ├── VariableController.java        # CRUD + 查询
│   └── VariableDetectionController.java  # 探测 + 批量保存
└── dto/
    ├── CreateVariableRequest.java
    ├── UpdateVariableRequest.java
    ├── VariableResponse.java
    ├── DetectedVariableResponse.java
    ├── VariableOccurrence.java
    └── BatchVariableSaveRequest.java
```

### 4.5 VariableService 关键逻辑

**resolveProjectId**：根据 scope + scopeId 反查 projectId，用于权限校验：
- PROJECT → scopeId 即 projectId
- HOSTGROUP → 通过 HostGroupRepository 查 projectId
- ENVIRONMENT → 通过 EnvironmentRepository 查 projectId
- ROLE_VARS / ROLE_DEFAULTS → 通过 RoleRepository 查 projectId

**listVariables(projectId)**：返回项目下所有变量，需要：
- scope=PROJECT 且 scopeId=projectId
- scope=HOSTGROUP 且 scopeId 在项目主机组 ID 列表中
- scope=ENVIRONMENT 且 scopeId 在项目环境 ID 列表中
- scope=ROLE_VARS / ROLE_DEFAULTS 且 scopeId 在项目角色 ID 列表中

或者使用 scope + scopeId 参数精确过滤。

### 4.6 Playbook YAML 生成

`PlaybookService.collectMergedVars` 更新为使用新的统一 VariableRepository：
1. ROLE_DEFAULTS（scope=ROLE_DEFAULTS，scopeId=roleId）
2. ROLE_VARS（scope=ROLE_VARS，scopeId=roleId）
3. PROJECT（scope=PROJECT，scopeId=projectId）
4. HOSTGROUP（scope=HOSTGROUP，scopeId=hostGroupId）
5. ENVIRONMENT（scope=ENVIRONMENT，scopeId=environmentId）+ EnvConfig
6. Playbook ExtraVars

合并逻辑不变，只是数据来源统一为一张表。

### 4.7 清理逻辑

`ProjectCleanupService` 更新：
- 项目删除 → 删除 scope=PROJECT 且 scopeId=projectId 的变量
- 主机组删除 → 删除 scope=HOSTGROUP 且 scopeId=hostGroupId 的变量
- 环境删除 → 删除 scope=ENVIRONMENT 且 scopeId=envId 的变量
- 角色删除 → 删除 scope in (ROLE_VARS, ROLE_DEFAULTS) 且 scopeId=roleId 的变量

---

## 5. 前端设计

### 5.1 统一页面入口

所有变量通过 `/projects/:id/variables` 管理。Role 详情页移除 Vars 和 Defaults 标签页。

### 5.2 页面布局

保持现有的两种视图模式（通过 Segmented 切换）：

**表格视图**：选择一个 scope，展示该作用域下的所有变量。

**树形视图**：按作用域分组展示所有变量，同名变量标注优先级覆盖关系。

### 5.3 新增/编辑 Modal

统一的 Modal 表单：
- Scope 选择（5 个选项）
- ScopeId 选择（根据 scope 动态展示：主机组下拉 / 环境下拉 / 角色下拉；PROJECT 时隐藏）
- Key 输入
- Value 输入

编辑时 scope 和 scopeId 不可更改，只允许修改 key 和 value。

### 5.4 变量探测

保留在变量管理页面，简化为：
- 探测结果表格中，scope 选择使用统一的 5 值枚举
- 批量保存使用新的简化 API（直接指定 scope + scopeId）

### 5.5 类型定义

```typescript
// types/entity/Variable.ts
type VariableScope = "PROJECT" | "HOSTGROUP" | "ENVIRONMENT" | "ROLE_VARS" | "ROLE_DEFAULTS";

interface Variable {
  id: number;
  scope: VariableScope;
  scopeId: number;
  key: string;
  value: string | null;
  createdAt: string;
  updatedAt: string;
}
```

删除 `types/entity/RoleVariable.ts` 和 `api/roleVariable.ts`。

### 5.6 优先级工具

`utils/variablePriority.ts` 更新：
- `VariableScopeKind` 扩展为 5 值
- 优先级：ENVIRONMENT(4) > HOSTGROUP(3) > PROJECT(2) > ROLE_VARS(1) > ROLE_DEFAULTS(0)

---

## 6. 测试范围

### 后端

- VariableService 单元测试：CRUD、重复 key 校验、权限校验、resolveProjectId
- VariableDetectionService 单元测试：探测逻辑不变，批量保存使用新 DTO
- VariableController 集成测试：CRUD 端点、查询过滤
- VariableDetectionController 集成测试：探测端点、批量保存端点
- PlaybookService 测试：验证合并逻辑在新表结构下正确
- 数据迁移验证

### 前端

- VariableManager 组件测试
- API 层测试
- variablePriority 工具测试

---

## 7. 迁移影响范围

### 需要修改的文件

**后端（删除）**：
- `role/entity/RoleVariable.java`
- `role/entity/RoleDefaultVariable.java`
- `role/repository/RoleVariableRepository.java`
- `role/repository/RoleDefaultVariableRepository.java`
- `role/service/RoleVariableService.java`
- `role/service/RoleDefaultVariableService.java`
- `role/controller/RoleVariableController.java`
- `role/controller/RoleDefaultVariableController.java`
- `role/dto/CreateRoleVariableRequest.java`
- `role/dto/UpdateRoleVariableRequest.java`
- `role/dto/RoleVariableResponse.java`
- `role/dto/CreateRoleDefaultVariableRequest.java`
- `role/dto/UpdateRoleDefaultVariableRequest.java`
- `role/dto/RoleDefaultVariableResponse.java`

**后端（修改）**：
- `variable/entity/Variable.java` — 新增 ROLE_VARS/ROLE_DEFAULTS scope
- `variable/entity/VariableScope.java` — 枚举扩展
- `variable/repository/VariableRepository.java` — 新增查询方法
- `variable/service/VariableService.java` — 统一 CRUD 逻辑
- `variable/service/VariableDetectionService.java` — 批量保存逻辑更新
- `variable/controller/VariableController.java` — 查询参数扩展
- `variable/controller/VariableDetectionController.java` — 批量保存端点更新
- `variable/dto/CreateVariableRequest.java` — scopeId 变更
- `variable/dto/BatchVariableSaveRequest.java` — 去掉 saveAs，直接用 scope
- `playbook/service/PlaybookService.java` — collectMergedVars 更新
- `project/service/ProjectCleanupService.java` — 清理逻辑更新

**前端（删除）**：
- `types/entity/RoleVariable.ts`
- `api/roleVariable.ts`
- `pages/role/RoleVars.tsx`
- `pages/role/RoleDefaults.tsx`

**前端（修改）**：
- `types/entity/Variable.ts` — scope 扩展
- `api/variable.ts` — 统一 API
- `pages/variable/VariableManager.tsx` — 重新设计
- `pages/role/RoleDetail.tsx` — 移除 Vars/Defaults 标签页
- `utils/variablePriority.ts` — 优先级更新
