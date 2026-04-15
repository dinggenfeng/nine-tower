# Ansible 模块模板选择与动态参数表单设计

## 概述

将 Task 和 Handler 创建/编辑表单中的 `module` 文本输入升级为下拉选择器，选中模块后动态渲染该模块的常用参数表单，支持通过 key-value 形式添加不常用参数。

首批支持三个模块：`copy`、`template`、`file`。

## 设计决策

- **纯前端实现**：模块元数据以 TypeScript 常量维护在前端，不涉及后端改动。Ansible 模块定义本质是静态的（跟随 Ansible 版本），前端维护合理。
- **后端零改动**：`module` 字段继续存字符串，`args` 字段继续存 JSON text。
- **Task 和 Handler 共用**：模块选择组件和动态表单抽取为共享组件。

## 模块元数据结构

新建 `frontend/src/constants/ansibleModules.ts`：

```typescript
interface ModuleParam {
  name: string;           // 参数名，如 "src"
  label: string;          // 显示标签，如 "源文件路径"
  type: 'input' | 'select' | 'switch' | 'number';
  required: boolean;
  placeholder?: string;
  tooltip?: string;       // 参数说明
  options?: { label: string; value: string }[];  // select 类型用
  defaultValue?: string | boolean | number;
}

interface ModuleDefinition {
  name: string;           // 如 "copy"
  label: string;          // 如 "Copy"
  description: string;    // 简短描述，下拉框中显示
  docUrl: string;         // Ansible 官方文档链接
  params: ModuleParam[];  // 常用参数列表
  validate?: (values: Record<string, any>) => Record<string, string>;
}
```

## 模块定义

### copy — 复制文件到远程主机

常用参数：
- `src`（Input，条件必填 — 与 content 二选一）
- `content`（Input，条件必填 — 与 src 二选一）
- `dest`（Input，必填）
- `owner`（Input，选填）
- `group`（Input，选填）
- `mode`（Input，选填，placeholder: "0644"）
- `backup`（Switch，默认 false）
- `remote_src`（Switch，默认 false）

自定义校验：src 和 content 至少填写一个。

文档：https://docs.ansible.com/ansible/latest/collections/ansible/builtin/copy_module.html

### template — 渲染 Jinja2 模板到远程主机

常用参数：
- `src`（Input，必填）
- `dest`（Input，必填）
- `owner`（Input，选填）
- `group`（Input，选填）
- `mode`（Input，选填，placeholder: "0644"）
- `backup`（Switch，默认 false）

文档：https://docs.ansible.com/ansible/latest/collections/ansible/builtin/template_module.html

### file — 管理文件/目录属性

常用参数：
- `path`（Input，必填）
- `state`（Select，选填，选项：file / directory / link / hard / touch / absent）
- `owner`（Input，选填）
- `group`（Input，选填）
- `mode`（Input，选填，placeholder: "0644"）
- `src`（Input，选填，state=link/hard 时使用）
- `recurse`（Switch，默认 false）

文档：https://docs.ansible.com/ansible/latest/collections/ansible/builtin/file_module.html

## 共享组件

### ModuleSelect — 模块下拉选择器

位置：`frontend/src/components/role/ModuleSelect.tsx`

- 下拉项：左边模块名，右边简短描述
- 鼠标悬浮到某项时，显示"查看文档"图标按钮，点击 `window.open` 跳转 Ansible 官方文档
- 支持用户手动输入不在列表中的模块名（兼容自定义模块）

### ModuleParamsForm — 动态参数表单

位置：`frontend/src/components/role/ModuleParamsForm.tsx`

- 根据选中的模块渲染常用参数字段（Input / Select / Switch 等）
- 底部"添加参数"按钮，添加 key-value 行用于不常用参数
- 若选中的模块不在预置列表中，只显示 key-value 额外参数区域
- 自定义校验规则（如 copy 的 src/content 二选一）

## 数据流

### 提交

```
用户选择模块 → 填写常用参数 + 额外参数
                    ↓
提交时合并为一个 JSON 对象 → 写入 args 字段
                    ↓
后端照常存储 module(string) + args(JSON text)
```

### 编辑回显

```
读取 args JSON → 匹配到常用参数名的 → 填入对应表单字段
               → 未匹配的参数名 → 填入额外参数 key-value 列表
```

## 交互细节

- 切换模块时，清空之前的参数值并重新渲染表单
- 编辑已有 Task/Handler 时，先根据 module 值匹配模块定义，再解析 args 回显
- 校验失败时高亮对应字段，如 copy 模块 src 和 content 都为空时提示"src 和 content 至少填一个"

## 改动范围

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/constants/ansibleModules.ts` | 新建 — 模块元数据定义 |
| `frontend/src/components/role/ModuleSelect.tsx` | 新建 — 模块下拉组件 |
| `frontend/src/components/role/ModuleParamsForm.tsx` | 新建 — 动态参数表单组件 |
| `frontend/src/pages/role/RoleTasks.tsx` | 改动 — 集成新组件替换原 module Input 和 args TextArea |
| `frontend/src/pages/role/RoleHandlers.tsx` | 改动 — 同上 |
| 后端 | 无改动 |
