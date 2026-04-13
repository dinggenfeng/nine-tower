# UI 优化设计文档

## 概述

将当前基于 Ant Design 默认样式的基础 UI 升级为专业克制的 DevOps 工具风格（类似 GitLab / Grafana）。通过 Ant Design 5 的 ConfigProvider theme token 系统建立全局设计语言，结合 CSS Modules 为各页面补充布局和特殊样式。

**风格关键词：** 深色侧边栏、蓝灰配色、低饱和度、小圆角、紧凑间距、信息密度高。

## 全局主题 (theme.ts)

新建 `frontend/src/theme.ts`，集中管理 Ant Design design tokens，通过 `ConfigProvider` 注入。

### Token 定义

| Token | 值 | 说明 |
|-------|------|------|
| colorPrimary | `#3b82f6` | 蓝色主色调 |
| colorBgContainer | `#ffffff` | 内容区容器白底 |
| colorBgLayout | `#f1f5f9` | 页面背景浅灰 |
| borderRadius | `6` | 统一圆角 |
| fontSize | `14` | 基础字号 |
| colorText | `#1e293b` | 主文字色 |
| colorTextSecondary | `#64748b` | 次要文字色 |
| colorBorder | `#e2e8f0` | 边框色 |
| colorSuccess | `#10b981` | 成功色 |
| colorWarning | `#f59e0b` | 警告色 |
| colorError | `#ef4444` | 错误色 |

### 额外颜色常量（非 Ant Design token，供 CSS Modules 使用）

| 变量名 | 值 | 说明 |
|--------|------|------|
| headerBg | `#0f172a` | Header 深色背景 |
| siderBg | `#1e293b` | 侧边栏深色背景 |
| siderItemActiveBg | `#3b82f6` | 侧边栏选中项背景 |
| siderTextColor | `#94a3b8` | 侧边栏文字色 |
| siderTextActiveColor | `#f1f5f9` | 侧边栏选中文字色 |
| siderGroupLabel | `#475569` | 侧边栏分组标签色 |
| tagAdminBg | `#1e3a5f` | 管理员标签背景 |
| tagAdminColor | `#3b82f6` | 管理员标签文字 |
| tagMemberBg | `#1e293b` | 成员标签背景 |
| tagMemberColor | `#94a3b8` | 成员标签文字 |

## 布局改造

### MainLayout（全局顶栏）

**当前：** 白色 Header，简单的 logo 文字 + 用户名下拉。

**优化后：**
- Header 背景色改为 `#0f172a`（深色）
- Logo 文字改为 "Ansible Playbook Studio"，白色字体
- 右侧显示当前项目名（pill 标签样式，`#1e293b` 背景）+ 用户头像下拉
- 新增 CSS Module 文件 `MainLayout.module.css`

### ProjectLayout（项目侧边栏）

**当前：** 白色侧边栏，200px 宽，Ant Design Menu 默认样式。

**优化后：**
- 侧边栏背景色改为 `#1e293b`（深色）
- 导航菜单分为两组：
  - **功能区**（标签 "导航"）：Roles、Host Groups、Variables、Environments、Tags、Playbooks
  - **管理区**（标签 "管理"）：Members、Settings
- 分组标签：`#475569`，大写，letter-spacing: 1px
- 选中项：`#3b82f6` 背景，左侧圆角 0 右侧 4px，白色文字
- 未选中项：`#94a3b8` 文字色
- 内容区添加面包屑导航（项目名 / 当前页面）
- 内容区背景 `#f1f5f9`，内部内容包裹在白色卡片（圆角 8px + 微阴影 `0 1px 3px rgba(0,0,0,0.06)`）
- 新增 CSS Module 文件 `ProjectLayout.module.css`

## 页面改造

### 登录/注册页

**当前：** 灰色背景居中白色卡片 400px。

**优化后：**
- 改为左右分栏布局（左右各 50%）
- **左侧品牌区：**
  - 背景：`linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)`
  - 装饰：斜线纹理（45deg repeating-linear-gradient，opacity 0.05）
  - 内容：大标题 "Playbook Studio"、副标题 "Ansible" 小字（蓝色）、描述文字、功能标签（项目管理/主机管理等）
- **右侧表单区：**
  - 白色背景，垂直居中
  - 标题 "欢迎回来" / "创建账号"（登录 / 注册区分）
  - 副标题 "登录以继续" / "注册新账号"
  - 输入框：`#f8fafc` 背景、`#e2e8f0` 边框、圆角 6px
  - 主按钮：`#3b82f6` 背景、圆角 6px
- 响应式：窄屏（<768px）隐藏左侧品牌区，表单全宽
- 新增 CSS Module 文件 `Login.module.css`（注册页复用）

### 项目列表页

**当前：** 标题 + 新建按钮 + Ant Design List 卡片网格。

**优化后：**
- 页面标题下方添加项目总数（"共 N 个项目"，`#64748b` 色）
- 项目卡片改造：
  - 左上角：项目名首字母头像（28x28px，`#1e293b` 背景，白色字体，圆角 6px）
  - 右上角：角色标签（管理员 `#1e3a5f` 底 `#3b82f6` 字；成员 `#1e293b` 底 `#94a3b8` 字）
  - 中部：项目名（font-weight: 600）+ 描述
  - 底部分隔线 + 资源统计（主机数、角色数），`#94a3b8` 色。若当前 API 不返回统计数据，先不显示底部统计行，后续 API 支持后再添加
  - 卡片样式：圆角 8px、1px `#e2e8f0` 边框、微阴影、悬停阴影增强（transition 0.2s）
- 卡片操作（设置/删除）移到 hover 时显示的操作菜单
- 空状态：居中引导文案 + 新建按钮

### 成员管理页

**当前：** h2 标题 + 添加按钮 + Ant Design Table。

**优化后：**
- 使用 PageHeader 组件（见下方公共组件）
- 表格包裹在白色卡片中（内容区已有卡片背景，无需额外包裹）
- 角色列使用深色标签（同项目列表的标签样式）
- 操作列精简：管理员看到下拉角色选择 + 移除按钮

### Role 列表页

**当前：** 标题 + 表格 + 创建/编辑弹窗。

**优化后：**
- 使用 PageHeader 组件
- 表格样式与成员管理一致
- Role 名称可点击，蓝色链接样式

### Role 详情页

**当前：** Card + 废弃的 TabPane API。

**优化后：**
- 使用 Tabs 的 `items` prop 替代废弃的 TabPane
- 页面头部：Role 名称 + 描述 + 返回按钮
- Tab 内容区的 "即将推出" 占位符改为更美观的空状态组件（图标 + 文字）

### 主机组管理页

**当前：** flex 双面板，左侧 List + 右侧 Table，基础样式。

**优化后：**
- 左侧面板：
  - 白色卡片 + 圆角 8px + 微阴影
  - 头部：标题 "主机组" + 蓝色 "+" 按钮（圆角方块）
  - 列表项：显示名称 + 主机数量（次要文字）
  - 选中态：`#1e293b` 深色背景、白色文字、圆角 6px
  - 每项末尾操作菜单（⋯）
- 右侧面板：
  - 白色卡片 + 圆角 8px + 微阴影
  - 头部：组名 + 主机数 + "添加主机" 按钮
  - 表格表头 `#f8fafc` 背景
  - IP 地址使用 `font-family: monospace`
- 修复：将 HTML 原生 checkbox 替换为 Ant Design Checkbox 组件
- 新增 CSS Module 文件 `HostGroupManager.module.css`

### 项目设置页

**当前：** h2 标题 + Card(max-width:600px) + 危险区域 Card。

**优化后：**
- 使用 PageHeader 组件
- 表单区域保持 max-width: 600px
- 危险区域：红色边框（`#ef4444`）左边框 3px，浅红背景 `#fef2f2`

## 公共组件

### PageHeader 组件

提取统一的页面头部组件，所有列表/管理页面复用：

```
Props:
- title: string          // 页面标题
- description?: string   // 页面描述
- action?: ReactNode     // 右侧操作按钮
```

布局：左侧标题+描述、右侧操作按钮、底部分隔线。

文件位置：`frontend/src/components/PageHeader.tsx`

## 技术实现

### 文件变更清单

**新增文件：**
- `frontend/src/theme.ts` — Ant Design theme tokens + 颜色常量
- `frontend/src/components/PageHeader.tsx` — 公共页面头部组件
- `frontend/src/components/Layout/MainLayout.module.css`
- `frontend/src/components/Layout/ProjectLayout.module.css`
- `frontend/src/pages/auth/Login.module.css`
- `frontend/src/pages/host/HostGroupManager.module.css`

**修改文件：**
- `frontend/src/main.tsx` — 引入 ConfigProvider + theme
- `frontend/src/components/Layout/MainLayout.tsx` — 深色 Header + 样式改造
- `frontend/src/components/Layout/ProjectLayout.tsx` — 深色侧边栏 + 分组导航 + 面包屑
- `frontend/src/pages/auth/Login.tsx` — 左右分栏布局
- `frontend/src/pages/auth/Register.tsx` — 复用登录页布局
- `frontend/src/pages/project/ProjectList.tsx` — 卡片改造 + 首字母头像 + 资源统计
- `frontend/src/pages/project/ProjectSettings.tsx` — PageHeader + 危险区域样式
- `frontend/src/pages/project/MemberManagement.tsx` — PageHeader + 标签样式
- `frontend/src/pages/role/RoleList.tsx` — PageHeader + 表格样式
- `frontend/src/pages/role/RoleDetail.tsx` — 修复废弃 API + 空状态改进
- `frontend/src/pages/host/HostGroupManager.tsx` — 双面板视觉改造 + 修复 checkbox

### CSS Modules 命名约定

- 文件名与组件同名：`ComponentName.module.css`
- 类名使用 camelCase：`.headerWrapper`、`.siderMenu`、`.brandSection`

### 不引入新依赖

所有改造基于现有 Ant Design 5 + CSS Modules（Vite 内置支持），不新增任何 npm 包。
