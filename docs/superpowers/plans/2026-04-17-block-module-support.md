# Block 模块支持实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Task 管理界面中为 Ansible `block` 模块提供完整的嵌套任务编辑能力，支持 block/rescue/always 三段，子任务支持完整属性，禁止嵌套 block。

**Architecture:** 数据库自引用方案 — Task 表新增 `parent_task_id` 和 `block_section` 两列表达层级关系；Service 层处理创建/更新/删除/查询时的父子逻辑；前端在选择 block 模块时动态切换表单，显示 BlockTasksEditor 子任务编辑器。

**Tech Stack:** Spring Boot（JPA + Validation）、React（Ant Design 5 + TypeScript）

---

## 文件变更总览

| 文件 | 改动 |
|------|------|
| `backend/.../entity/Task.java` | 新增 parentTaskId、blockSection 字段 |
| `backend/.../dto/BlockChildRequest.java` | 新建 — 子任务创建 DTO |
| `backend/.../dto/CreateTaskRequest.java` | 新增 blockChildren 字段 |
| `backend/.../dto/UpdateTaskRequest.java` | 新增 blockChildren 字段 |
| `backend/.../dto/TaskResponse.java` | 新增 parentTaskId、blockSection、children 字段 |
| `backend/.../repository/TaskRepository.java` | 新增按 parentTaskId 查询方法 |
| `backend/.../service/TaskService.java` | 创建/更新/删除/查询增加层级处理 |
| `frontend/src/types/entity/Task.ts` | 新增 BlockSection 类型和 Task/Request 类型字段 |
| `frontend/src/components/role/BlockTasksEditor.tsx` | 新建 — block 子任务编辑器 |
| `frontend/src/pages/role/RoleTasks.tsx` | 表单增加 block 模式切换，集成 BlockTasksEditor |
| `frontend/src/utils/taskToYaml.ts` | 新增 blockToYaml 递归渲染 |

---

## 后端任务

### Task 1: Task 实体新增 parentTaskId 和 blockSection 字段

**Files:**
- Modify: `backend/src/main/java/com/ansible/role/entity/Task.java`

- [ ] **Step 1: 在 Task.java 中新增两个字段**

在 `ignoreErrors` 字段后添加：

```java
@Column(name = "parent_task_id")
private Long parentTaskId;

@Column(name = "block_section", length = 20)
private String blockSection; // "BLOCK" | "RESCUE" | "ALWAYS"
```

- [ ] **Step 2: 运行格式化**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/ansible/role/entity/Task.java
git commit -m "feat(role): add parentTaskId and blockSection to Task entity"
```

---

### Task 2: 新建 BlockChildRequest DTO

**Files:**
- Create: `backend/src/main/java/com/ansible/role/dto/BlockChildRequest.java`

- [ ] **Step 1: 创建 BlockChildRequest.java**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockChildRequest {

  @NotBlank(message = "Section is required")
  private String section; // BLOCK / RESCUE / ALWAYS

  @NotBlank(message = "Child task name is required")
  @Size(max = 200)
  private String name;

  @NotBlank(message = "Module is required")
  @Size(max = 100)
  private String module;

  private String args;

  @Size(max = 500)
  private String whenCondition;

  @Size(max = 500)
  private String loop;

  @Size(max = 500)
  private String until;

  @Size(max = 100)
  private String register;

  private String notify; // JSON array string

  @NotNull(message = "Task order is required")
  private Integer taskOrder;

  private Boolean become;

  @Size(max = 100)
  private String becomeUser;

  private Boolean ignoreErrors;
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/ansible/role/dto/BlockChildRequest.java
git commit -m "feat(role): add BlockChildRequest DTO"
```

---

### Task 3: 扩展 CreateTaskRequest 和 UpdateTaskRequest

**Files:**
- Modify: `backend/src/main/java/com/ansible/role/dto/CreateTaskRequest.java`
- Modify: `backend/src/main/java/com/ansible/role/dto/UpdateTaskRequest.java`

- [ ] **Step 1: 在 CreateTaskRequest.java 的字段列表之后（`ignoreErrors` 字段后）添加**

```java
private java.util.List<com.ansible.role.dto.BlockChildRequest> blockChildren;
```

- [ ] **Step 2: 在 UpdateTaskRequest.java 的字段列表之后（`ignoreErrors` 字段后）添加**

```java
private java.util.List<com.ansible.role.dto.BlockChildRequest> blockChildren;
```

- [ ] **Step 3: 运行格式化**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/ansible/role/dto/CreateTaskRequest.java backend/src/main/java/com/ansible/role/dto/UpdateTaskRequest.java
git commit -m "feat(role): add blockChildren to CreateTaskRequest and UpdateTaskRequest"
```

---

### Task 4: 扩展 TaskResponse（含 children）

**Files:**
- Modify: `backend/src/main/java/com/ansible/role/dto/TaskResponse.java`

TaskResponse 当前使用 `@JsonCreator` 和字段对字段构造，需要改成同时支持从 Task 实体构造（用于列表查询）和手动构造（用于 children 嵌套）。

- [ ] **Step 1: 在 TaskResponse.java 中新增三个字段及对应的构造函数/字段**

在 `createdAt` 字段后添加：

```java
private final Long parentTaskId;
private final String blockSection;
private final java.util.List<TaskResponse> children;
```

在 `@JsonCreator` 方法参数列表中添加 `@JsonProperty("parentTaskId") Long parentTaskId`、`@JsonProperty("blockSection") String blockSection`、`@JsonProperty("children") java.util.List<TaskResponse> children`，并在构造函数体中赋值这三个字段。

在 `public TaskResponse(Task task)` 构造函数中，添加对 `parentTaskId`、`blockSection` 的赋值（从 task.getParentTaskId() 和 task.getBlockSection()），`children` 设为 `null`（children 由 Service 层手动填充）。

- [ ] **Step 2: 运行格式化并检查**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/ansible/role/dto/TaskResponse.java
git commit -m "feat(role): add parentTaskId, blockSection, children to TaskResponse"
```

---

### Task 5: TaskRepository 新增查询方法

**Files:**
- Modify: `backend/src/main/java/com/ansible/role/repository/TaskRepository.java`

- [ ] **Step 1: 新增两个查询方法**

```java
List<Task> findAllByParentTaskIdOrderByTaskOrderAsc(Long parentTaskId);

List<Task> findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(Long roleId);
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/ansible/role/repository/TaskRepository.java
git commit -m "feat(role): add findByParentTaskId and findTopLevelByRoleId to TaskRepository"
```

---

### Task 6: TaskService 层级逻辑

**Files:**
- Modify: `backend/src/main/java/com/ansible/role/service/TaskService.java`

这是最核心的改动。需要修改创建、更新、删除、查询四个方法。

- [ ] **Step 1: 修改 createTask 方法 — 在保存 task 后处理 blockChildren**

在 `Task saved = taskRepository.save(task);` 之后，`return new TaskResponse(saved);` 之前，添加：

```java
if (request.getBlockChildren() != null && !request.getBlockChildren().isEmpty()) {
    for (BlockChildRequest child : request.getBlockChildren()) {
        Task childTask = new Task();
        childTask.setRoleId(roleId);
        childTask.setParentTaskId(saved.getId());
        childTask.setBlockSection(child.getSection());
        childTask.setName(child.getName());
        childTask.setModule(child.getModule());
        childTask.setArgs(child.getArgs());
        childTask.setWhenCondition(child.getWhenCondition());
        childTask.setLoop(child.getLoop());
        childTask.setUntil(child.getUntil());
        childTask.setRegister(child.getRegister());
        childTask.setNotify(child.getNotify());
        childTask.setTaskOrder(child.getTaskOrder());
        childTask.setBecome(child.getBecome());
        childTask.setBecomeUser(child.getBecomeUser());
        childTask.setIgnoreErrors(child.getIgnoreErrors());
        childTask.setCreatedBy(currentUserId);
        taskRepository.save(childTask);
    }
}
```

在方法顶部添加 `BlockChildRequest` 的 import：
```java
import com.ansible.role.dto.BlockChildRequest;
```

- [ ] **Step 2: 修改 getTasksByRole 方法 — 只查顶层任务，block 类型附加 children**

将：
```java
return taskRepository.findAllByRoleIdOrderByTaskOrderAsc(roleId).stream()
    .map(TaskResponse::new)
    .toList();
```

替换为：
```java
List<Task> topLevel = taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(roleId);
return topLevel.stream()
    .map(task -> {
        if ("block".equals(task.getModule())) {
            return buildBlockResponse(task);
        }
        return new TaskResponse(task);
    })
    .toList();
```

添加私有方法 `buildBlockResponse`：
```java
private TaskResponse buildBlockResponse(Task blockTask) {
    TaskResponse response = new TaskResponse(blockTask);
    List<Task> children = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(blockTask.getId());
    List<TaskResponse> childResponses = children.stream()
        .map(TaskResponse::new)
        .toList();
    return new TaskResponse(
        blockTask.getId(),
        blockTask.getRoleId(),
        blockTask.getName(),
        blockTask.getModule(),
        blockTask.getArgs(),
        blockTask.getWhenCondition(),
        blockTask.getLoop(),
        blockTask.getUntil(),
        blockTask.getRegister(),
        parseNotify(blockTask.getNotify()),
        blockTask.getTaskOrder(),
        blockTask.getBecome(),
        blockTask.getBecomeUser(),
        blockTask.getIgnoreErrors(),
        blockTask.getCreatedBy(),
        blockTask.getCreatedAt(),
        blockTask.getParentTaskId(),
        blockTask.getBlockSection(),
        childResponses
    );
}
```

注意：`TaskResponse` 需要一个完整的构造函数接受所有字段（含 parentTaskId、blockSection、children），在 Task 4 中已经添加。

- [ ] **Step 3: 修改 updateTask 方法 — 处理 blockChildren 全量替换**

在 `Task saved = taskRepository.save(task);` 之前，添加：

```java
if (request.getBlockChildren() != null) {
    // Delete all existing children
    List<Task> existingChildren = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(taskId);
    taskRepository.deleteAll(existingChildren);
    // Create new children
    for (BlockChildRequest child : request.getBlockChildren()) {
        Task childTask = new Task();
        childTask.setRoleId(task.getRoleId());
        childTask.setParentTaskId(taskId);
        childTask.setBlockSection(child.getSection());
        childTask.setName(child.getName());
        childTask.setModule(child.getModule());
        childTask.setArgs(child.getArgs());
        childTask.setWhenCondition(child.getWhenCondition());
        childTask.setLoop(child.getLoop());
        childTask.setUntil(child.getUntil());
        childTask.setRegister(child.getRegister());
        childTask.setNotify(child.getNotify());
        childTask.setTaskOrder(child.getTaskOrder());
        childTask.setBecome(child.getBecome());
        childTask.setBecomeUser(child.getBecomeUser());
        childTask.setIgnoreErrors(child.getIgnoreErrors());
        childTask.setCreatedBy(task.getCreatedBy());
        taskRepository.save(childTask);
    }
}
```

- [ ] **Step 4: 修改 deleteTask 方法 — block 类型级联删除子任务**

将 `taskRepository.delete(task);` 替换为：

```java
if ("block".equals(task.getModule())) {
    List<Task> children = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(taskId);
    taskRepository.deleteAll(children);
}
taskRepository.delete(task);
```

- [ ] **Step 5: 添加缺失的 import**

```java
import com.ansible.role.dto.BlockChildRequest;
import java.util.ArrayList; // if not already present
```

- [ ] **Step 6: 运行格式化**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 7: 编译验证**

```bash
cd backend && mvn compile
```

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/ansible/role/service/TaskService.java
git commit -m "feat(role): handle block children in TaskService CRUD operations"
```

---

### Task 7: 后端集成测试（TaskService + Controller）

**Files:**
- Check existing test: `backend/src/test/java/com/ansible/role/service/TaskServiceTest.java`

- [ ] **Step 1: 查看现有 TaskServiceTest.java 的结构**

- [ ] **Step 2: 添加 block task 创建测试**

在 TaskServiceTest 中新增测试方法：

```java
@Test
void createBlockTask_withChildren_savesBlockAndChildren() {
    // Setup: create role first
    Role role = new Role();
    role.setName("test-role");
    role.setProjectId(projectId);
    role = roleRepository.save(role);

    BlockChildRequest child1 = new BlockChildRequest();
    child1.setSection("BLOCK");
    child1.setName("Child task 1");
    child1.setModule("command");
    child1.setTaskOrder(1);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("My Block");
    request.setModule("block");
    request.setTaskOrder(1);
    request.setBlockChildren(List.of(child1));

    TaskResponse response = taskService.createTask(role.getId(), request, userId);

    assertNotNull(response.getId());
    List<TaskResponse> children = response.getChildren();
    assertEquals(1, children.size());
    assertEquals("Child task 1", children.get(0).getName());
    assertEquals("BLOCK", children.get(0).getBlockSection());
    assertEquals(response.getId(), children.get(0).getParentTaskId());
}
```

- [ ] **Step 3: 添加 block task 查询测试**

```java
@Test
void getTasksByRole_withBlockTask_returnsBlockWithChildren() {
    // ... create role, block task with children ...
    List<TaskResponse> result = taskService.getTasksByRole(role.getId(), userId);
    TaskResponse block = result.stream()
        .filter(t -> "block".equals(t.getModule()))
        .findFirst()
        .orElseThrow();
    assertEquals(2, block.getChildren().size());
}
```

- [ ] **Step 4: 添加 block task 删除级联测试**

```java
@Test
void deleteTask_blockTask_deletesChildren() {
    // ... create role, block task with 2 children ...
    Long blockId = blockTask.getId();
    taskService.deleteTask(blockId, userId);
    List<Task> children = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(blockId);
    assertTrue(children.isEmpty());
    assertTrue(taskRepository.findById(blockId).isEmpty());
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend && mvn test -Dtest=TaskServiceTest
```

- [ ] **Step 6: 提交**

```bash
git add backend/src/test/java/com/ansible/role/service/TaskServiceTest.java
git commit -m "test(role): add TaskService block children tests"
```

---

## 前端任务

### Task 8: 扩展前端 Task 类型

**Files:**
- Modify: `frontend/src/types/entity/Task.ts`

- [ ] **Step 1: 添加 BlockSection 类型和 Task 接口字段**

在文件顶部添加：

```typescript
export type BlockSection = 'BLOCK' | 'RESCUE' | 'ALWAYS';
```

在 `Task` 接口中添加三个字段：

```typescript
parentTaskId: number | null;
blockSection: BlockSection | null;
children: Task[];
```

在 `CreateTaskRequest` 和 `UpdateTaskRequest` 中添加：

```typescript
blockChildren?: BlockChildRequest[];
```

新增 `BlockChildRequest` 接口：

```typescript
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
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/types/entity/Task.ts
git commit -m "feat(frontend): add BlockSection type and Task.children field"
```

---

### Task 9: 新建 BlockTasksEditor 组件

**Files:**
- Create: `frontend/src/components/role/BlockTasksEditor.tsx`

这是最复杂的前端组件。使用 Ant Design Tabs 实现 block/rescue/always 三个标签页，每个标签页内是一个可折叠的子任务卡片列表。

- [ ] **Step 1: 创建 BlockTasksEditor.tsx**

```tsx
import { useState } from 'react';
import { Button, Card, Collapse, Form, Input, Select, Switch, Tabs, Space, InputNumber, Popconfirm, message } from 'antd';
import { PlusOutlined, DeleteOutlined, UpOutlined, DownOutlined } from '@ant-design/icons';
import type { BlockChildRequest, BlockSection } from '../../types/entity/Task';
import { getModuleDefinition } from '../../constants/ansibleModules';
import ModuleSelect from './ModuleSelect';
import { ModuleParamsGrid, ExtraParamsInput } from './ModuleParamsForm';

interface BlockChildFormData {
  key: string;
  name: string;
  module: string;
  moduleParams: Record<string, unknown>;
  extraParams: { key: string; value: string }[];
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
}

function buildArgsJson(
  moduleParams: Record<string, unknown> | undefined,
  extraParams: { key: string; value: string }[] | undefined,
): string {
  const result: Record<string, unknown> = {};
  if (moduleParams) {
    for (const [k, v] of Object.entries(moduleParams)) {
      if (v !== undefined && v !== '') {
        result[k] = v;
      }
    }
  }
  if (extraParams) {
    for (const item of extraParams) {
      if (item.key) {
        result[item.key] = item.value;
      }
    }
  }
  return Object.keys(result).length > 0 ? JSON.stringify(result) : '';
}

function ChildTaskCard({
  data,
  index,
  total,
  onChange,
  onDelete,
  onMoveUp,
  onMoveDown,
}: {
  data: BlockChildFormData;
  index: number;
  total: number;
  onChange: (data: BlockChildFormData) => void;
  onDelete: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}) {
  const { moduleParams, extraParams, ...rest } = data;

  return (
    <Card
      size="small"
      style={{ marginBottom: 8 }}
      title={
        <Space>
          <span>{index + 1}. {data.name || '(未命名子任务)'}</span>
          {data.module && <span style={{ color: '#999' }}>({data.module})</span>}
        </Space>
      }
      extra={
        <Space>
          <Button type="text" size="small" icon={<UpOutlined />} disabled={index === 0} onClick={onMoveUp} />
          <Button type="text" size="small" icon={<DownOutlined />} disabled={index === total - 1} onClick={onMoveDown} />
          <Popconfirm title="确定删除?" onConfirm={onDelete}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      }
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
        <Form.Item label="名称" required style={{ marginBottom: 12 }}>
          <Input
            value={data.name}
            onChange={(e) => onChange({ ...data, name: e.target.value })}
            placeholder="子任务名称"
          />
        </Form.Item>
        <Form.Item label="模块" required style={{ marginBottom: 12 }}>
          <ModuleSelect
            value={data.module}
            onChange={(val) => onChange({ ...data, module: val, moduleParams: {}, extraParams: [] })}
            filterModule="block"
          />
        </Form.Item>
      </div>
      {data.module && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
          <ModuleParamsGrid moduleName={data.module} moduleParams={data.moduleParams} onChange={(mp) => onChange({ ...data, moduleParams: mp })} />
        </div>
      )}
      <ExtraParamsInput
        extraParams={data.extraParams || []}
        onChange={(ep) => onChange({ ...data, extraParams: ep })}
      />
      <Collapse
        ghost
        bordered={false}
        items={[{
          key: 'advanced',
          label: '高级选项',
          children: (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
              <Form.Item label="When" style={{ marginBottom: 8 }}>
                <Input value={data.whenCondition} onChange={(e) => onChange({ ...data, whenCondition: e.target.value })} placeholder="条件表达式" />
              </Form.Item>
              <Form.Item label="Loop" style={{ marginBottom: 8 }}>
                <Input value={data.loop} onChange={(e) => onChange({ ...data, loop: e.target.value })} placeholder="{{ items }}" />
              </Form.Item>
              <Form.Item label="Register" style={{ marginBottom: 8 }}>
                <Input value={data.register} onChange={(e) => onChange({ ...data, register: e.target.value })} placeholder="变量名" />
              </Form.Item>
              <Form.Item label="Become" style={{ marginBottom: 8 }}>
                <Switch checked={data.become} onChange={(v) => onChange({ ...data, become: v })} />
              </Form.Item>
              <Form.Item label="Become User" style={{ marginBottom: 8 }}>
                <Input value={data.becomeUser} onChange={(e) => onChange({ ...data, becomeUser: e.target.value })} placeholder="root" />
              </Form.Item>
              <Form.Item label="Ignore Errors" style={{ marginBottom: 8 }}>
                <Switch checked={data.ignoreErrors} onChange={(v) => onChange({ ...data, ignoreErrors: v })} />
              </Form.Item>
            </div>
          ),
        }]}
      />
    </Card>
  );
}

function generateKey() {
  return Math.random().toString(36).slice(2);
}

export default function BlockTasksEditor({
  blockChildren,
  onChange,
}: {
  blockChildren: BlockChildRequest[];
  onChange: (children: BlockChildRequest[]) => void;
}) {
  // Internal state: array of editable child forms keyed by string id
  const [childForms, setChildForms] = useState<BlockChildFormData[]>(() =>
    blockChildren.length > 0
      ? blockChildren.map((c) => ({
          key: generateKey(),
          name: c.name,
          module: c.module,
          moduleParams: {},
          extraParams: [],
          whenCondition: c.whenCondition || '',
          loop: c.loop || '',
          register: c.register || '',
          become: c.become || false,
          becomeUser: c.becomeUser || '',
          ignoreErrors: c.ignoreErrors || false,
        }))
      : []
  );

  const sectionItems = (section: BlockSection) => {
    const sectionChildren = childForms.filter((_, i) => {
      // We store section in a separate map; for simplicity use a data-attr approach
      return true; // filtered below
    });
    return childForms
      .map((f, i) => ({ form: f, originalIndex: i }))
      .filter(({ form }) => (form as unknown as { _section: BlockSection })._section === section)
      .map(({ form, originalIndex }) => originalIndex);
  };

  // We'll store section per form using a map
  const [sectionMap, setSectionMap] = useState<Record<string, BlockSection>>(() => {
    const map: Record<string, BlockSection> = {};
    blockChildren.forEach((c, i) => {
      const key = childForms[i]?.key || generateKey();
      map[key] = c.section;
    });
    return map;
  });

  const getFormsBySection = (section: BlockSection) =>
    childForms.filter((f) => sectionMap[f.key] === section);

  const handleAddChild = (section: BlockSection) => {
    const newForm: BlockChildFormData = {
      key: generateKey(),
      name: '',
      module: '',
      moduleParams: {},
      extraParams: [],
      whenCondition: '',
      loop: '',
      register: '',
      become: false,
      becomeUser: '',
      ignoreErrors: false,
    };
    setChildForms([...childForms, newForm]);
    setSectionMap({ ...sectionMap, [newForm.key]: section });
  };

  const handleUpdateChild = (key: string, updated: BlockChildFormData) => {
    setChildForms(childForms.map((f) => (f.key === key ? updated : f)));
  };

  const handleDeleteChild = (key: string) => {
    setChildForms(childForms.filter((f) => f.key !== key));
    const newMap = { ...sectionMap };
    delete newMap[key];
    setSectionMap(newMap);
  };

  const handleMoveUp = (section: BlockSection, index: number) => {
    const forms = getFormsBySection(section);
    if (index <= 0) return;
    const otherForms = childForms.filter((f) => sectionMap[f.key] !== section);
    const sectionForms = getFormsBySection(section);
    const reordered = [...sectionForms];
    [sectionForms[index - 1], sectionForms[index]] = [sectionForms[index], sectionForms[index - 1]];
    setChildForms([...otherForms, ...sectionForms]);
  };

  const handleMoveDown = (section: BlockSection, index: number) => {
    const forms = getFormsBySection(section);
    if (index >= forms.length - 1) return;
    const otherForms = childForms.filter((f) => sectionMap[f.key] !== section);
    const sectionForms = getFormsBySection(section);
    const reordered = [...sectionForms];
    [sectionForms[index], sectionForms[index + 1]] = [sectionForms[index + 1], sectionForms[index]];
    setChildForms([...otherForms, ...sectionForms]);
  };

  const syncToParent = () => {
    const result: BlockChildRequest[] = childForms.map((f, i) => ({
      section: sectionMap[f.key] || 'BLOCK',
      name: f.name,
      module: f.module,
      args: buildArgsJson(f.moduleParams, f.extraParams),
      whenCondition: f.whenCondition || undefined,
      loop: f.loop || undefined,
      register: f.register || undefined,
      become: f.become || undefined,
      becomeUser: f.becomeUser || undefined,
      ignoreErrors: f.ignoreErrors || undefined,
      taskOrder: i,
    }));
    onChange(result);
  };

  // Sync whenever childForms changes
  useState(() => { syncToParent(); });

  const tabItems = [
    {
      key: 'BLOCK',
      label: 'block（必填）',
      children: (
        <div>
          {getFormsBySection('BLOCK').map((form, i) => (
            <ChildTaskCard
              key={form.key}
              data={{ ...form, ...{ _section: 'BLOCK' } as unknown as BlockChildFormData }}
              index={i}
              total={getFormsBySection('BLOCK').length}
              onChange={(d) => handleUpdateChild(form.key, d)}
              onDelete={() => handleDeleteChild(form.key)}
              onMoveUp={() => handleMoveUp('BLOCK', i)}
              onMoveDown={() => handleMoveDown('BLOCK', i)}
            />
          ))}
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleAddChild('BLOCK')} style={{ width: '100%' }}>
            添加 block 子任务
          </Button>
        </div>
      ),
    },
    {
      key: 'RESCUE',
      label: 'rescue（可选）',
      children: (
        <div>
          {getFormsBySection('RESCUE').length === 0 && (
            <div style={{ color: '#999', textAlign: 'center', padding: 16 }}>暂无 rescue 任务</div>
          )}
          {getFormsBySection('RESCUE').map((form, i) => (
            <ChildTaskCard
              key={form.key}
              data={form}
              index={i}
              total={getFormsBySection('RESCUE').length}
              onChange={(d) => handleUpdateChild(form.key, d)}
              onDelete={() => handleDeleteChild(form.key)}
              onMoveUp={() => handleMoveUp('RESCUE', i)}
              onMoveDown={() => handleMoveDown('RESCUE', i)}
            />
          ))}
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleAddChild('RESCUE')} style={{ width: '100%' }}>
            添加 rescue 子任务
          </Button>
        </div>
      ),
    },
    {
      key: 'ALWAYS',
      label: 'always（可选）',
      children: (
        <div>
          {getFormsBySection('ALWAYS').length === 0 && (
            <div style={{ color: '#999', textAlign: 'center', padding: 16 }}>暂无 always 任务</div>
          )}
          {getFormsBySection('ALWAYS').map((form, i) => (
            <ChildTaskCard
              key={form.key}
              data={form}
              index={i}
              total={getFormsBySection('ALWAYS').length}
              onChange={(d) => handleUpdateChild(form.key, d)}
              onDelete={() => handleDeleteChild(form.key)}
              onMoveUp={() => handleMoveUp('ALWAYS', i)}
              onMoveDown={() => handleMoveDown('ALWAYS', i)}
            />
          ))}
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleAddChild('ALWAYS')} style={{ width: '100%' }}>
            添加 always 子任务
          </Button>
        </div>
      ),
    },
  ];

  return <Tabs items={tabItems} />;
}
```

> **注意**：以上代码中的 `useState` hook 在 `syncToParent` 中的使用方式有误（它不应该在 useState 初始化器里调用），应该用 `useEffect` 替代。这会在 Step 1 完成后修正。

- [ ] **Step 1（修正）: 用 useEffect 替代 useState 初始化器中的 syncToParent 调用**

将 `useState(() => { syncToParent(); });` 替换为：

```tsx
import { useEffect } from 'react';
// ...
useEffect(() => {
  syncToParent();
}, [childForms, sectionMap]);
```

- [ ] **Step 2: 检查 ModuleSelect 是否支持 filterModule prop**

查看 `frontend/src/components/role/ModuleSelect.tsx`，如果现有组件没有 `filterModule` prop，需要先在 ModuleSelect 中添加该功能。

- [ ] **Step 3: 检查 ModuleParamsGrid 是否支持外部 moduleParams + onChange props**

查看 `frontend/src/components/role/ModuleParamsForm.tsx`，如果现有 `ModuleParamsGrid` 不支持受控模式（moduleParams value + onChange callback），需要新增一个支持受控模式的版本或修改现有组件。

- [ ] **Step 4: 提交（草稿）**

```bash
git add frontend/src/components/role/BlockTasksEditor.tsx
git commit -m "feat(frontend): add BlockTasksEditor component"
```

---

### Task 10: ModuleSelect 支持 filterModule 过滤（如果需要）

**Files:**
- Modify: `frontend/src/components/role/ModuleSelect.tsx`

- [ ] **Step 1: 在 ModuleSelect props 中新增 filterModule?: string**

如果 `filterModule` 传入 "block"，则在选项列表中过滤掉 name === "block" 的项。

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/role/ModuleSelect.tsx
git commit -m "feat(frontend): add filterModule prop to ModuleSelect"
```

---

### Task 11: RoleTasks 集成 block 模式

**Files:**
- Modify: `frontend/src/pages/role/RoleTasks.tsx`

核心改动：在 Modal 中检测 `selectedModule === 'block'`，动态切换表单内容。

- [ ] **Step 1: 导入 BlockTasksEditor**

```tsx
import BlockTasksEditor from '../../components/role/BlockTasksEditor';
```

- [ ] **Step 2: 在 RoleTasks 中新增 state 管理 blockChildren**

```tsx
const [blockChildren, setBlockChildren] = useState<BlockChildRequest[]>([]);
```

- [ ] **Step 3: 修改 handleCreate — 重置 blockChildren**

```tsx
setBlockChildren([]);
```

- [ ] **Step 4: 修改 handleEdit — 如果是 block 类型则加载 children**

在 `form.setFieldsValue(...)` 后添加：

```tsx
if (task.module === 'block' && task.children && task.children.length > 0) {
  setBlockChildren(task.children.map((c) => ({
    section: c.blockSection || 'BLOCK',
    name: c.name,
    module: c.module,
    args: c.args,
    whenCondition: c.whenCondition,
    loop: c.loop,
    until: c.until,
    register: c.register,
    notify: c.notify,
    taskOrder: c.taskOrder,
    become: c.become,
    becomeUser: c.becomeUser,
    ignoreErrors: c.ignoreErrors,
  })));
} else {
  setBlockChildren([]);
}
```

- [ ] **Step 5: 修改 Modal 内容 — 根据 selectedModule 条件渲染**

在 `<Form form={form} layout="vertical">` 的内容中，将 `ModuleParamsGrid` 和 `ExtraParamsInput` 部分包裹条件判断：

```tsx
{selectedModule === 'block' ? (
  <>
    <BlockTasksEditor
      blockChildren={blockChildren}
      onChange={(children) => {
        setBlockChildren(children);
        // 同时更新 form 中一个虚拟字段用于触发表单变化
        form.setFieldsValue({ _blockChildren: children });
      }}
    />
    <input type="hidden" id="block-children-sync" />
  </>
) : (
  <>
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
      <ModuleParamsGrid moduleName={selectedModule} />
    </div>
    <ExtraParamsInput />
  </>
)}
```

- [ ] **Step 6: 修改高级选项折叠 — 根据 selectedModule 显示不同选项**

当 `selectedModule === 'block'` 时，高级选项中只显示 when、become、becomeUser、ignoreErrors，隐藏 loop、until、register、notify。

当 `selectedModule !== 'block'` 时，高级选项和现在完全一致。

- [ ] **Step 7: 修改 handleSubmit — 构建 blockChildren 到请求数据**

在构建 `CreateTaskRequest` / `UpdateTaskRequest` 时：

```tsx
if (values.module === 'block') {
  createData = {
    ...createData,
    blockChildren,
  };
  // block 级别忽略 args, loop, until, register, notify
  delete createData.args;
  delete createData.loop;
  delete createData.until;
  delete createData.register;
  delete createData.notify;
}
```

- [ ] **Step 8: 修改 handlePreviewForm — 支持 block 预览**

当 `selectedModule === 'block'` 时，需要在内存中构建一个含 children 的 task 对象，然后调用 `blockToYaml` 序列化，而不是普通 `taskToYaml`。

- [ ] **Step 9: 提交**

```bash
git add frontend/src/pages/role/RoleTasks.tsx
git commit -m "feat(frontend): integrate BlockTasksEditor in RoleTasks form"
```

---

### Task 12: taskToYaml 支持 block 递归渲染

**Files:**
- Modify: `frontend/src/utils/taskToYaml.ts`

- [ ] **Step 1: 新增 blockToYaml 辅助函数**

在 `taskToYaml` 函数之前添加：

```typescript
export function blockToYaml(task: TaskYamlInput & { children?: TaskYamlInput[] }): string {
  const lines: string[] = [];

  lines.push(`- name: ${yamlScalar(task.name || 'Unnamed task')}`);
  lines.push(`  block:`);

  const blockChildren = (task.children || []).filter((c) => !c.blockSection || c.blockSection === 'BLOCK');
  for (const child of blockChildren) {
    const childYaml = taskToYaml(child);
    const indented = childYaml.split('\n').map((l) => '    ' + l).join('\n');
    lines.push(indented);
  }

  const rescueChildren = (task.children || []).filter((c) => c.blockSection === 'RESCUE');
  if (rescueChildren.length > 0) {
    lines.push(`  rescue:`);
    for (const child of rescueChildren) {
      const childYaml = taskToYaml(child);
      const indented = childYaml.split('\n').map((l) => '    ' + l).join('\n');
      lines.push(indented);
    }
  }

  const alwaysChildren = (task.children || []).filter((c) => c.blockSection === 'ALWAYS');
  if (alwaysChildren.length > 0) {
    lines.push(`  always:`);
    for (const child of alwaysChildren) {
      const childYaml = taskToYaml(child);
      const indented = childYaml.split('\n').map((l) => '    ' + l).join('\n');
      lines.push(indented);
    }
  }

  if (task.whenCondition) {
    lines.push(`  when: ${yamlScalar(task.whenCondition)}`);
  }
  if (task.become) {
    lines.push('  become: true');
  }
  if (task.becomeUser) {
    lines.push(`  become_user: ${yamlScalar(task.becomeUser)}`);
  }
  if (task.ignoreErrors) {
    lines.push('  ignore_errors: true');
  }

  return lines.join('\n');
}
```

修改 `TaskYamlInput` 接口，增加可选字段：

```typescript
export interface TaskYamlInput {
  // ... existing fields
  blockSection?: string;
  parentTaskId?: number | null;
  children?: TaskYamlInput[];
}
```

- [ ] **Step 2: 修改 handlePreviewAll — 构建任务树**

在 `RoleTasks.tsx` 的 `handlePreviewAll` 中，`tasks` 数组中如果存在 block 类型的 task，其 `children` 已经在后端返回。直接对每个 task 判断：

```tsx
const yaml = tasks.map((t) => {
  if (t.module === 'block' && t.children && t.children.length > 0) {
    return blockToYaml(t);
  }
  return taskToYaml(t);
}).join('\n\n');
```

- [ ] **Step 3: 运行 lint 检查**

```bash
cd frontend && npm run lint
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/utils/taskToYaml.ts frontend/src/pages/role/RoleTasks.tsx
git commit -m "feat(frontend): add blockToYaml recursive rendering"
```

---

### Task 13: 前端集成测试

**Files:**
- Check: `frontend/src/components/role/__tests__/` 或 `frontend/src/utils/__tests__/`

- [ ] **Step 1: 为 taskToYaml 添加 block 测试用例**

```typescript
import { blockToYaml, taskToYaml } from '../taskToYaml';

describe('blockToYaml', () => {
  it('renders block with children', () => {
    const blockTask = {
      name: 'SSH block',
      module: 'block',
      whenCondition: "{{ ssh_enabled }}",
      children: [
        {
          name: 'Get SSH key',
          module: 'command',
          args: '{"_raw_params": "ssh-keyscan -H {{ item }}"}',
          register: 'ssh_keyscan_result',
          loop: "{{ groups['all'] }}",
          blockSection: 'BLOCK',
        },
      ],
    };
    const yaml = blockToYaml(blockTask);
    expect(yaml).toContain('- name: SSH block');
    expect(yaml).toContain('  block:');
    expect(yaml).toContain('    - name: Get SSH key');
    expect(yaml).toContain('      command: ssh-keyscan -H {{ item }}');
    expect(yaml).toContain('      register: ssh_keyscan_result');
    expect(yaml).toContain('      loop: "{{ groups[\'all\'] }}"');
    expect(yaml).toContain('  when: "{{ ssh_enabled }}"');
  });

  it('renders rescue and always sections', () => {
    const blockTask = {
      name: 'Block with rescue',
      module: 'block',
      children: [
        { name: 'Main', module: 'shell', args: '{}', blockSection: 'BLOCK', taskOrder: 1 },
        { name: 'Recovery', module: 'debug', args: '{"msg": "failed"}', blockSection: 'RESCUE', taskOrder: 1 },
        { name: 'Cleanup', module: 'file', args: '{"path": "/tmp/x", "state": "absent"}', blockSection: 'ALWAYS', taskOrder: 1 },
      ],
    };
    const yaml = blockToYaml(blockTask);
    expect(yaml).toContain('  block:');
    expect(yaml).toContain('    - name: Main');
    expect(yaml).toContain('  rescue:');
    expect(yaml).toContain('    - name: Recovery');
    expect(yaml).toContain('  always:');
    expect(yaml).toContain('    - name: Cleanup');
  });
});
```

- [ ] **Step 2: 运行测试**

```bash
cd frontend && npm run test -- --run taskToYaml
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/utils/__tests__/taskToYaml.test.ts
git commit -m "test(frontend): add blockToYaml unit tests"
```

---

## 自检清单

完成实现后，对照 spec 检查：

- [ ] Task 实体有 `parentTaskId` 和 `blockSection` 字段
- [ ] `CreateTaskRequest` / `UpdateTaskRequest` 有 `blockChildren` 字段
- [ ] `TaskResponse` 有 `parentTaskId`、`blockSection`、`children` 字段
- [ ] 创建 block task 时子任务同步保存
- [ ] 更新 block task 时子任务全量替换
- [ ] 删除 block task 时子任务级联删除
- [ ] 查询返回顶层任务，block 类型附加 children
- [ ] 前端 Task 类型包含新字段
- [ ] BlockTasksEditor 组件支持 block/rescue/always 三段
- [ ] 子任务模块选择器过滤了 block 选项
- [ ] YAML 预览正确渲染 block/rescue/always 结构
- [ ] 所有测试通过

---

## 执行方式

**Plan complete.** 两个执行选项：

**1. Subagent-Driven (recommended)** — 每个任务派发独立子 agent，任务间有检查点回顾

**2. Inline Execution** — 在当前 session 中按计划批量执行，设定检查点

选择哪个？