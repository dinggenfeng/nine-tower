# Template Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Template management UI as a tab in the Role detail page, following the RoleVars pattern.

**Architecture:** TypeScript types + API layer + RoleTemplates component (Table + Modal CRUD) + wire into RoleDetail tabs. Flat list display with parentDir column; content editing via TextArea in modal; detail fetched on edit via getTemplate(id).

**Tech Stack:** React 18, Ant Design 5, TypeScript, Axios

---

### Task 1: Template Types + API Layer

**Files:**
- Create: `frontend/src/types/entity/Template.ts`
- Create: `frontend/src/api/template.ts`

- [ ] **Step 1: Create Template types**

Create `frontend/src/types/entity/Template.ts`:

```typescript
export interface Template {
  id: number;
  roleId: number;
  parentDir: string | null;
  name: string;
  targetPath: string | null;
  content: string | null;
  createdBy: number;
  createdAt: string;
}

export interface CreateTemplateRequest {
  name: string;
  parentDir?: string;
  targetPath?: string;
  content?: string;
}

export interface UpdateTemplateRequest {
  name?: string;
  parentDir?: string;
  targetPath?: string;
  content?: string;
}
```

- [ ] **Step 2: Create Template API**

Create `frontend/src/api/template.ts`:

```typescript
import request from './request';
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from '../types/entity/Template';

export async function createTemplate(
  roleId: number,
  data: CreateTemplateRequest
): Promise<Template> {
  const res = await request.post<Template>(`/roles/${roleId}/templates`, data);
  return res.data;
}

export async function getTemplates(roleId: number): Promise<Template[]> {
  const res = await request.get<Template[]>(`/roles/${roleId}/templates`);
  return res.data;
}

export async function getTemplate(id: number): Promise<Template> {
  const res = await request.get<Template>(`/templates/${id}`);
  return res.data;
}

export async function updateTemplate(
  id: number,
  data: UpdateTemplateRequest
): Promise<Template> {
  const res = await request.put<Template>(`/templates/${id}`, data);
  return res.data;
}

export async function deleteTemplate(id: number): Promise<void> {
  await request.delete(`/templates/${id}`);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/entity/Template.ts frontend/src/api/template.ts
git commit -m "feat: add Template types and API layer"
```

---

### Task 2: RoleTemplates Component

**Files:**
- Create: `frontend/src/pages/role/RoleTemplates.tsx`

- [ ] **Step 1: Create RoleTemplates component**

Create `frontend/src/pages/role/RoleTemplates.tsx`:

```tsx
import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from '../../types/entity/Template';
import {
  createTemplate,
  getTemplates,
  getTemplate,
  updateTemplate,
  deleteTemplate,
} from '../../api/template';

interface RoleTemplatesProps {
  roleId: number;
}

export default function RoleTemplates({ roleId }: RoleTemplatesProps) {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<Template | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getTemplates(roleId);
      setTemplates(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = () => {
    setEditingTemplate(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = async (record: Template) => {
    const detail = await getTemplate(record.id);
    setEditingTemplate(detail);
    form.setFieldsValue({
      name: detail.name,
      parentDir: detail.parentDir,
      targetPath: detail.targetPath,
      content: detail.content,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingTemplate) {
      const data: UpdateTemplateRequest = {
        name: values.name,
        parentDir: values.parentDir,
        targetPath: values.targetPath,
        content: values.content,
      };
      await updateTemplate(editingTemplate.id, data);
      message.success('模板已更新');
    } else {
      const data: CreateTemplateRequest = {
        name: values.name,
        parentDir: values.parentDir,
        targetPath: values.targetPath,
        content: values.content,
      };
      await createTemplate(roleId, data);
      message.success('模板已创建');
    }
    setModalOpen(false);
    fetchData();
  };

  const handleDelete = async (id: number) => {
    await deleteTemplate(id);
    message.success('模板已删除');
    fetchData();
  };

  const columns = [
    { title: '文件名', dataIndex: 'name', key: 'name' },
    {
      title: '目录',
      dataIndex: 'parentDir',
      key: 'parentDir',
      render: (dir: string | null) => dir || '/',
    },
    { title: '目标路径', dataIndex: 'targetPath', key: 'targetPath' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Template) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加模板
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={templates}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editingTemplate ? '编辑模板' : '添加模板'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="文件名"
            rules={[{ required: true, message: '请输入模板文件名' }]}
          >
            <Input placeholder="例如: nginx.conf.j2" />
          </Form.Item>
          <Form.Item name="parentDir" label="目录">
            <Input placeholder="例如: nginx/conf.d（留空表示根目录）" />
          </Form.Item>
          <Form.Item name="targetPath" label="目标路径">
            <Input placeholder="例如: /etc/nginx/nginx.conf" />
          </Form.Item>
          <Form.Item name="content" label="模板内容">
            <Input.TextArea
              rows={12}
              placeholder="Jinja2 模板内容"
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/role/RoleTemplates.tsx
git commit -m "feat: add RoleTemplates component"
```

---

### Task 3: Wire into RoleDetail

**Files:**
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Update RoleDetail to use RoleTemplates**

In `frontend/src/pages/role/RoleDetail.tsx`:

1. Add import at line 10 (after RoleHandlers import):
```typescript
import RoleTemplates from './RoleTemplates';
```

2. Replace the templates tab item (line 46):
```typescript
// Change from:
{ key: 'templates', label: 'Templates', children: <ComingSoon /> },
// Change to:
{ key: 'templates', label: 'Templates', children: <RoleTemplates roleId={Number(roleId)} /> },
```

- [ ] **Step 2: Verify compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/role/RoleDetail.tsx
git commit -m "feat: wire RoleTemplates into RoleDetail tabs"
```

---

### Task 4: Manual Testing + Cleanup

- [ ] **Step 1: Start dev servers**

Run: `cd frontend && npm run dev` (backend should already be running on port 8080)

- [ ] **Step 2: Manual test in browser**

1. Navigate to a project → Roles → click a Role → Templates tab
2. Test create: add a template with name "nginx.conf.j2", targetPath "/etc/nginx/nginx.conf", content "server { listen {{ port }}; }"
3. Test create with parentDir: add "vhost.conf.j2" in directory "conf.d"
4. Test edit: click edit, verify content loads, modify and save
5. Test delete: delete a template, confirm it disappears

- [ ] **Step 3: Run format check**

Run: `cd frontend && npm run format && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit any formatting changes**

```bash
git add frontend/src/
git commit -m "style: format Template frontend code"
```
