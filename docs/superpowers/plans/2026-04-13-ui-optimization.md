# UI Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the frontend from default Ant Design styling to a professional DevOps tool aesthetic with dark sidebar, blue-gray palette, and polished page layouts.

**Architecture:** Introduce a centralized `theme.ts` for Ant Design 5 ConfigProvider tokens and color constants. Use CSS Modules (built into Vite) for component-specific styling. Extract a reusable `PageHeader` component. All changes are frontend-only with zero new npm dependencies.

**Tech Stack:** React 18, Ant Design 5 (ConfigProvider + theme tokens), CSS Modules, Vite, TypeScript

---

### Task 1: Create Theme Configuration

**Files:**
- Create: `frontend/src/theme.ts`

- [ ] **Step 1: Create theme.ts with Ant Design tokens and color constants**

```typescript
// frontend/src/theme.ts
import type { ThemeConfig } from 'antd';

export const theme: ThemeConfig = {
  token: {
    colorPrimary: '#3b82f6',
    colorBgContainer: '#ffffff',
    colorBgLayout: '#f1f5f9',
    borderRadius: 6,
    fontSize: 14,
    colorText: '#1e293b',
    colorTextSecondary: '#64748b',
    colorBorder: '#e2e8f0',
    colorSuccess: '#10b981',
    colorWarning: '#f59e0b',
    colorError: '#ef4444',
  },
};

/** Colors not covered by Ant Design tokens — use in CSS Modules and inline styles */
export const colors = {
  headerBg: '#0f172a',
  siderBg: '#1e293b',
  siderItemActiveBg: '#3b82f6',
  siderTextColor: '#94a3b8',
  siderTextActiveColor: '#f1f5f9',
  siderGroupLabel: '#475569',
  tagAdminBg: '#1e3a5f',
  tagAdminColor: '#3b82f6',
  tagMemberBg: '#1e293b',
  tagMemberColor: '#94a3b8',
} as const;
```

- [ ] **Step 2: Wire theme into ConfigProvider in main.tsx**

Replace the full content of `frontend/src/main.tsx` with:

```typescript
import { App as AntApp, ConfigProvider } from 'antd';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { theme } from './theme';
import 'antd/dist/reset.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <ConfigProvider theme={theme}>
        <AntApp>
          <App />
        </AntApp>
      </ConfigProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
```

- [ ] **Step 3: Verify the app starts without errors**

Run: `cd frontend && npm run dev`
Expected: App launches at localhost:5173. Colors should already be subtly different (primary blue, border colors).

- [ ] **Step 4: Run lint to verify**

Run: `cd frontend && npm run lint`
Expected: No new lint errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/theme.ts frontend/src/main.tsx
git commit -m "feat(frontend): add centralized Ant Design theme configuration"
```

---

### Task 2: Create PageHeader Component

**Files:**
- Create: `frontend/src/components/PageHeader.tsx`

- [ ] **Step 1: Create the PageHeader component**

```typescript
// frontend/src/components/PageHeader.tsx
import { Typography } from 'antd';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export default function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: 20,
        paddingBottom: 16,
        borderBottom: '1px solid #f1f5f9',
      }}
    >
      <div>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        {description && (
          <Typography.Text type="secondary" style={{ marginTop: 4, display: 'block' }}>
            {description}
          </Typography.Text>
        )}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}
```

- [ ] **Step 2: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/PageHeader.tsx
git commit -m "feat(frontend): add reusable PageHeader component"
```

---

### Task 3: Redesign MainLayout (Dark Header)

**Files:**
- Modify: `frontend/src/components/Layout/MainLayout.tsx`
- Create: `frontend/src/components/Layout/MainLayout.module.css`

- [ ] **Step 1: Create MainLayout.module.css**

```css
/* frontend/src/components/Layout/MainLayout.module.css */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #0f172a;
  padding: 0 24px;
  height: 56px;
  line-height: 56px;
  border-bottom: 1px solid #1e293b;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.logoIcon {
  font-size: 20px;
  color: #3b82f6;
}

.logoText {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.userArea {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: #94a3b8;
  font-size: 14px;
}

.content {
  padding: 24px;
  background: #f1f5f9;
  min-height: calc(100vh - 56px);
}
```

- [ ] **Step 2: Rewrite MainLayout.tsx to use CSS Module and dark header**

Replace the full content of `frontend/src/components/Layout/MainLayout.tsx` with:

```typescript
import { LogoutOutlined, ProjectOutlined } from '@ant-design/icons';
import { Avatar, Dropdown, Layout, type MenuProps } from 'antd';
import { useNavigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import styles from './MainLayout.module.css';

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header className={styles.header}>
        <div className={styles.logo} onClick={() => navigate('/projects')}>
          <ProjectOutlined className={styles.logoIcon} />
          <span className={styles.logoText}>Ansible Playbook Studio</span>
        </div>
        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <div className={styles.userArea}>
            <Avatar size="small" style={{ backgroundColor: '#3b82f6' }}>
              {user?.username?.[0]?.toUpperCase()}
            </Avatar>
            <span className={styles.username}>{user?.username}</span>
          </div>
        </Dropdown>
      </Layout.Header>
      <Layout.Content className={styles.content}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
```

- [ ] **Step 3: Verify the app renders with the dark header**

Run: `cd frontend && npm run dev`
Expected: Header is now dark (#0f172a), logo text is "Ansible Playbook Studio" in white, user area has subtle gray text.

- [ ] **Step 4: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/Layout/MainLayout.tsx frontend/src/components/Layout/MainLayout.module.css
git commit -m "feat(frontend): redesign MainLayout with dark header"
```

---

### Task 4: Redesign ProjectLayout (Dark Sidebar + Breadcrumb)

**Files:**
- Modify: `frontend/src/components/Layout/ProjectLayout.tsx`
- Create: `frontend/src/components/Layout/ProjectLayout.module.css`

- [ ] **Step 1: Create ProjectLayout.module.css**

```css
/* frontend/src/components/Layout/ProjectLayout.module.css */
.sider {
  background: #1e293b !important;
  border-right: 1px solid #334155;
}

.siderHeader {
  padding: 16px;
  font-weight: 600;
  font-size: 15px;
  color: #f1f5f9;
  border-bottom: 1px solid #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.groupLabel {
  padding: 12px 16px 4px;
  font-size: 11px;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 500;
}

.navList {
  padding: 4px 8px;
}

.navItem {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 0 6px 6px 0;
  margin-bottom: 2px;
  cursor: pointer;
  color: #94a3b8;
  font-size: 14px;
  transition: all 0.15s;
}

.navItem:hover {
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.05);
}

.navItemActive {
  background: #3b82f6;
  color: #f1f5f9;
}

.navItemActive:hover {
  background: #3b82f6;
  color: #f1f5f9;
}

.content {
  background: #f1f5f9;
  padding: 24px;
  min-height: 100%;
}

.breadcrumb {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 16px;
}

.breadcrumbLink {
  color: #64748b;
  cursor: pointer;
}

.breadcrumbLink:hover {
  color: #3b82f6;
}

.breadcrumbCurrent {
  color: #1e293b;
}

.contentCard {
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  min-height: calc(100vh - 160px);
}
```

- [ ] **Step 2: Rewrite ProjectLayout.tsx with dark sidebar, grouped nav, and breadcrumb**

Replace the full content of `frontend/src/components/Layout/ProjectLayout.tsx` with:

```typescript
import { useEffect } from 'react';
import { Layout } from 'antd';
import {
  TeamOutlined,
  SettingOutlined,
  DatabaseOutlined,
  TagsOutlined,
  CloudOutlined,
  CodeOutlined,
  PlayCircleOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useParams, useLocation } from 'react-router-dom';
import { useProjectStore } from '../../stores/projectStore';
import { getProject } from '../../api/project';
import styles from './ProjectLayout.module.css';

const navGroups = [
  {
    label: '导航',
    items: [
      { key: 'roles', icon: <CodeOutlined />, label: 'Roles' },
      { key: 'host-groups', icon: <DatabaseOutlined />, label: '主机组' },
      { key: 'variables', icon: <AppstoreOutlined />, label: '变量' },
      { key: 'environments', icon: <CloudOutlined />, label: '环境' },
      { key: 'tags', icon: <TagsOutlined />, label: '标签' },
      { key: 'playbooks', icon: <PlayCircleOutlined />, label: '剧本' },
    ],
  },
  {
    label: '管理',
    items: [
      { key: 'members', icon: <TeamOutlined />, label: '成员' },
      { key: 'settings', icon: <SettingOutlined />, label: '设置' },
    ],
  },
];

const keyToLabel: Record<string, string> = {};
navGroups.forEach((g) => g.items.forEach((item) => { keyToLabel[item.key] = item.label; }));

export default function ProjectLayout() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (id) {
      getProject(Number(id)).then(setCurrentProject);
    }
    return () => setCurrentProject(null);
  }, [id, setCurrentProject]);

  const pathParts = location.pathname.split('/');
  const currentKey = pathParts[3] || 'roles';

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Layout.Sider width={200} className={styles.sider}>
        <div className={styles.siderHeader}>
          {currentProject?.name || '加载中...'}
        </div>
        {navGroups.map((group) => (
          <div key={group.label}>
            <div className={styles.groupLabel}>{group.label}</div>
            <div className={styles.navList}>
              {group.items.map((item) => (
                <div
                  key={item.key}
                  className={`${styles.navItem} ${currentKey === item.key ? styles.navItemActive : ''}`}
                  onClick={() => navigate(`/projects/${id}/${item.key}`)}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </Layout.Sider>
      <Layout.Content className={styles.content}>
        <div className={styles.breadcrumb}>
          <span
            className={styles.breadcrumbLink}
            onClick={() => navigate('/projects')}
          >
            项目
          </span>
          {' / '}
          <span
            className={styles.breadcrumbLink}
            onClick={() => navigate(`/projects/${id}/roles`)}
          >
            {currentProject?.name || '...'}
          </span>
          {' / '}
          <span className={styles.breadcrumbCurrent}>
            {keyToLabel[currentKey] || currentKey}
          </span>
        </div>
        <div className={styles.contentCard}>
          <Outlet />
        </div>
      </Layout.Content>
    </Layout>
  );
}
```

- [ ] **Step 3: Verify dark sidebar renders correctly**

Run: `cd frontend && npm run dev`
Expected: Navigate to a project. Sidebar is dark (#1e293b) with grouped navigation ("导航" and "管理" sections). Active item is blue. Content area has breadcrumb navigation and white card with subtle shadow.

- [ ] **Step 4: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/Layout/ProjectLayout.tsx frontend/src/components/Layout/ProjectLayout.module.css
git commit -m "feat(frontend): redesign ProjectLayout with dark sidebar and breadcrumb"
```

---

### Task 5: Redesign Login Page

**Files:**
- Modify: `frontend/src/pages/auth/Login.tsx`
- Create: `frontend/src/pages/auth/Login.module.css`

- [ ] **Step 1: Create Login.module.css**

```css
/* frontend/src/pages/auth/Login.module.css */
.container {
  min-height: 100vh;
  display: flex;
}

.brandSection {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  position: relative;
  overflow: hidden;
}

.brandSection::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0.05;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 20px,
    #3b82f6 20px,
    #3b82f6 21px
  );
}

.brandContent {
  position: relative;
  z-index: 1;
}

.brandLabel {
  font-size: 13px;
  color: #3b82f6;
  letter-spacing: 3px;
  text-transform: uppercase;
  margin-bottom: 8px;
  font-weight: 500;
}

.brandTitle {
  font-size: 32px;
  font-weight: 700;
  color: #f1f5f9;
  margin-bottom: 12px;
  line-height: 1.2;
}

.brandDescription {
  font-size: 15px;
  color: #64748b;
  line-height: 1.6;
}

.brandTags {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.brandTag {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  color: #94a3b8;
}

.formSection {
  flex: 1;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.formWrapper {
  width: 100%;
  max-width: 360px;
}

.formTitle {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.formSubtitle {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 32px;
}

.formFooter {
  text-align: center;
  margin-top: 16px;
  color: #64748b;
  font-size: 14px;
}

@media (max-width: 768px) {
  .brandSection {
    display: none;
  }

  .formSection {
    padding: 24px;
  }
}
```

- [ ] **Step 2: Rewrite Login.tsx with split layout**

Replace the full content of `frontend/src/pages/auth/Login.tsx` with:

```typescript
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Input, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, LoginPayload } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import styles from './Login.module.css';

export default function Login() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<LoginPayload>();

  const onFinish = async (values: LoginPayload) => {
    try {
      const res = await authApi.login(values);
      login(res.data.token, res.data.user);
      message.success('登录成功');
      navigate('/projects');
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? '登录失败');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.brandSection}>
        <div className={styles.brandContent}>
          <div className={styles.brandLabel}>Ansible</div>
          <div className={styles.brandTitle}>Playbook Studio</div>
          <div className={styles.brandDescription}>
            可视化开发和管理你的 Ansible Playbook
          </div>
          <div className={styles.brandTags}>
            <span className={styles.brandTag}>项目管理</span>
            <span className={styles.brandTag}>主机管理</span>
            <span className={styles.brandTag}>角色编排</span>
            <span className={styles.brandTag}>剧本开发</span>
          </div>
        </div>
      </div>
      <div className={styles.formSection}>
        <div className={styles.formWrapper}>
          <div className={styles.formTitle}>欢迎回来</div>
          <div className={styles.formSubtitle}>登录以继续</div>
          <Form form={form} onFinish={onFinish} layout="vertical" size="large">
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                登 录
              </Button>
            </Form.Item>
          </Form>
          <div className={styles.formFooter}>
            还没有账号？<Link to="/register">立即注册</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Verify login page renders with split layout**

Run: `cd frontend && npm run dev`
Expected: Login page shows left dark brand panel with "Playbook Studio" branding + right white form panel. On narrow screens (<768px), brand panel hides.

- [ ] **Step 4: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/auth/Login.tsx frontend/src/pages/auth/Login.module.css
git commit -m "feat(frontend): redesign login page with split brand/form layout"
```

---

### Task 6: Redesign Register Page

**Files:**
- Modify: `frontend/src/pages/auth/Register.tsx`

- [ ] **Step 1: Rewrite Register.tsx reusing the Login CSS Module**

Replace the full content of `frontend/src/pages/auth/Register.tsx` with:

```typescript
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Input, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, RegisterPayload } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import styles from './Login.module.css';

export default function Register() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<RegisterPayload>();

  const onFinish = async (values: RegisterPayload) => {
    try {
      const res = await authApi.register(values);
      login(res.data.token, res.data.user);
      message.success('注册成功');
      navigate('/projects');
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? '注册失败');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.brandSection}>
        <div className={styles.brandContent}>
          <div className={styles.brandLabel}>Ansible</div>
          <div className={styles.brandTitle}>Playbook Studio</div>
          <div className={styles.brandDescription}>
            可视化开发和管理你的 Ansible Playbook
          </div>
          <div className={styles.brandTags}>
            <span className={styles.brandTag}>项目管理</span>
            <span className={styles.brandTag}>主机管理</span>
            <span className={styles.brandTag}>角色编排</span>
            <span className={styles.brandTag}>剧本开发</span>
          </div>
        </div>
      </div>
      <div className={styles.formSection}>
        <div className={styles.formWrapper}>
          <div className={styles.formTitle}>创建账号</div>
          <div className={styles.formSubtitle}>注册新账号以开始使用</div>
          <Form form={form} onFinish={onFinish} layout="vertical" size="large">
            <Form.Item
              name="username"
              rules={[
                { required: true, message: '请输入用户名' },
                { min: 3, message: '用户名至少3个字符' },
                { max: 50, message: '用户名最多50个字符' },
              ]}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" />
            </Form.Item>
            <Form.Item
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱' },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="邮箱" />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码至少8个字符' },
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                注 册
              </Button>
            </Form.Item>
          </Form>
          <div className={styles.formFooter}>
            已有账号？<Link to="/login">登录</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify register page matches login design**

Run: `cd frontend && npm run dev`
Expected: Register page has same split layout as login. Title says "创建账号", subtitle says "注册新账号以开始使用".

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/auth/Register.tsx
git commit -m "feat(frontend): redesign register page reusing login layout"
```

---

### Task 7: Redesign ProjectList Page

**Files:**
- Modify: `frontend/src/pages/project/ProjectList.tsx`

- [ ] **Step 1: Rewrite ProjectList.tsx with redesigned cards**

Replace the full content of `frontend/src/pages/project/ProjectList.tsx` with:

```typescript
import { useEffect, useState } from 'react';
import { Button, Empty, List, Modal, Form, Input, message, Dropdown } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  SettingOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { Project, CreateProjectRequest } from '../../types/entity/Project';
import { getMyProjects, createProject, deleteProject } from '../../api/project';
import { colors } from '../../theme';

const { TextArea } = Input;

export default function ProjectList() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm<CreateProjectRequest>();
  const navigate = useNavigate();

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const data = await getMyProjects();
      setProjects(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createProject(values);
    message.success('项目创建成功');
    setCreateModalOpen(false);
    form.resetFields();
    fetchProjects();
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除项目？',
      content: '此操作无法撤销。',
      onOk: async () => {
        await deleteProject(id);
        message.success('项目已删除');
        fetchProjects();
      },
    });
  };

  return (
    <div style={{ padding: 24 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: 8,
        }}
      >
        <h2 style={{ margin: 0, color: '#1e293b' }}>我的项目</h2>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
        >
          新建项目
        </Button>
      </div>
      <div style={{ color: '#64748b', fontSize: 14, marginBottom: 20 }}>
        共 {projects.length} 个项目
      </div>

      <List
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4 }}
        loading={loading}
        dataSource={projects}
        locale={{
          emptyText: (
            <Empty description="暂无项目">
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setCreateModalOpen(true)}
              >
                创建第一个项目
              </Button>
            </Empty>
          ),
        }}
        renderItem={(project) => {
          const isAdmin = project.myRole === 'PROJECT_ADMIN';
          const initial = project.name[0]?.toUpperCase() || 'P';

          const menuItems = [
            isAdmin && {
              key: 'settings',
              icon: <SettingOutlined />,
              label: '项目设置',
              onClick: () => navigate(`/projects/${project.id}/settings`),
            },
            isAdmin && {
              key: 'delete',
              icon: <DeleteOutlined />,
              label: '删除项目',
              danger: true,
              onClick: () => handleDelete(project.id),
            },
          ].filter(Boolean);

          return (
            <List.Item>
              <div
                onClick={() => navigate(`/projects/${project.id}/roles`)}
                style={{
                  background: '#fff',
                  borderRadius: 8,
                  padding: 16,
                  border: '1px solid #e2e8f0',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)';
                  e.currentTarget.style.borderColor = '#cbd5e1';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.04)';
                  e.currentTarget.style.borderColor = '#e2e8f0';
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                  }}
                >
                  <div
                    style={{
                      width: 32,
                      height: 32,
                      background: '#1e293b',
                      borderRadius: 6,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 14,
                      fontWeight: 600,
                      color: '#f1f5f9',
                    }}
                  >
                    {initial}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span
                      style={{
                        background: isAdmin ? colors.tagAdminBg : colors.tagMemberBg,
                        color: isAdmin ? colors.tagAdminColor : colors.tagMemberColor,
                        fontSize: 11,
                        padding: '2px 8px',
                        borderRadius: 4,
                        fontWeight: 500,
                      }}
                    >
                      {isAdmin ? '管理员' : '成员'}
                    </span>
                    {menuItems.length > 0 && (
                      <Dropdown
                        menu={{ items: menuItems }}
                        trigger={['click']}
                        placement="bottomRight"
                      >
                        <MoreOutlined
                          onClick={(e) => e.stopPropagation()}
                          style={{ color: '#94a3b8', fontSize: 16 }}
                        />
                      </Dropdown>
                    )}
                  </div>
                </div>
                <div
                  style={{
                    fontSize: 15,
                    fontWeight: 600,
                    color: '#1e293b',
                    marginTop: 12,
                  }}
                >
                  {project.name}
                </div>
                <div
                  style={{
                    fontSize: 13,
                    color: '#64748b',
                    marginTop: 4,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {project.description || '暂无描述'}
                </div>
              </div>
            </List.Item>
          );
        }}
      />

      <Modal
        title="创建项目"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => {
          setCreateModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Verify project list renders with new card design**

Run: `cd frontend && npm run dev`
Expected: Project cards show initial avatar (dark square), role tag in dark style, hover shadow effect, project count below title, improved empty state.

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/project/ProjectList.tsx
git commit -m "feat(frontend): redesign project list with avatar cards and dark role tags"
```

---

### Task 8: Redesign MemberManagement Page

**Files:**
- Modify: `frontend/src/pages/project/MemberManagement.tsx`

- [ ] **Step 1: Rewrite MemberManagement.tsx with PageHeader and dark tags**

Replace the full content of `frontend/src/pages/project/MemberManagement.tsx` with:

```typescript
import { useEffect, useState, useCallback } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  InputNumber,
  Select,
  message,
  Popconfirm,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  ProjectMember,
  AddMemberRequest,
} from '../../types/entity/Project';
import {
  getMembers,
  addMember,
  removeMember,
  updateMemberRole,
} from '../../api/project';
import { useProjectStore } from '../../stores/projectStore';
import { colors } from '../../theme';
import PageHeader from '../../components/PageHeader';

export default function MemberManagement() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [form] = Form.useForm<AddMemberRequest>();
  const { currentProject } = useProjectStore();
  const isAdmin = currentProject?.myRole === 'PROJECT_ADMIN';

  const fetchMembers = useCallback(async () => {
    setLoading(true);
    try {
      setMembers(await getMembers(projectId));
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const handleAdd = async () => {
    const values = await form.validateFields();
    await addMember(projectId, values);
    message.success('成员添加成功');
    setAddModalOpen(false);
    form.resetFields();
    fetchMembers();
  };

  const handleRemove = async (userId: number) => {
    await removeMember(projectId, userId);
    message.success('成员已移除');
    fetchMembers();
  };

  const handleRoleChange = async (
    userId: number,
    role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER',
  ) => {
    await updateMemberRole(projectId, userId, { role });
    message.success('角色更新成功');
    fetchMembers();
  };

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string, record: ProjectMember) =>
        isAdmin ? (
          <Select
            value={role}
            onChange={(value) =>
              handleRoleChange(
                record.userId,
                value as 'PROJECT_ADMIN' | 'PROJECT_MEMBER',
              )
            }
            options={[
              { value: 'PROJECT_ADMIN', label: '管理员' },
              { value: 'PROJECT_MEMBER', label: '成员' },
            ]}
            style={{ width: 120 }}
          />
        ) : (
          <span
            style={{
              background:
                role === 'PROJECT_ADMIN'
                  ? colors.tagAdminBg
                  : colors.tagMemberBg,
              color:
                role === 'PROJECT_ADMIN'
                  ? colors.tagAdminColor
                  : colors.tagMemberColor,
              fontSize: 12,
              padding: '2px 8px',
              borderRadius: 4,
              fontWeight: 500,
            }}
          >
            {role === 'PROJECT_ADMIN' ? '管理员' : '成员'}
          </span>
        ),
    },
    {
      title: '加入时间',
      dataIndex: 'joinedAt',
      key: 'joinedAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    ...(isAdmin
      ? [
          {
            title: '操作',
            key: 'action',
            render: (_: unknown, record: ProjectMember) => (
              <Popconfirm
                title="确认移除此成员？"
                onConfirm={() => handleRemove(record.userId)}
              >
                <Button type="link" danger size="small">
                  移除
                </Button>
              </Popconfirm>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <PageHeader
        title="成员管理"
        description="管理项目成员和权限"
        action={
          isAdmin ? (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setAddModalOpen(true)}
            >
              添加成员
            </Button>
          ) : undefined
        }
      />

      <Table
        columns={columns}
        dataSource={members}
        rowKey="userId"
        loading={loading}
        pagination={false}
      />

      <Modal
        title="添加成员"
        open={addModalOpen}
        onOk={handleAdd}
        onCancel={() => {
          setAddModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="userId"
            label="用户 ID"
            rules={[{ required: true, message: '请输入用户 ID' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select
              options={[
                { value: 'PROJECT_ADMIN', label: '管理员' },
                { value: 'PROJECT_MEMBER', label: '成员' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Verify member management page**

Run: `cd frontend && npm run dev`
Expected: Page shows PageHeader with title "成员管理", description, and "添加成员" button. Role tags use dark styling.

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/project/MemberManagement.tsx
git commit -m "feat(frontend): redesign member management with PageHeader and dark role tags"
```

---

### Task 9: Redesign RoleList Page

**Files:**
- Modify: `frontend/src/pages/role/RoleList.tsx`

- [ ] **Step 1: Rewrite RoleList.tsx with PageHeader**

Replace the full content of `frontend/src/pages/role/RoleList.tsx` with:

```typescript
import { useEffect, useState, useCallback } from 'react';
import { Button, Form, Input, message, Modal, Popconfirm, Space, Table } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import type { Role, CreateRoleRequest } from '../../types/entity/Role';
import { createRole, deleteRole, getRoles, updateRole } from '../../api/role';
import PageHeader from '../../components/PageHeader';

export default function RoleList() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);
  const navigate = useNavigate();

  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [form] = Form.useForm<CreateRoleRequest>();

  const fetchRoles = useCallback(async () => {
    setLoading(true);
    try {
      setRoles(await getRoles(pid));
    } finally {
      setLoading(false);
    }
  }, [pid]);

  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createRole(pid, values);
    message.success('Role 创建成功');
    setModalOpen(false);
    form.resetFields();
    fetchRoles();
  };

  const handleUpdate = async () => {
    if (!editingRole) return;
    const values = await form.validateFields();
    await updateRole(editingRole.id, values);
    message.success('Role 更新成功');
    setModalOpen(false);
    setEditingRole(null);
    form.resetFields();
    fetchRoles();
  };

  const handleDelete = async (roleId: number) => {
    await deleteRole(roleId);
    message.success('Role 已删除');
    fetchRoles();
  };

  const openModal = (role?: Role) => {
    if (role) {
      setEditingRole(role);
      form.setFieldsValue({ name: role.name, description: role.description });
    } else {
      setEditingRole(null);
      form.resetFields();
    }
    setModalOpen(true);
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: Role) => (
        <a
          onClick={() => navigate(`/projects/${pid}/roles/${record.id}`)}
          style={{ color: '#3b82f6', fontWeight: 500 }}
        >
          {name}
        </a>
      ),
    },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: Role) => (
        <Space>
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openModal(record)}
          />
          <Popconfirm
            title="确认删除此 Role？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Roles"
        description="管理 Ansible Roles"
        action={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => openModal()}
          >
            新建 Role
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={roles}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingRole ? '编辑 Role' : '新建 Role'}
        open={modalOpen}
        onOk={editingRole ? handleUpdate : handleCreate}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Verify role list page**

Run: `cd frontend && npm run dev`
Expected: PageHeader with "Roles" title, role names are blue links, action column is compact.

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/role/RoleList.tsx
git commit -m "feat(frontend): redesign role list with PageHeader"
```

---

### Task 10: Fix RoleDetail Page (Deprecated API + Empty State)

**Files:**
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Rewrite RoleDetail.tsx replacing deprecated TabPane with items prop**

Replace the full content of `frontend/src/pages/role/RoleDetail.tsx` with:

```typescript
import { useEffect, useState } from 'react';
import { Button, Card, Empty, Skeleton, Tabs } from 'antd';
import { ArrowLeftOutlined, InboxOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import type { Role } from '../../types/entity/Role';
import { getRole } from '../../api/role';

function ComingSoon() {
  return (
    <Empty
      image={<InboxOutlined style={{ fontSize: 48, color: '#94a3b8' }} />}
      description={
        <span style={{ color: '#64748b' }}>即将推出</span>
      }
      style={{ padding: '48px 0' }}
    />
  );
}

const tabItems = [
  { key: 'tasks', label: 'Tasks', children: <ComingSoon /> },
  { key: 'handlers', label: 'Handlers', children: <ComingSoon /> },
  { key: 'templates', label: 'Templates', children: <ComingSoon /> },
  { key: 'files', label: 'Files', children: <ComingSoon /> },
  { key: 'vars', label: 'Vars', children: <ComingSoon /> },
  { key: 'defaults', label: 'Defaults', children: <ComingSoon /> },
];

export default function RoleDetail() {
  const { id, roleId } = useParams<{ id: string; roleId: string }>();
  const navigate = useNavigate();
  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (roleId) {
      getRole(Number(roleId)).then((r) => {
        setRole(r);
        setLoading(false);
      });
    }
  }, [roleId]);

  if (loading) {
    return <Skeleton active />;
  }

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(`/projects/${id}/roles`)}
          style={{ color: '#64748b', padding: '4px 8px' }}
        >
          返回 Roles
        </Button>
      </div>
      <Card
        style={{ marginBottom: 16 }}
        title={
          <span style={{ fontSize: 18, fontWeight: 600 }}>{role?.name}</span>
        }
      >
        <p style={{ color: '#64748b', margin: 0 }}>
          {role?.description || '无描述'}
        </p>
      </Card>
      <Card bodyStyle={{ padding: 0 }}>
        <Tabs
          defaultActiveKey="tasks"
          items={tabItems}
          style={{ padding: '0 24px' }}
        />
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Verify role detail page**

Run: `cd frontend && npm run dev`
Expected: Back button navigates to role list. Tabs use `items` prop (no console deprecation warnings). Empty tabs show icon + "即将推出" in a polished Empty component.

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/role/RoleDetail.tsx
git commit -m "fix(frontend): replace deprecated TabPane with items prop, improve empty state"
```

---

### Task 11: Redesign ProjectSettings Page

**Files:**
- Modify: `frontend/src/pages/project/ProjectSettings.tsx`

- [ ] **Step 1: Rewrite ProjectSettings.tsx with PageHeader and styled danger zone**

Replace the full content of `frontend/src/pages/project/ProjectSettings.tsx` with:

```typescript
import { useEffect, useState } from 'react';
import { Form, Input, Button, message, Card } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import type { UpdateProjectRequest } from '../../types/entity/Project';
import { updateProject, deleteProject } from '../../api/project';
import { useProjectStore } from '../../stores/projectStore';
import PageHeader from '../../components/PageHeader';

const { TextArea } = Input;

export default function ProjectSettings() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm<UpdateProjectRequest>();
  const [loading, setLoading] = useState(false);
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (currentProject) {
      form.setFieldsValue({
        name: currentProject.name,
        description: currentProject.description,
      });
    }
  }, [currentProject, form]);

  const handleUpdate = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const updated = await updateProject(Number(id), values);
      setCurrentProject(updated);
      message.success('项目更新成功');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = () => {
    deleteProject(Number(id)).then(() => {
      message.success('项目已删除');
      navigate('/projects');
    });
  };

  if (currentProject?.myRole !== 'PROJECT_ADMIN') {
    return <div>仅项目管理员可访问设置。</div>;
  }

  return (
    <div>
      <PageHeader title="项目设置" description="管理项目基本信息" />
      <Card style={{ maxWidth: 600 }}>
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item
            name="name"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              保存
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="危险区域"
        style={{
          maxWidth: 600,
          marginTop: 24,
          borderLeft: '3px solid #ef4444',
          background: '#fef2f2',
        }}
      >
        <p style={{ color: '#64748b', marginBottom: 16 }}>
          删除项目后，所有数据将永久丢失且无法恢复。
        </p>
        <Button danger onClick={handleDelete}>
          删除项目
        </Button>
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Verify project settings page**

Run: `cd frontend && npm run dev`
Expected: PageHeader with "项目设置", danger zone card has red left border and light red background with warning text.

- [ ] **Step 3: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/project/ProjectSettings.tsx
git commit -m "feat(frontend): redesign project settings with PageHeader and styled danger zone"
```

---

### Task 12: Redesign HostGroupManager Page

**Files:**
- Modify: `frontend/src/pages/host/HostGroupManager.tsx`
- Create: `frontend/src/pages/host/HostGroupManager.module.css`

- [ ] **Step 1: Create HostGroupManager.module.css**

```css
/* frontend/src/pages/host/HostGroupManager.module.css */
.container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 200px);
}

.leftPanel {
  width: 300px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.leftHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.leftTitle {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.groupList {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.groupItem {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.15s;
}

.groupItem:hover {
  background: #f8fafc;
}

.groupItemActive {
  background: #1e293b;
}

.groupItemActive:hover {
  background: #1e293b;
}

.groupName {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.groupItemActive .groupName {
  color: #f1f5f9;
}

.groupCount {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.groupItemActive .groupCount {
  color: #64748b;
}

.groupActions {
  color: #94a3b8;
}

.groupItemActive .groupActions {
  color: #64748b;
}

.rightPanel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rightHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.rightTitle {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.rightSubtitle {
  font-size: 13px;
  color: #94a3b8;
  margin-left: 8px;
  font-weight: 400;
}

.ipCell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  color: #334155;
}
```

- [ ] **Step 2: Rewrite HostGroupManager.tsx with CSS Module and improved visuals**

Replace the full content of `frontend/src/pages/host/HostGroupManager.tsx` with:

```typescript
import { useEffect, useState, useCallback } from 'react';
import {
  Button,
  Checkbox,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  HostGroup,
  Host,
  CreateHostGroupRequest,
  CreateHostRequest,
} from '../../types/entity/Host';
import {
  createHostGroup,
  deleteHostGroup,
  getHostGroups,
  updateHostGroup,
  createHost,
  deleteHost,
  getHosts,
  updateHost,
} from '../../api/host';
import styles from './HostGroupManager.module.css';

const { TextArea } = Input;

export default function HostGroupManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [selectedHostGroup, setSelectedHostGroup] = useState<HostGroup | null>(null);
  const [hosts, setHosts] = useState<Host[]>([]);
  const [loading, setLoading] = useState(false);
  const [hgModalOpen, setHgModalOpen] = useState(false);
  const [hostModalOpen, setHostModalOpen] = useState(false);
  const [editingHg, setEditingHg] = useState<HostGroup | null>(null);
  const [editingHost, setEditingHost] = useState<Host | null>(null);
  const [hgForm] = Form.useForm<CreateHostGroupRequest>();
  const [hostForm] = Form.useForm<CreateHostRequest>();

  const fetchHostGroups = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getHostGroups(pid);
      setHostGroups(data);
    } finally {
      setLoading(false);
    }
  }, [pid]);

  const fetchHosts = useCallback(async (hgId: number) => {
    setLoading(true);
    try {
      setHosts(await getHosts(hgId));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHostGroups();
  }, [fetchHostGroups]);

  const handleSelectHostGroup = (hg: HostGroup) => {
    setSelectedHostGroup(hg);
    fetchHosts(hg.id);
  };

  const handleCreateHg = async () => {
    const values = await hgForm.validateFields();
    await createHostGroup(pid, values);
    message.success('主机组创建成功');
    setHgModalOpen(false);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleUpdateHg = async () => {
    if (!editingHg) return;
    const values = await hgForm.validateFields();
    await updateHostGroup(editingHg.id, values);
    message.success('主机组更新成功');
    setHgModalOpen(false);
    setEditingHg(null);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleDeleteHg = async (hgId: number) => {
    await deleteHostGroup(hgId);
    message.success('主机组已删除');
    if (selectedHostGroup?.id === hgId) {
      setSelectedHostGroup(null);
      setHosts([]);
    }
    fetchHostGroups();
  };

  const openHgModal = (hg?: HostGroup) => {
    if (hg) {
      setEditingHg(hg);
      hgForm.setFieldsValue({ name: hg.name, description: hg.description });
    } else {
      setEditingHg(null);
      hgForm.resetFields();
    }
    setHgModalOpen(true);
  };

  const handleCreateHost = async () => {
    if (!selectedHostGroup) return;
    const values = await hostForm.validateFields();
    await createHost(selectedHostGroup.id, values);
    message.success('主机创建成功');
    setHostModalOpen(false);
    hostForm.resetFields();
    fetchHosts(selectedHostGroup.id);
  };

  const handleUpdateHost = async () => {
    if (!editingHost) return;
    const values = await hostForm.validateFields();
    await updateHost(editingHost.id, values);
    message.success('主机更新成功');
    setHostModalOpen(false);
    setEditingHost(null);
    hostForm.resetFields();
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const handleDeleteHost = async (hostId: number) => {
    await deleteHost(hostId);
    message.success('主机已删除');
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const openHostModal = (host?: Host) => {
    if (host) {
      setEditingHost(host);
      hostForm.setFieldsValue({
        name: host.name,
        ip: host.ip,
        port: host.port,
        ansibleUser: host.ansibleUser,
        ansibleBecome: host.ansibleBecome,
      });
    } else {
      setEditingHost(null);
      hostForm.resetFields();
    }
    setHostModalOpen(true);
  };

  const hostColumns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <span style={{ fontWeight: 500 }}>{name}</span>,
    },
    {
      title: 'IP',
      dataIndex: 'ip',
      key: 'ip',
      render: (ip: string) => <span className={styles.ipCell}>{ip}</span>,
    },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: 'SSH用户', dataIndex: 'ansibleUser', key: 'ansibleUser' },
    {
      title: '提权',
      dataIndex: 'ansibleBecome',
      key: 'ansibleBecome',
      render: (v: boolean) => (
        <Tag color={v ? 'green' : 'default'}>{v ? '是' : '否'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: Host) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => openHostModal(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确认删除此主机？"
            onConfirm={() => handleDeleteHost(record.id)}
          >
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      {/* Left: HostGroup list */}
      <div className={styles.leftPanel}>
        <div className={styles.leftHeader}>
          <span className={styles.leftTitle}>主机组</span>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => openHgModal()}
          />
        </div>
        <div className={styles.groupList}>
          {hostGroups.length === 0 && !loading && (
            <Empty
              description="暂无主机组"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              style={{ marginTop: 40 }}
            />
          )}
          {hostGroups.map((hg) => {
            const isActive = selectedHostGroup?.id === hg.id;
            return (
              <div
                key={hg.id}
                className={`${styles.groupItem} ${isActive ? styles.groupItemActive : ''}`}
                onClick={() => handleSelectHostGroup(hg)}
              >
                <div>
                  <div className={styles.groupName}>{hg.name}</div>
                  <div className={styles.groupCount}>
                    {hg.description || '无描述'}
                  </div>
                </div>
                <Space
                  className={styles.groupActions}
                  onClick={(e) => e.stopPropagation()}
                >
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => openHgModal(hg)}
                    style={{ color: 'inherit' }}
                  />
                  <Popconfirm
                    title="确认删除此主机组？"
                    onConfirm={() => handleDeleteHg(hg.id)}
                  >
                    <Button
                      type="text"
                      size="small"
                      icon={<DeleteOutlined />}
                      style={{ color: 'inherit' }}
                    />
                  </Popconfirm>
                </Space>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right: Host list */}
      <div className={styles.rightPanel}>
        <div className={styles.rightHeader}>
          <div>
            <span className={styles.rightTitle}>
              {selectedHostGroup ? selectedHostGroup.name : '请选择主机组'}
            </span>
            {selectedHostGroup && (
              <span className={styles.rightSubtitle}>
                {hosts.length} 台主机
              </span>
            )}
          </div>
          {selectedHostGroup && (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => openHostModal()}
            >
              添加主机
            </Button>
          )}
        </div>
        <div style={{ flex: 1, overflow: 'auto' }}>
          {selectedHostGroup ? (
            <Table
              columns={hostColumns}
              dataSource={hosts}
              rowKey="id"
              loading={loading}
              pagination={false}
              locale={{
                emptyText: (
                  <Empty
                    description="该主机组下暂无主机"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                  />
                ),
              }}
            />
          ) : (
            <Empty
              description="请从左侧选择一个主机组"
              style={{ marginTop: 80 }}
            />
          )}
        </div>
      </div>

      {/* HostGroup Modal */}
      <Modal
        title={editingHg ? '编辑主机组' : '新建主机组'}
        open={hgModalOpen}
        onOk={editingHg ? handleUpdateHg : handleCreateHg}
        onCancel={() => {
          setHgModalOpen(false);
          hgForm.resetFields();
        }}
      >
        <Form form={hgForm} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Host Modal */}
      <Modal
        title={editingHost ? '编辑主机' : '新建主机'}
        open={hostModalOpen}
        onOk={editingHost ? handleUpdateHost : handleCreateHost}
        onCancel={() => {
          setHostModalOpen(false);
          hostForm.resetFields();
        }}
      >
        <Form form={hostForm} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item
            name="ip"
            label="IP"
            rules={[{ required: true, message: '请输入IP' }]}
          >
            <Input maxLength={45} placeholder="192.168.1.10" />
          </Form.Item>
          <Form.Item name="port" label="端口" initialValue={22}>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="ansibleUser" label="SSH用户">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="ansibleSshPass" label="SSH密码（加密存储）">
            <Input.Password maxLength={500} placeholder="留空则不更新" />
          </Form.Item>
          <Form.Item
            name="ansibleBecome"
            label="提权"
            valuePropName="checked"
            initialValue={false}
          >
            <Checkbox />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 3: Verify host group manager page**

Run: `cd frontend && npm run dev`
Expected: Left panel is white card with rounded corners, selected group has dark background (#1e293b) with white text. Right panel shows host count next to group name. IP addresses render in monospace font. Checkbox uses Ant Design Checkbox instead of native HTML. Both panels have subtle shadows.

- [ ] **Step 4: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/host/HostGroupManager.tsx frontend/src/pages/host/HostGroupManager.module.css
git commit -m "feat(frontend): redesign host group manager with dark selection and CSS Modules"
```

---

### Task 13: Final Verification

**Files:** None (verification only)

- [ ] **Step 1: Run full lint check**

Run: `cd frontend && npm run lint`
Expected: No errors.

- [ ] **Step 2: Run build to verify no compilation errors**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no errors.

- [ ] **Step 3: Run existing tests**

Run: `cd frontend && npm run test -- --run`
Expected: All existing tests pass (if any).

- [ ] **Step 4: Manual smoke test**

Open `http://localhost:5173` and verify:
1. Login page: split layout with brand panel + form
2. Register page: same layout, different title
3. Project list: card avatars, dark role tags, hover shadows, project count
4. Project sidebar: dark background, grouped navigation, breadcrumb
5. Member management: PageHeader, dark role tags
6. Role list: PageHeader, blue name links
7. Role detail: back button, no console deprecation warnings, polished empty states
8. Project settings: PageHeader, red danger zone
9. Host group manager: dark selection, monospace IPs, Ant Design Checkbox

- [ ] **Step 5: Commit any remaining fixes if needed**

If any issues were found during verification, fix and commit them.
