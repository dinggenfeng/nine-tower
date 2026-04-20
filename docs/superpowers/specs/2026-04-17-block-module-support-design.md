# Block 模块支持设计

## 概述

在 Task 管理界面中为 `block` Ansible 模块提供完整的嵌套任务编辑能力。block 是 Ansible Playbook 中将多个任务组合的核心语法，支持 `rescue`（错误处理）和 `always`（无论成败都执行）两个可选段。

本设计仅涉及 Task，Handler 不支持 block。

## 需求摘要

- block 模块支持 `block`（必填）、`rescue`（可选）、`always`（可选）三段
- block 内子任务支持完整 Task 属性（name、module、args、when、loop、register 等）
- block 内子任务禁止再选 block 模块（即不支持嵌套 block，只支持一层）
- block 级别支持 when、become、becomeUser、ignoreErrors 四个属性

## 设计决策

- **方案 A（数据库自引用）**：Task 表增加 `parent_task_id` 和 `block_section` 两列，子任务复用 Task 实体。这是关系型建模最自然的方式，复用现有 CRUD，改动量适中。
- **创建/更新策略**：创建 block 时通过请求体传入子任务列表（`blockChildren`）；更新时全量替换旧子任务，前端不需要跟踪子任务 id。
- **查询返回树结构**：GET 只返回顶层任务（`parentTaskId IS NULL`），block 类型附带 `children` 数组。
- **YAML 序列化递归渲染**：检测到 block 模块时，递归渲染子任务列表。

## 1. 数据库 Schema 变更

### Task 表新增列

| 列名 | 类型 | 可空 | 说明 |
|------|------|------|------|
| `parent_task_id` | BIGINT | YES | 指向父 block task 的 ID，顶层任务为 null |
| `block_section` | VARCHAR(20) | YES | BLOCK / RESCUE / ALWAYS，顶层任务为 null |

### 约束规则

- `parent_task_id` 和 `block_section` 要么同时为 null（顶层任务），要么同时非 null（子任务）
- `module = 'block'` 时，该任务的 `args`、`loop`、`until`、`register`、`notify` 字段应为空；前端在提交时应丢弃这些字段
- 子任务的 `taskOrder` 表示在其所属 section 内的排序
- 删除 block task 时，Service 层级联删除所有子任务
- 子任务禁止选择 `block` 模块（数据库层通过应用层校验）

## 2. 后端实体与 API

### Task 实体（Java）

位置：`backend/src/main/java/com/ansible/role/entity/Task.java`

新增两个字段：

```java
@Column(name = "parent_task_id")
private Long parentTaskId;

@Column(name = "block_section", length = 20)
private String blockSection; // "BLOCK" | "RESCUE" | "ALWAYS"
```

### DTO 新增

**BlockChildRequest**（创建/更新子任务）：

```java
public class BlockChildRequest {
    private String section;    // BLOCK / RESCUE / ALWAYS
    private String name;
    private String module;
    private String args;
    private String whenCondition;
    private String loop;
    private String until;
    private String register;
    private String notify;     // JSON array string
    private Integer taskOrder;
    private Boolean become;
    private String becomeUser;
    private Boolean ignoreErrors;
}
```

**CreateTaskRequest** 新增可选字段：

```java
private List<BlockChildRequest> blockChildren;
```

**UpdateTaskRequest** 新增可选字段：

```java
private List<BlockChildRequest> blockChildren;
```

### Service 层逻辑

**创建（TaskService）**：
1. 创建 block task 本身（parentTaskId=null, blockSection=null）
2. 如果传入了 `blockChildren`，逐条创建子任务记录（parentTaskId=blockTaskId, blockSection=对应值）
3. taskOrder 由前端传入，保证同 section 内连续

**更新（TaskService）**：
1. 更新 block task 本身
2. 如果传入了 `blockChildren`：先删除该 block 下所有旧子任务，再批量创建新子任务（全量替换）
3. 如果未传入 `blockChildren`：保持现有子任务不变

**删除（TaskService）**：
- 如果删除的是 block task（module='block'），级联删除所有 parentTaskId 指向它的子任务
- 普通 task 删除不受影响

**查询（TaskService）**：
- 返回所有 `parentTaskId IS NULL` 的 task（顶层任务）
- 对于 module='block' 的任务，额外加载其 children 列表
- children 按 `blockSection` + `taskOrder` 排序

### Controller 层

接口 URL 和 HTTP 方法不变：
- `POST /api/roles/{roleId}/tasks`
- `PUT /api/tasks/{id}`
- `GET /api/roles/{roleId}/tasks`
- `DELETE /api/tasks/{id}`

## 3. 前端类型定义

位置：`frontend/src/types/entity/Task.ts`

```typescript
export type BlockSection = 'BLOCK' | 'RESCUE' | 'ALWAYS';

export interface Task {
  id: number;
  roleId: number;
  name: string;
  module: string;
  args: string;
  whenCondition: string;
  loop: string;
  until: string;
  register: string;
  notify: string[];
  taskOrder: number;
  become: boolean;
  becomeUser: string;
  ignoreErrors: boolean;
  createdBy: number;
  createdAt: string;
  // 新增
  parentTaskId: number | null;
  blockSection: BlockSection | null;
  children: Task[];
}

export interface BlockChildRequest {
  section: BlockSection;
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder: number;
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
}

export interface CreateTaskRequest {
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder: number;
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
  // 新增
  blockChildren?: BlockChildRequest[];
}

export interface UpdateTaskRequest {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder?: number;
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
  // 新增
  blockChildren?: BlockChildRequest[];
}
```

## 4. 前端 UI

### 表单动态切换

当用户选择 `block` 模块时，Task 编辑弹窗中：
- **隐藏**：ModuleParamsGrid、ExtraParamsInput、Loop、Until、Register、Notify（block 内子任务自己有自己的这些属性，不需要在 block 级别设置）
- **显示**：BlockTasksEditor 组件 + block 级别高级选项（when、become、becomeUser、ignoreErrors）

选择非 block 模块时，行为和现在完全一致。

### BlockTasksEditor 组件

位置：`frontend/src/components/role/BlockTasksEditor.tsx`（新建）

使用 Ant Design Tabs 分三个标签页：`block`（必填）、`rescue`（可选）、`always`（可选）。

每个标签页内是子任务卡片列表。子任务卡片可折叠，展开后显示：

- 名称输入框
- 模块选择器（**过滤掉 block 选项**）
- 模块参数字段（复用 ModuleParamsGrid + ExtraParamsInput）
- 高级选项（when、loop、register、become、becomeUser、ignoreErrors）
- 删除按钮

交互：
- 每个 section 支持添加子任务
- 子任务支持上/下排序（箭头按钮）
- block 标签页至少需要一个子任务
- rescue/always 可以为空

### 预览 YAML

当预览 block task 的 YAML 时，需要先在内存中构建任务树，然后递归调用 `blockToYaml` 序列化。

## 5. YAML 序列化

位置：`frontend/src/utils/taskToYaml.ts`

### 新增 blockToYaml 函数

```typescript
function blockToYaml(task: Task): string {
  // 渲染 block task 本身（name + block/rescue/always + when/become/ignore_errors）
  // 递归调用 taskToYaml 渲染每个子任务
}
```

### 输出格式示例

```yaml
- name: Scan and add each host's SSH key to known_hosts
  block:
    - name: Get remote host SSH public key
      command: ssh-keyscan -H {{ item }}
      register: ssh_keyscan_result
      loop: "{{ groups['all'] }}"
    - name: Add SSH key to known_hosts
      known_hosts:
        name: "{{ item }}"
        key: "{{ ssh_keyscan_result.results | map(attribute='stdout') | list | join('\n') }}"
        path: "~/.ssh/known_hosts"
      loop: "{{ groups['all'] }}"
  rescue:
    - name: Handle SSH key scan failure
      debug:
        msg: "Failed to scan SSH keys"
  always:
    - name: Cleanup temp files
      file:
        path: "/tmp/ssh_scan_tmp"
        state: absent
  when: "{{ ssh_scan_enabled | default(false) }}"
  become: true
```

渲染顺序：先 block 段，再 rescue 段（若有），最后 always 段（若有）。空段不渲染。

## 6. 改动范围

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/application.yml` | 无改动 |
| `backend/src/main/java/com/ansible/role/entity/Task.java` | 改动 — 新增 parentTaskId、blockSection 字段 |
| `backend/src/main/java/com/ansible/role/dto/CreateTaskRequest.java` | 改动 — 新增 blockChildren 字段 |
| `backend/src/main/java/com/ansible/role/dto/UpdateTaskRequest.java` | 改动 — 新增 blockChildren 字段 |
| `backend/src/main/java/com/ansible/role/dto/BlockChildRequest.java` | 新建 |
| `backend/src/main/java/com/ansible/role/service/TaskService.java` | 改动 — 创建/更新/删除/查询逻辑增加层级处理 |
| `backend/src/main/java/com/ansible/role/repository/TaskRepository.java` | 改动 — 新增查询方法 |
| `frontend/src/types/entity/Task.ts` | 改动 — 新增字段和类型 |
| `frontend/src/constants/ansibleModules.ts` | 改动 — block 模块定义不变（params 已只有 when 和 ignore_errors） |
| `frontend/src/components/role/BlockTasksEditor.tsx` | 新建 — block 子任务编辑器 |
| `frontend/src/pages/role/RoleTasks.tsx` | 改动 — 表单增加 block 模式切换，集成 BlockTasksEditor |
| `frontend/src/utils/taskToYaml.ts` | 改动 — 新增 blockToYaml 函数，递归渲染 |
| `frontend/src/api/task.ts` | 无需改动（请求体字段已覆盖） |
