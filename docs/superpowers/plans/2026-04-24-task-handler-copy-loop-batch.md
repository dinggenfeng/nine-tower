# Task/Handler 复制 + Loop 批量添加 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Task 和 Handler 管理添加复制功能，为 Loop 列表模式添加批量输入入口

**Architecture:** 纯前端改动，复用现有 create 接口。复制 = 打开预填数据的创建弹窗。批量添加 = 在现有 Select tags 旁增加按钮，弹出 Modal 输入多行文本。

**Tech Stack:** React 18, Ant Design 5, TypeScript

---

### Task 1: Task 复制功能（RoleTasks.tsx）

**Files:**
- Modify: `frontend/src/pages/role/RoleTasks.tsx`

- [ ] **Step 1: 添加 handleCopy 函数**

在 `handleEdit` 函数（约 line 104）之后，添加 `handleCopy` 函数。逻辑与 `handleEdit` 相同，但：
- `setEditingTask(null)` 而不是 `setEditingTask(task)`
- 名称加 ` (副本)` 后缀
- `taskOrder` 设为 `task.taskOrder + 1`

```typescript
const handleCopy = async (task: Task) => {
  setEditingTask(null);
  const { moduleParams, extraParams } = parseArgsToForm(task.args, task.module);
  setSelectedModule(task.module);
  setBlockChildren([]);
  setTagIds([]);
  form.resetFields();
  form.setFieldsValue({
    name: task.name ? `${task.name} (副本)` : "",
    module: task.module,
    moduleParams,
    extraParams: extraParams.length > 0 ? extraParams : undefined,
    whenCondition: task.whenCondition,
    loop: task.loop,
    until: task.until,
    register: task.register,
    notify: task.notify,
    taskOrder: task.taskOrder + 1,
    become: task.become || false,
    becomeUser: task.becomeUser,
    ignoreErrors: task.ignoreErrors || false,
  });
  if (task.loop) {
    try {
      const parsed = JSON.parse(task.loop);
      if (Array.isArray(parsed)) {
        setLoopMode("list");
        setLoopItems(parsed.map(String));
      } else {
        setLoopMode("expression");
        setLoopItems([]);
      }
    } catch {
      setLoopMode("expression");
      setLoopItems([]);
    }
  } else {
    setLoopMode("expression");
    setLoopItems([]);
  }
  setModalOpen(true);
};
```

- [ ] **Step 2: 在操作列添加复制按钮**

在 columns 定义的操作列 render 函数中（约 line 427-459），在预览按钮（`EyeOutlined`）之后、编辑按钮之前添加复制按钮。将操作列 width 从 200 改为 240。

```tsx
<Button
  type="text"
  size="small"
  icon={<CopyOutlined />}
  onClick={() => handleCopy(record)}
/>
```

- [ ] **Step 3: 提交**

```bash
cd frontend && npm run build
```

确认构建通过后提交：

```bash
git add frontend/src/pages/role/RoleTasks.tsx
git commit -m "feat: add copy task functionality"
```

---

### Task 2: Handler 复制功能（RoleHandlers.tsx）

**Files:**
- Modify: `frontend/src/pages/role/RoleHandlers.tsx`

- [ ] **Step 1: 添加 handleCopy 函数**

在 `handleEdit` 函数（约 line 81）之后，添加 `handleCopy` 函数：

```typescript
const handleCopy = (handler: Handler) => {
  setEditingHandler(null);
  const { moduleParams, extraParams } = parseArgsToForm(handler.args, handler.module);
  setSelectedModule(handler.module);
  form.resetFields();
  form.setFieldsValue({
    name: handler.name ? `${handler.name} (副本)` : "",
    module: handler.module,
    moduleParams,
    extraParams: extraParams.length > 0 ? extraParams : undefined,
    whenCondition: handler.whenCondition,
    register: handler.register,
    become: handler.become || false,
    becomeUser: handler.becomeUser,
    ignoreErrors: handler.ignoreErrors || false,
  });
  setModalOpen(true);
};
```

- [ ] **Step 2: 在操作列添加复制按钮**

在 columns 定义的操作列 render 函数中（约 line 259-278），在预览按钮之后、编辑按钮之前添加复制按钮。将操作列 width 从 150 改为 200。

```tsx
<Button
  type="text"
  size="small"
  icon={<CopyOutlined />}
  onClick={() => handleCopy(record)}
/>
```

- [ ] **Step 3: 提交**

```bash
cd frontend && npm run build
```

确认构建通过后提交：

```bash
git add frontend/src/pages/role/RoleHandlers.tsx
git commit -m "feat: add copy handler functionality"
```

---

### Task 3: Loop 批量添加 — RoleTasks.tsx 主任务表单

**Files:**
- Modify: `frontend/src/pages/role/RoleTasks.tsx`

- [ ] **Step 1: 添加批量添加 Modal 的 state**

在现有 state 声明区域（约 line 72 之后），添加：

```typescript
const [batchLoopOpen, setBatchLoopOpen] = useState(false);
const [batchLoopText, setBatchLoopText] = useState("");
```

- [ ] **Step 2: 在 Loop 列表模式的 Select 下方添加"批量添加"按钮和 Modal**

在 Loop 的 `<Select mode="tags">` 后面（约 line 689），添加按钮：

```tsx
<Button
  type="link"
  size="small"
  onClick={() => { setBatchLoopText(""); setBatchLoopOpen(true); }}
  style={{ padding: 0 }}
>
  批量添加
</Button>
```

Modal 放在组件 return 的最后（YAML 预览 Modal 之后）：

```tsx
<Modal
  title="批量添加列表项"
  open={batchLoopOpen}
  onCancel={() => setBatchLoopOpen(false)}
  onOk={() => {
    const newItems = batchLoopText
      .split("\n")
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    const merged = [...new Set([...loopItems, ...newItems])];
    setLoopItems(merged);
    form.setFieldValue("loop", merged.length > 0 ? JSON.stringify(merged) : "");
    setBatchLoopOpen(false);
  }}
  width={480}
  okText="确定"
  cancelText="取消"
>
  <Input.TextArea
    rows={8}
    value={batchLoopText}
    onChange={(e) => setBatchLoopText(e.target.value)}
    placeholder="每行一个列表项，空行将被忽略&#10;例如:&#10;nginx&#10;git&#10;curl"
  />
</Modal>
```

- [ ] **Step 3: 提交**

```bash
cd frontend && npm run build
```

确认构建通过后提交：

```bash
git add frontend/src/pages/role/RoleTasks.tsx
git commit -m "feat: add batch input for loop list items in task form"
```

---

### Task 4: Loop 批量添加 — BlockTasksEditor.tsx 子任务表单

**Files:**
- Modify: `frontend/src/components/role/BlockTasksEditor.tsx`

- [ ] **Step 1: 在 ChildTaskCard 中添加批量添加 Modal state 和 UI**

在 `ChildTaskCard` 函数组件内添加 state：

```typescript
const [batchLoopOpen, setBatchLoopOpen] = useState(false);
const [batchLoopText, setBatchLoopText] = useState("");
```

在 Loop 列表模式的 `<Select mode="tags">` 后面（约 line 308），添加按钮：

```tsx
<Button
  type="link"
  size="small"
  onClick={() => { setBatchLoopText(""); setBatchLoopOpen(true); }}
  style={{ padding: 0 }}
>
  批量添加
</Button>
```

在 `ChildTaskCard` 的 return JSX 最后（`</Card>` 之前），添加 Modal：

```tsx
<Modal
  title="批量添加列表项"
  open={batchLoopOpen}
  onCancel={() => setBatchLoopOpen(false)}
  onOk={() => {
    const newItems = batchLoopText
      .split("\n")
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    const merged = [...new Set([...data.loopItems, ...newItems])];
    onChange({
      ...data,
      loopItems: merged,
      loop: merged.length > 0 ? JSON.stringify(merged) : "",
    });
    setBatchLoopOpen(false);
  }}
  width={480}
  okText="确定"
  cancelText="取消"
>
  <Input.TextArea
    rows={8}
    value={batchLoopText}
    onChange={(e) => setBatchLoopText(e.target.value)}
    placeholder="每行一个列表项，空行将被忽略&#10;例如:&#10;nginx&#10;git&#10;curl"
  />
</Modal>
```

需要确保在 ChildTaskCard 顶部 import 中添加 `Modal` 和 `Input`。当前 import 中已有 `Input` 但没有 `Modal`，需要在 antd import 行（line 3-15）中加入 `Modal`。

- [ ] **Step 2: 提交**

```bash
cd frontend && npm run build
```

确认构建通过后提交：

```bash
git add frontend/src/components/role/BlockTasksEditor.tsx
git commit -m "feat: add batch input for loop list items in block child task form"
```
