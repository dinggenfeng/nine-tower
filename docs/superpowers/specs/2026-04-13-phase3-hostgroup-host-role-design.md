# Phase 3: 主机组 + 主机 + Role 基础 CRUD — 设计文档

**日期：** 2026-04-13  
**状态：** 已确认，待实施

---

## 1. 范围

三个功能模块的后端+前端完整闭环：

1. **主机组（HostGroup）CRUD** — 项目级资源
2. **主机（Host）CRUD** — 主机组的子资源，含 AES-256 敏感字段加密
3. **Role 基础 CRUD** — 项目级资源，含详情页 Tab 框架占位

---

## 2. 后端设计

### 2.1 新增包

- `com.ansible.host/` — 主机组和主机
- `com.ansible.role/` — Ansible Role

### 2.2 实体

#### HostGroup（extends BaseEntity）

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | Long | 所属项目 |
| name | String(100) | 主机组名称 |
| description | String(500) | 描述 |
| createdBy | Long | 创建者（继承自 BaseEntity） |

#### Host（extends BaseEntity）

| 字段 | 类型 | 说明 |
|------|------|------|
| hostGroupId | Long | 所属主机组 |
| name | String(100) | 主机名称 |
| ip | String(45) | IP 地址 |
| port | Integer | SSH 端口，默认 22 |
| ansibleUser | String(100) | SSH 用户名 |
| ansibleSshPass | String(500) | SSH 密码（AES-256 加密存储） |
| ansibleSshPrivateKeyFile | String(2000) | SSH 私钥文件内容（AES-256 加密存储） |
| ansibleBecome | Boolean | 是否提权，默认 false |
| createdBy | Long | 创建者（继承自 BaseEntity） |

#### Role（extends BaseEntity）

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | Long | 所属项目 |
| name | String(100) | Role 名称 |
| description | String(500) | 描述 |
| createdBy | Long | 创建者（继承自 BaseEntity） |

### 2.3 敏感字段加密

- 新增 `com.ansible.common.EncryptionService`（Spring `@Component`）
- 算法：AES-256-GCM（认证加密，防篡改）
- AES 密钥通过 `application.yml` 配置项 `app.encryption.key` 读取（Base64 编码的 32 字节密钥）
- `encrypt(plaintext) → Base64(iv + ciphertext + tag)`
- `decrypt(encrypted) → plaintext`
- Host Service 层：写入时加密 `ansibleSshPass` 和 `ansibleSshPrivateKeyFile`，读取时解密后在 Response DTO 中显示 `****`
- 仅当用户明确请求编辑时，前端发送新值；空值或 `****` 表示不更新

### 2.4 权限模型

遵循设计文档 Section 4.2 数据级权限：

| 操作 | 数据创建者 | PROJECT_ADMIN | 其他成员 |
|------|-----------|--------------|---------|
| 查看 | ✓ | ✓ | ✓ |
| 创建 | ✓（项目成员即可） | ✓ | ✓ |
| 编辑 | ✓ | ✓ | ✗ |
| 删除 | ✓ | ✓ | ✗ |

**ProjectAccessChecker 扩展：**
- 新增 `checkOwnerOrAdmin(Long projectId, Long resourceCreatedBy, Long currentUserId)` — 校验当前用户是资源创建者或 PROJECT_ADMIN
- Host 的项目归属通过 HostGroup 反查 projectId

### 2.5 API

#### 主机组（遵循设计文档 Section 6.4）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/host-groups` | 创建主机组 |
| GET | `/api/projects/{projectId}/host-groups` | 主机组列表 |
| GET | `/api/host-groups/{id}` | 主机组详情 |
| PUT | `/api/host-groups/{id}` | 更新主机组 |
| DELETE | `/api/host-groups/{id}` | 删除主机组（级联删除其下所有主机） |

#### 主机（遵循设计文档 Section 6.4）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/host-groups/{hgId}/hosts` | 创建主机 |
| GET | `/api/host-groups/{hgId}/hosts` | 主机列表 |
| GET | `/api/hosts/{id}` | 主机详情（敏感字段脱敏） |
| PUT | `/api/hosts/{id}` | 更新主机 |
| DELETE | `/api/hosts/{id}` | 删除主机 |

#### Role（遵循设计文档 Section 6.5）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/roles` | 创建 Role |
| GET | `/api/projects/{projectId}/roles` | Role 列表 |
| GET | `/api/roles/{id}` | Role 详情 |
| PUT | `/api/roles/{id}` | 更新 Role |
| DELETE | `/api/roles/{id}` | 删除 Role |

### 2.6 DTO

#### HostGroup

- `CreateHostGroupRequest` — name(必填), description
- `UpdateHostGroupRequest` — name, description
- `HostGroupResponse` — id, projectId, name, description, createdBy, createdAt

#### Host

- `CreateHostRequest` — name(必填), ip(必填), port, ansibleUser, ansibleSshPass, ansibleSshPrivateKeyFile, ansibleBecome
- `UpdateHostRequest` — name, ip, port, ansibleUser, ansibleSshPass, ansibleSshPrivateKeyFile, ansibleBecome
- `HostResponse` — id, hostGroupId, name, ip, port, ansibleUser, ansibleSshPass(`****`), ansibleSshPrivateKeyFile(`****`), ansibleBecome, createdBy, createdAt

#### Role

- `CreateRoleRequest` — name(必填), description
- `UpdateRoleRequest` — name, description
- `RoleResponse` — id, projectId, name, description, createdBy, createdAt

---

## 3. 前端设计

### 3.1 页面

#### HostGroupManager（单页面 Master-Detail）

- **左侧面板：** 主机组列表
  - 顶部：「新建主机组」按钮
  - 列表项显示名称和描述，点击选中
  - 选中项高亮，右键或操作按钮支持编辑/删除
- **右侧面板：** 选中主机组的主机列表
  - 顶部：主机组名称 + 「新建主机」按钮
  - 表格展示主机列表（名称、IP、端口、用户名、是否提权）
  - 敏感字段显示 `****`
  - 支持编辑/删除操作

#### RoleList

- 表格列表展示项目下所有 Role（名称、描述、创建者、创建时间）
- 「新建 Role」按钮 + Modal 表单
- 点击 Role 名称导航到 RoleDetail
- 编辑/删除操作（创建者或 PROJECT_ADMIN 可见）

#### RoleDetail

- 顶部显示 Role 名称和描述，支持编辑
- 6 个 Tab：Tasks、Handlers、Templates、Files、Vars、Defaults
- 所有 Tab 内容显示「即将推出」占位文案
- 路由：`/projects/:id/roles/:roleId`

### 3.2 路由更新

在 ProjectLayout 下新增：

```
/projects/:id/host-groups → HostGroupManager
/projects/:id/roles → RoleList
/projects/:id/roles/:roleId → RoleDetail
```

### 3.3 新增文件

- `frontend/src/types/entity/Host.ts` — HostGroup, Host, 请求/响应类型
- `frontend/src/types/entity/Role.ts` — Role 类型
- `frontend/src/api/host.ts` — 主机组和主机 API
- `frontend/src/api/role.ts` — Role API
- `frontend/src/pages/host/HostGroupManager.tsx`
- `frontend/src/pages/role/RoleList.tsx`
- `frontend/src/pages/role/RoleDetail.tsx`

---

## 4. 测试策略

遵循设计文档 Section 10.1 细粒度迭代原则，每个功能单元闭环：

### 后端

- **HostGroupService 单元测试** — CRUD + 权限检查 mock
- **HostService 单元测试** — CRUD + 加密/脱敏逻辑 + 权限检查 mock
- **RoleService 单元测试** — CRUD + 权限检查 mock
- **EncryptionService 单元测试** — 加密/解密正确性
- **HostGroupController 集成测试** — 完整请求链路 + 权限验证
- **HostController 集成测试** — 含敏感字段脱敏验证
- **RoleController 集成测试** — 完整请求链路 + 权限验证

### 前端

- TypeScript 编译通过
- ESLint 无报错

---

## 5. 实施顺序

按功能单元逐个闭环：

1. EncryptionService + 单元测试
2. ProjectAccessChecker 扩展（checkOwnerOrAdmin）
3. HostGroup 后端全套（实体→Repository→DTO→Service+测试→Controller+测试）
4. Host 后端全套（含加密逻辑）
5. Role 后端全套
6. 代码质量扫描 + 修复
7. 前端 Host 类型 + API 层
8. 前端 Role 类型 + API 层
9. 前端 HostGroupManager 页面
10. 前端 RoleList + RoleDetail 页面
11. 路由更新 + 前端质量检查
12. i18n 中文化
