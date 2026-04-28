# 主机组/主机复制功能设计

日期: 2026-04-28

## 概述

为主机组管理和主机管理增加复制功能：
1. 主机组复制：一键复制主机组及其下所有主机
2. 主机复制：弹窗预填原主机信息，用户确认后保存

## 功能一：主机组复制

### 交互流程

- 主机组列表每个卡片操作区增加复制按钮（`CopyOutlined` 图标，与编辑/删除并列）
- 点击复制按钮直接触发复制（无需弹窗确认）
- 后端自动生成新名称 `{name} (副本)`，若已存在则追加数字
- 复制主机组的同时复制其下所有主机
- 不复制 HOSTGROUP 级别的 Variable
- 不复制 Playbook-HostGroup 关联
- 成功后刷新列表并自动选中新主机组

### 后端新增端点

```
POST /api/host-groups/{id}/copy
```

`HostGroupService.copyHostGroup(hostGroupId, currentUserId)`:
1. 查询源主机组，校验 `checkOwnerOrAdmin` 权限
2. 生成唯一名称：`{name} (副本)` → `{name} (副本2)` → `{name} (副本3)`...
3. 创建新 HostGroup（projectId 相同，createdBy = currentUserId）
4. 查询源主机组下所有 Host，逐条复制（hostGroupId 指向新记录，加密字段直接复用密文）
5. 返回新 HostGroupResponse

### 涉及文件

- `backend/src/main/java/com/ansible/host/controller/HostGroupController.java` — 新增 copy 端点
- `backend/src/main/java/com/ansible/host/service/HostGroupService.java` — 新增 copyHostGroup 方法
- `frontend/src/api/host.ts` — 新增 copyHostGroup API 函数
- `frontend/src/pages/host/HostGroupManager.tsx` — 添加复制按钮和 handleCopyHostGroup

## 功能二：主机复制

### 交互流程

- 主机表格每行操作列增加复制按钮
- 点击后打开创建弹窗，标题为「复制主机」
- 表单预填源主机的 name、ip、port、ansibleUser、ansibleBecome
- SSH 密码和私钥字段留空（后端从源主机复制加密值）
- 用户可修改任意字段后提交
- 若用户填写了新密码/私钥，则使用新值；否则复用源主机加密值

### 后端改动

复用现有 `POST /api/host-groups/{hgId}/hosts` 端点。

`CreateHostRequest` 新增可选字段：
```java
private Long copyFromHostId;
```

`HostService.createHost` 增强：
- 若 `copyFromHostId != null`，加载源主机实体
- 密码字段为空时，从源主机复制加密值；非空时加密新值
- 私钥字段同理

### 涉及文件

- `backend/src/main/java/com/ansible/host/dto/CreateHostRequest.java` — 新增 copyFromHostId 字段
- `backend/src/main/java/com/ansible/host/service/HostService.java` — createHost 增加复制逻辑
- `frontend/src/types/entity/Host.ts` — CreateHostRequest 新增 copyFromHostId
- `frontend/src/pages/host/HostGroupManager.tsx` — 添加复制按钮和 handleCopyHost

## 边界情况

| 场景 | 处理 |
|------|------|
| 主机组名称 `{name} (副本N)` 全部被占用 | 极端情况返回 409 Conflict |
| 主机组下无主机 | 只复制主机组本身，不报错 |
| 复制主机时源主机不存在 | 忽略 copyFromHostId，正常创建 |
| 复制主机时密码字段填了空格 | 视为空，从源主机复制 |
| 项目权限不足 | 后端返回 403 |
