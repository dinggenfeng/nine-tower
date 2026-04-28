# Variable Detection Feature Design

## Overview

从 Role 的 Task/Handler/Template 内容中识别 `{{ variable }}` 引用，去重后展示，支持用户调整作用域后批量保存变量。

## Entry Point

变量管理页面 (`VariableManager`) 的【新建变量】按钮右侧新增【变量探测】按钮。

## Backend

### API: Scan Variables

```
GET /api/projects/{projectId}/detect-variables
```

遍历项目下所有 Role 的：
- Task: `name`, `args` (JSON string), `whenCondition`, `loop`
- Handler: `name`, `args` (JSON string), `whenCondition`
- Template: `content` (Jinja2 text)

用正则 `\{\{\s*([\w.]+)\s*\}\}` 提取变量名。排除 Ansible 内置变量：
- `item` 及 `item.*`（loop 内置）
- `ansible_*`（facts 变量）

Role 内按 key 去重，Role 间不合并（保留各自来源）。

**Response:**
```json
[
  {
    "key": "app_port",
    "occurrences": [
      {"roleId": 1, "roleName": "web", "type": "TASK", "entityId": 10, "entityName": "配置应用", "field": "args"},
      {"roleId": 2, "roleName": "api", "type": "TEMPLATE", "entityId": 5, "entityName": "nginx.conf", "field": "content"}
    ],
    "suggestedScope": "PROJECT"
  }
]
```

`suggestedScope` 推测规则：变量只在一个 Role 出现 → `ROLE`；在多个 Role 出现 → `PROJECT`。

### API: Batch Save

```
POST /api/projects/{projectId}/variables/batch
```

**Request body:**
```json
[
  {
    "key": "app_port",
    "saveAs": "VARIABLE",
    "scope": "PROJECT",
    "value": "8080"
  },
  {
    "key": "db_host",
    "saveAs": "ROLE_VARIABLE",
    "roleId": 1,
    "value": "localhost"
  }
]
```

`saveAs`: `VARIABLE` → 保存到 `variables` 表（项目级/主机组级/环境级）；`ROLE_VARIABLE` → 保存到 `role_variables` 表。

**Validation:**
- 项目级变量：key 在项目内唯一（查 `variables` 表 `scope=PROJECT, scopeId=projectId`）
- Role 级变量：key 在 Role 内唯一（查 `role_variables` 表 `roleId`）
- 冲突的行返回 error，不影响其他行

### New Classes

- `VariableDetectionService` (`variable/service/`) — 扫描逻辑
- `VariableDetectionController` (`variable/controller/`) — 两个端点
- DTOs: `DetectedVariableResponse`, `VariableOccurrence`, `BatchVariableSaveRequest`

## Frontend

### VariableManager 改动

-【新建变量】按钮右侧新增【变量探测】按钮（dashed border）
- 点击后调用 `GET /api/projects/{projectId}/detect-variables`
- 结果在页面下方以表格展示（或 Drawer 内）

### 结果表格列

| 列 | 说明 |
|----|------|
| 变量名 | `code` 样式显示 |
| 来源 | Role · 类型 · 实体名，多来源分行显示 |
| 作用域 | 下拉选择：项目级 / Role · \<Role名\>。推测值默认选中 |
| 值（可选） | 文本输入框 |
| 操作 | 复制 + 删除按钮 |

### 复制功能

- 项目级变量 → 仅可复制为 Role 级（弹出选择 Role）
- Role 级变量 → 可复制为 Role 级或项目级（弹出选择目标类型+Role）
- 复制后新增一行，原行不变

### 批量保存

- 前端先行检查同批次内重复 key + 作用域组合
- 调用 `POST /api/projects/{projectId}/variables/batch`
- 后端校验唯一性，逐条返回成功/失败
- 成功：变量写入数据库，结果行消失
- 失败：行标红，显示错误提示

## Data Flow

```
User clicks [变量探测]
  → GET /detect-variables
  → Display table with auto-inferred scope
  → User adjusts scope, fills values, copies/deletes rows
  → User clicks [批量保存]
  → POST /variables/batch
  → Refresh variable list
```

## Testing

- Service unit tests: regex extraction accuracy, scope inference, dedup logic
- Controller integration tests: scan endpoint, batch save endpoint, uniqueness validation
- Frontend: result table rendering, scope dropdown, copy modal, batch save flow
