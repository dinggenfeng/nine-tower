# Task/Handler 复制 + Loop 批量添加设计

日期: 2026-04-24

## 概述

两个独立的前端功能增强：
1. Task 和 Handler 列表增加复制功能
2. Loop 列表模式增加批量添加入口

## 功能一：Task/Handler 复制

### 交互流程

- Task/Handler 列表每行操作栏增加"复制"按钮（`CopyOutlined` 图标）
- 点击后打开创建弹窗，自动填入原记录的所有字段
- 弹窗标题显示"复制 Task"/"复制 Handler"
- 名称字段加 ` (副本)` 后缀
- Task 的 `taskOrder` 设为原值 +1
- 用户修改后点保存，调用 `createTask`/`createHandler` 接口创建新记录
- 不复制 tag 关联，用户需手动重新选择

### 纯前端操作

- 后端无需新增接口，复用现有 create 接口
- 复制 = 打开预填数据的创建弹窗

### 涉及文件

- `frontend/src/pages/role/RoleTasks.tsx` — 添加 handleCopy 函数
- `frontend/src/pages/role/RoleHandlers.tsx` — 添加 handleCopy 函数

## 功能二：Loop 列表批量添加

### 交互流程

- 保留现有 `<Select mode="tags">` 输入方式
- 在 tags 选择器右侧增加"批量添加"按钮（仅列表模式显示）
- 点击弹出小 Modal，包含 `<Input.TextArea>`
- Modal 提示文字："每行一个列表项，空行将被忽略"
- 用户输入多行文本后点"确定"：
  - 按换行拆分
  - 过滤空行和首尾空格
  - 追加到现有 loopItems
  - 自动去重
- 已有的单项输入和删除行为不变

### 涉及文件

- `frontend/src/pages/role/RoleTasks.tsx` — 主任务 loop 区域增加批量添加
- `frontend/src/components/role/BlockTasksEditor.tsx` — block 子任务 loop 区域同步增加
