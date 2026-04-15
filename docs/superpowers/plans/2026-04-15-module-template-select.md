# Module Template Select & Dynamic Params Form Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the plain text `module` input and `args` textarea in Task/Handler forms with a module dropdown selector (with descriptions and doc links) and a dynamic parameter form that adapts to the selected module.

**Architecture:** Pure frontend change. Module metadata (name, description, doc URL, parameter definitions) is maintained as TypeScript constants. Two new shared components (`ModuleSelect`, `ModuleParamsForm`) are integrated into existing `RoleTasks` and `RoleHandlers` pages. Backend unchanged — `module` stores string, `args` stores JSON.

**Tech Stack:** React 18, Ant Design 5, TypeScript

---

### File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `frontend/src/constants/ansibleModules.ts` | Create | Module metadata: types, definitions for copy/template/file |
| `frontend/src/components/role/ModuleSelect.tsx` | Create | Dropdown selector with descriptions and doc link on hover |
| `frontend/src/components/role/ModuleParamsForm.tsx` | Create | Dynamic param fields + extra key-value params |
| `frontend/src/pages/role/RoleTasks.tsx` | Modify | Integrate new components, update submit/edit logic |
| `frontend/src/pages/role/RoleHandlers.tsx` | Modify | Same integration as RoleTasks |

---

### Task 1: Module Metadata Constants

**Files:**
- Create: `frontend/src/constants/ansibleModules.ts`

- [ ] **Step 1: Create the module metadata file with types and definitions**

Create `frontend/src/constants/ansibleModules.ts`:

```typescript
export interface ModuleParamOption {
  label: string;
  value: string;
}

export interface ModuleParam {
  name: string;
  label: string;
  type: 'input' | 'select' | 'switch';
  required: boolean;
  placeholder?: string;
  tooltip?: string;
  options?: ModuleParamOption[];
  defaultValue?: string | boolean;
}

export interface ModuleDefinition {
  name: string;
  label: string;
  description: string;
  docUrl: string;
  params: ModuleParam[];
  validate?: (values: Record<string, unknown>) => Record<string, string>;
}

export const ANSIBLE_MODULES: ModuleDefinition[] = [
  {
    name: 'copy',
    label: 'copy',
    description: '复制文件到远程主机',
    docUrl:
      'https://docs.ansible.com/ansible/latest/collections/ansible/builtin/copy_module.html',
    params: [
      {
        name: 'src',
        label: '源文件路径 (src)',
        type: 'input',
        required: false,
        placeholder: '/path/to/local/file',
        tooltip: '本地文件路径，与 content 二选一',
      },
      {
        name: 'content',
        label: '文件内容 (content)',
        type: 'input',
        required: false,
        placeholder: '直接写入远程文件的内容',
        tooltip: '直接指定文件内容，与 src 二选一',
      },
      {
        name: 'dest',
        label: '目标路径 (dest)',
        type: 'input',
        required: true,
        placeholder: '/path/to/remote/file',
        tooltip: '远程主机上的目标路径',
      },
      {
        name: 'owner',
        label: '所有者 (owner)',
        type: 'input',
        required: false,
        placeholder: 'root',
      },
      {
        name: 'group',
        label: '所属组 (group)',
        type: 'input',
        required: false,
        placeholder: 'root',
      },
      {
        name: 'mode',
        label: '权限 (mode)',
        type: 'input',
        required: false,
        placeholder: '0644',
      },
      {
        name: 'backup',
        label: '备份 (backup)',
        type: 'switch',
        required: false,
        defaultValue: false,
        tooltip: '覆盖前是否备份原文件',
      },
      {
        name: 'remote_src',
        label: '远程源 (remote_src)',
        type: 'switch',
        required: false,
        defaultValue: false,
        tooltip: 'src 是否为远程主机上的路径',
      },
    ],
    validate: (values: Record<string, unknown>) => {
      const errors: Record<string, string> = {};
      if (!values.src && !values.content) {
        errors.src = 'src 和 content 至少填写一个';
        errors.content = 'src 和 content 至少填写一个';
      }
      if (values.src && values.content) {
        errors.content = 'src 和 content 不能同时填写';
      }
      return errors;
    },
  },
  {
    name: 'template',
    label: 'template',
    description: '渲染 Jinja2 模板到远程主机',
    docUrl:
      'https://docs.ansible.com/ansible/latest/collections/ansible/builtin/template_module.html',
    params: [
      {
        name: 'src',
        label: '模板路径 (src)',
        type: 'input',
        required: true,
        placeholder: 'templates/nginx.conf.j2',
        tooltip: '本地 Jinja2 模板文件路径',
      },
      {
        name: 'dest',
        label: '目标路径 (dest)',
        type: 'input',
        required: true,
        placeholder: '/etc/nginx/nginx.conf',
        tooltip: '远程主机上的目标路径',
      },
      {
        name: 'owner',
        label: '所有者 (owner)',
        type: 'input',
        required: false,
        placeholder: 'root',
      },
      {
        name: 'group',
        label: '所属组 (group)',
        type: 'input',
        required: false,
        placeholder: 'root',
      },
      {
        name: 'mode',
        label: '权限 (mode)',
        type: 'input',
        required: false,
        placeholder: '0644',
      },
      {
        name: 'backup',
        label: '备份 (backup)',
        type: 'switch',
        required: false,
        defaultValue: false,
        tooltip: '覆盖前是否备份原文件',
      },
    ],
  },
  {
    name: 'file',
    label: 'file',
    description: '管理文件和目录属性',
    docUrl:
      'https://docs.ansible.com/ansible/latest/collections/ansible/builtin/file_module.html',
    params: [
      {
        name: 'path',
        label: '路径 (path)',
        type: 'input',
        required: true,
        placeholder: '/etc/myapp',
        tooltip: '要管理的文件或目录路径',
      },
      {
        name: 'state',
        label: '状态 (state)',
        type: 'select',
        required: false,
        tooltip: '目标状态类型',
        options: [
          { label: 'file — 普通文件', value: 'file' },
          { label: 'directory — 目录', value: 'directory' },
          { label: 'link — 符号链接', value: 'link' },
          { label: 'hard — 硬链接', value: 'hard' },
          { label: 'touch — 创建空文件', value: 'touch' },
          { label: 'absent — 删除', value: 'absent' },
        ],
      },
      {
        name: 'src',
        label: '链接源 (src)',
        type: 'input',
        required: false,
        placeholder: '/path/to/source',
        tooltip: 'state 为 link 或 hard 时，链接指向的源路径',
      },
      {
        name: 'owner',
        label: '所有者 (owner)',
        type: 'input',
        required: false,
        placeholder: 'root',
      },
      {
        name: 'group',
        label: '所属组 (group)',
        type: 'input',
        required: false,
        placeholder: 'root',
      },
      {
        name: 'mode',
        label: '权限 (mode)',
        type: 'input',
        required: false,
        placeholder: '0755',
      },
      {
        name: 'recurse',
        label: '递归 (recurse)',
        type: 'switch',
        required: false,
        defaultValue: false,
        tooltip: '递归设置目录属性',
      },
    ],
  },
];

export function getModuleDefinition(name: string): ModuleDefinition | undefined {
  return ANSIBLE_MODULES.find((m) => m.name === name);
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors related to the new file.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/constants/ansibleModules.ts
git commit -m "feat: add Ansible module metadata constants (copy, template, file)"
```

---

### Task 2: ModuleSelect Component

**Files:**
- Create: `frontend/src/components/role/ModuleSelect.tsx`

- [ ] **Step 1: Create the ModuleSelect component**

Create directory and file `frontend/src/components/role/ModuleSelect.tsx`:

```tsx
import { Select, Typography, Button } from 'antd';
import { LinkOutlined } from '@ant-design/icons';
import { ANSIBLE_MODULES } from '../../constants/ansibleModules';

const { Text } = Typography;

interface ModuleSelectProps {
  value?: string;
  onChange?: (value: string) => void;
}

export default function ModuleSelect({ value, onChange }: ModuleSelectProps) {
  return (
    <Select
      showSearch
      allowClear
      value={value}
      onChange={onChange}
      placeholder="选择或输入 Ansible 模块名"
      optionFilterProp="label"
      optionLabelProp="label"
    >
      {ANSIBLE_MODULES.map((mod) => (
        <Select.Option key={mod.name} value={mod.name} label={mod.label}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div>
              <Text strong>{mod.label}</Text>
              <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                {mod.description}
              </Text>
            </div>
            <Button
              type="link"
              size="small"
              icon={<LinkOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                window.open(mod.docUrl, '_blank');
              }}
              title="查看文档"
            />
          </div>
        </Select.Option>
      ))}
    </Select>
  );
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/role/ModuleSelect.tsx
git commit -m "feat: add ModuleSelect dropdown component with descriptions and doc links"
```

---

### Task 3: ModuleParamsForm Component

**Files:**
- Create: `frontend/src/components/role/ModuleParamsForm.tsx`

- [ ] **Step 1: Create the ModuleParamsForm component**

Create `frontend/src/components/role/ModuleParamsForm.tsx`:

```tsx
import { Form, Input, Select, Switch, Button, Space, Tooltip } from 'antd';
import { PlusOutlined, MinusCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import type { ModuleDefinition } from '../../constants/ansibleModules';
import { getModuleDefinition } from '../../constants/ansibleModules';

interface ModuleParamsFormProps {
  moduleName: string | undefined;
}

export default function ModuleParamsForm({ moduleName }: ModuleParamsFormProps) {
  const moduleDef: ModuleDefinition | undefined = moduleName
    ? getModuleDefinition(moduleName)
    : undefined;

  return (
    <>
      {moduleDef && (
        <>
          {moduleDef.params.map((param) => (
            <Form.Item
              key={param.name}
              name={['moduleParams', param.name]}
              label={
                <span>
                  {param.label}
                  {param.tooltip && (
                    <Tooltip title={param.tooltip}>
                      <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                    </Tooltip>
                  )}
                </span>
              }
              rules={
                param.required
                  ? [{ required: true, message: `请输入 ${param.label}` }]
                  : undefined
              }
              valuePropName={param.type === 'switch' ? 'checked' : 'value'}
              initialValue={param.defaultValue}
            >
              {param.type === 'input' && <Input placeholder={param.placeholder} />}
              {param.type === 'select' && (
                <Select
                  allowClear
                  placeholder={param.placeholder || '请选择'}
                  options={param.options}
                />
              )}
              {param.type === 'switch' && <Switch />}
            </Form.Item>
          ))}
        </>
      )}

      <Form.List name="extraParams">
        {(fields, { add, remove }) => (
          <>
            {fields.map(({ key, name, ...restField }) => (
              <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                <Form.Item
                  {...restField}
                  name={[name, 'key']}
                  rules={[{ required: true, message: '请输入参数名' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="参数名" style={{ width: 180 }} />
                </Form.Item>
                <Form.Item
                  {...restField}
                  name={[name, 'value']}
                  rules={[{ required: true, message: '请输入参数值' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="参数值" style={{ width: 280 }} />
                </Form.Item>
                <MinusCircleOutlined onClick={() => remove(name)} />
              </Space>
            ))}
            <Form.Item>
              <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                添加参数
              </Button>
            </Form.Item>
          </>
        )}
      </Form.List>
    </>
  );
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/role/ModuleParamsForm.tsx
git commit -m "feat: add ModuleParamsForm with dynamic fields and extra key-value params"
```

---

### Task 4: Integrate into RoleTasks

**Files:**
- Modify: `frontend/src/pages/role/RoleTasks.tsx`

- [ ] **Step 1: Add helper functions for args serialization/deserialization**

Add a utility at the top of `RoleTasks.tsx` (below imports) to convert between the form representation and the `args` JSON string. Also import the new components and `getModuleDefinition`.

Replace the imports section and add helpers:

```tsx
// Add to existing imports:
import ModuleSelect from '../../components/role/ModuleSelect';
import ModuleParamsForm from '../../components/role/ModuleParamsForm';
import { getModuleDefinition } from '../../constants/ansibleModules';

// Add helper functions after imports:

/** Merge moduleParams + extraParams into a JSON string for the args field */
function buildArgsJson(
  moduleParams: Record<string, unknown> | undefined,
  extraParams: { key: string; value: string }[] | undefined,
): string {
  const result: Record<string, unknown> = {};
  if (moduleParams) {
    for (const [k, v] of Object.entries(moduleParams)) {
      if (v !== undefined && v !== '' && v !== null) {
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

/** Parse args JSON string into moduleParams + extraParams for form population */
function parseArgsToForm(
  argsJson: string | undefined,
  moduleName: string | undefined,
): { moduleParams: Record<string, unknown>; extraParams: { key: string; value: string }[] } {
  const moduleParams: Record<string, unknown> = {};
  const extraParams: { key: string; value: string }[] = [];
  if (!argsJson) return { moduleParams, extraParams };

  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(argsJson);
  } catch {
    extraParams.push({ key: '', value: argsJson });
    return { moduleParams, extraParams };
  }

  const moduleDef = moduleName ? getModuleDefinition(moduleName) : undefined;
  const knownParams = new Set(moduleDef?.params.map((p) => p.name) ?? []);

  for (const [k, v] of Object.entries(parsed)) {
    if (knownParams.has(k)) {
      moduleParams[k] = v;
    } else {
      extraParams.push({ key: k, value: String(v) });
    }
  }
  return { moduleParams, extraParams };
}
```

- [ ] **Step 2: Add module state tracking and update handleCreate**

Add a state variable to track the selected module for dynamic rendering. Update `handleCreate` to clear it:

```tsx
// Add state after the existing useState calls:
const [selectedModule, setSelectedModule] = useState<string | undefined>(undefined);

// Replace handleCreate:
const handleCreate = () => {
  setEditingTask(null);
  setSelectedModule(undefined);
  form.resetFields();
  form.setFieldValue('taskOrder', tasks.length + 1);
  setModalOpen(true);
};
```

- [ ] **Step 3: Update handleEdit to parse args into form fields**

Replace the existing `handleEdit`:

```tsx
const handleEdit = (task: Task) => {
  setEditingTask(task);
  const { moduleParams, extraParams } = parseArgsToForm(task.args, task.module);
  setSelectedModule(task.module);
  form.setFieldsValue({
    name: task.name,
    module: task.module,
    moduleParams,
    extraParams: extraParams.length > 0 ? extraParams : undefined,
    whenCondition: task.whenCondition,
    loop: task.loop,
    until: task.until,
    register: task.register,
    notify: task.notify,
    taskOrder: task.taskOrder,
  });
  setModalOpen(true);
};
```

- [ ] **Step 4: Update handleSubmit to serialize args from form fields**

Replace the existing `handleSubmit`:

```tsx
const handleSubmit = async () => {
  const values = await form.validateFields();

  // Run module-level custom validation
  const moduleDef = values.module ? getModuleDefinition(values.module) : undefined;
  if (moduleDef?.validate) {
    const errors = moduleDef.validate(values.moduleParams || {});
    if (Object.keys(errors).length > 0) {
      // Set field errors on moduleParams fields
      const fieldErrors = Object.entries(errors).map(([field, msg]) => ({
        name: ['moduleParams', field],
        errors: [msg],
      }));
      form.setFields(fieldErrors);
      return;
    }
  }

  const args = buildArgsJson(values.moduleParams, values.extraParams);

  if (editingTask) {
    const data: UpdateTaskRequest = {
      name: values.name,
      module: values.module,
      args: args || undefined,
      whenCondition: values.whenCondition,
      loop: values.loop,
      until: values.until,
      register: values.register,
      notify: values.notify,
      taskOrder: values.taskOrder,
    };
    await updateTask(editingTask.id, data);
    message.success('已更新');
  } else {
    const data: CreateTaskRequest = {
      name: values.name,
      module: values.module,
      args: args || undefined,
      whenCondition: values.whenCondition,
      loop: values.loop,
      until: values.until,
      register: values.register,
      notify: values.notify,
      taskOrder: values.taskOrder,
    };
    await createTask(roleId, data);
    message.success('已创建');
  }
  setModalOpen(false);
  fetchData();
};
```

- [ ] **Step 5: Replace module Input and args TextArea in the form JSX**

In the Modal's Form, replace the `module` Form.Item and `args` Form.Item with:

```tsx
<Form.Item
  name="module"
  label="模块"
  rules={[{ required: true, message: '请选择 Ansible 模块' }]}
>
  <ModuleSelect
    onChange={(val) => {
      setSelectedModule(val);
      form.setFieldsValue({ moduleParams: undefined, extraParams: undefined });
    }}
  />
</Form.Item>
<ModuleParamsForm moduleName={selectedModule} />
```

Remove the old `args` Form.Item (the `<Form.Item name="args" label="参数 (JSON)">` block).

- [ ] **Step 6: Verify TypeScript compiles and lint passes**

Run: `cd frontend && npx tsc --noEmit && npm run lint`
Expected: No errors.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/role/RoleTasks.tsx
git commit -m "feat: integrate ModuleSelect and ModuleParamsForm into RoleTasks"
```

---

### Task 5: Integrate into RoleHandlers

**Files:**
- Modify: `frontend/src/pages/role/RoleHandlers.tsx`

- [ ] **Step 1: Add imports and helper functions**

Add the same imports and helper functions (`buildArgsJson`, `parseArgsToForm`) to `RoleHandlers.tsx`. The helper functions are identical to those in Task 4 Step 1.

```tsx
// Add to existing imports:
import ModuleSelect from '../../components/role/ModuleSelect';
import ModuleParamsForm from '../../components/role/ModuleParamsForm';
import { getModuleDefinition } from '../../constants/ansibleModules';
```

Copy the same `buildArgsJson` and `parseArgsToForm` functions from Task 4 Step 1.

- [ ] **Step 2: Add module state and update handleCreate**

```tsx
// Add state:
const [selectedModule, setSelectedModule] = useState<string | undefined>(undefined);

// Replace handleCreate:
const handleCreate = () => {
  setEditingHandler(null);
  setSelectedModule(undefined);
  form.resetFields();
  setModalOpen(true);
};
```

- [ ] **Step 3: Update handleEdit**

Replace the existing `handleEdit`:

```tsx
const handleEdit = (handler: Handler) => {
  setEditingHandler(handler);
  const { moduleParams, extraParams } = parseArgsToForm(handler.args, handler.module);
  setSelectedModule(handler.module);
  form.setFieldsValue({
    name: handler.name,
    module: handler.module,
    moduleParams,
    extraParams: extraParams.length > 0 ? extraParams : undefined,
    whenCondition: handler.whenCondition,
    register: handler.register,
  });
  setModalOpen(true);
};
```

- [ ] **Step 4: Update handleSubmit**

Replace the existing `handleSubmit`:

```tsx
const handleSubmit = async () => {
  const values = await form.validateFields();

  const moduleDef = values.module ? getModuleDefinition(values.module) : undefined;
  if (moduleDef?.validate) {
    const errors = moduleDef.validate(values.moduleParams || {});
    if (Object.keys(errors).length > 0) {
      const fieldErrors = Object.entries(errors).map(([field, msg]) => ({
        name: ['moduleParams', field],
        errors: [msg],
      }));
      form.setFields(fieldErrors);
      return;
    }
  }

  const args = buildArgsJson(values.moduleParams, values.extraParams);

  if (editingHandler) {
    const data: UpdateHandlerRequest = {
      name: values.name,
      module: values.module,
      args: args || undefined,
      whenCondition: values.whenCondition,
      register: values.register,
    };
    await updateHandler(editingHandler.id, data);
    message.success('已更新');
  } else {
    const data: CreateHandlerRequest = {
      name: values.name,
      module: values.module,
      args: args || undefined,
      whenCondition: values.whenCondition,
      register: values.register,
    };
    await createHandler(roleId, data);
    message.success('已创建');
  }
  setModalOpen(false);
  fetchHandlers();
};
```

- [ ] **Step 5: Replace module Input and args TextArea in the form JSX**

Same as Task 4 Step 5 — replace the `module` Form.Item and remove the `args` Form.Item:

```tsx
<Form.Item
  name="module"
  label="模块"
  rules={[{ required: true, message: '请选择 Ansible 模块' }]}
>
  <ModuleSelect
    onChange={(val) => {
      setSelectedModule(val);
      form.setFieldsValue({ moduleParams: undefined, extraParams: undefined });
    }}
  />
</Form.Item>
<ModuleParamsForm moduleName={selectedModule} />
```

Remove the old `<Form.Item name="args" label="参数 (JSON)">` block.

- [ ] **Step 6: Verify TypeScript compiles and lint passes**

Run: `cd frontend && npx tsc --noEmit && npm run lint`
Expected: No errors.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/role/RoleHandlers.tsx
git commit -m "feat: integrate ModuleSelect and ModuleParamsForm into RoleHandlers"
```

---

### Task 6: Manual Testing & Final Verification

- [ ] **Step 1: Start the frontend dev server**

Run: `cd frontend && npm run dev`

- [ ] **Step 2: Test Task creation with copy module**

Navigate to a project → Role → Tasks tab → click "添加 Task":
1. Verify the module dropdown shows copy, template, file with descriptions
2. Hover over a module option — verify "查看文档" link button appears
3. Click the doc link — verify it opens Ansible docs in a new tab
4. Select `copy` → verify dynamic form shows: src, content, dest, owner, group, mode, backup, remote_src
5. Leave src and content both empty, fill dest → submit → verify error "src 和 content 至少填写一个"
6. Fill src and dest → add an extra param (e.g. `force` = `yes`) → submit → verify success

- [ ] **Step 3: Test Task editing with args parsing**

Edit the task just created:
1. Verify module shows `copy` selected
2. Verify src, dest fields are populated from saved args
3. Verify the extra param `force=yes` appears in the key-value list
4. Change module to `template` → verify form fields change and old values are cleared

- [ ] **Step 4: Test Handler creation and editing**

Navigate to Handlers tab and repeat similar tests for Handler creation and editing.

- [ ] **Step 5: Run lint and type check**

Run: `cd frontend && npx tsc --noEmit && npm run lint`
Expected: All pass.

- [ ] **Step 6: Final commit if any fixes needed**

```bash
git add -u
git commit -m "fix: address issues found during manual testing"
```
