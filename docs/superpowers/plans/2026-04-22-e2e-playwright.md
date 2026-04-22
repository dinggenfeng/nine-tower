# E2E 测试计划 — Playwright

## Context

项目所有功能模块已开发完成，前端有 75 个单元/组件测试，后端有完整的单元+集成测试覆盖。但没有端到端测试——每次发版需要人工点一遍全部模块确认没有回归。本计划引入 Playwright，编写约 18 条 E2E 测试覆盖所有模块的 CRUD 主流程 + 关键跨模块协作场景，替代手动回归测试。

---

## 1. 技术选型

- **框架**: Playwright (TypeScript)
- **安装位置**: `frontend/` 子目录（与 Vitest 共享 node_modules）
- **浏览器**: Chromium only（内部工具，不需要跨浏览器）
- **执行方式**: 手动触发 `npx playwright test`，不接入 CI

## 2. 目录结构

```
frontend/
├── playwright.config.ts          # Playwright 配置
├── e2e/
│   ├── fixtures.ts               # 共享 helper：登录、建项目、API 清理
│   ├── auth.spec.ts              # 注册/登录/登出
│   ├── project.spec.ts           # 项目 CRUD + 成员管理
│   ├── host.spec.ts              # 主机组 + 主机 CRUD
│   ├── role.spec.ts              # Role CRUD
│   ├── task.spec.ts              # Task + Handler CRUD + notify 关联
│   ├── template.spec.ts          # Template 创建/编辑/下载
│   ├── file.spec.ts              # File 目录/上传/下载
│   ├── variable.spec.ts          # 四级变量 CRUD + 优先级标记
│   ├── environment.spec.ts       # 环境 + EnvConfig CRUD
│   ├── tag.spec.ts               # Tag CRUD + Task 关联
│   ├── playbook.spec.ts          # Playbook CRUD + 组装 + YAML 导出
│   └── integration.spec.ts       # 端到端协作 + 权限场景
```

## 3. 前置条件（测试启动脚本）

E2E 测试需要后端 + 数据库运行。在 `playwright.config.ts` 中配置 `webServer`：

```ts
// 不自动启动后端，假设已手动启动
// webServer: { command: '...', reuseExistingServer: true }
```

测试前手动执行：
1. `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
2. `cd frontend && npm run dev`

## 4. 测试用例清单（18 条）

### 第一层：模块 CRUD 主流程（15 条）

#### 4.1 认证 (`auth.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 1 | 注册 → 登录 → 登出 | 注册新用户 → 跳转登录 → 输入账密 → 登录 → 点登出 | 登录后跳转 /projects；登出后跳转 /login |

#### 4.2 项目 + 成员 (`project.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 2 | 项目 CRUD | 新建项目 → 编辑名称 → 删除 | 列表出现/消失项目 |
| 3 | 成员管理 | 添加第二个用户为 MEMBER → 改角色为 ADMIN → 移除 | 成员列表正确显示角色变更 |

#### 4.3 主机组 + 主机 (`host.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 4 | 主机组 + 主机 CRUD | 建主机组 → 编辑 → 添加主机(含 SSH 密码) → 编辑主机 → 删除主机 → 删除主机组 | SSH 密码字段显示 `****` |

#### 4.4 Role (`role.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 5 | Role CRUD | 建 Role → 编辑名称 → 删除 | 列表正确 |

#### 4.5 Task + Handler (`task.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 6 | Task CRUD + 排序 | 建两个 Task → 拖拽排序 → 编辑 → 删除 | 列表顺序正确 |
| 7 | Handler CRUD + notify | 建 Handler → 编辑 Task 的 notify 字段选择该 Handler → 查看 Handler 的 "notified by" 列表 | Handler 页面显示关联的 Task |
| 8 | Block 任务 | 建 block 类型 Task → 在 block/rescue/always 各加一个子任务 | YAML 预览包含 block/rescue/always 结构 |

#### 4.6 Template (`template.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 9 | Template CRUD | 建目录 → 建 .j2 文件 → 编辑内容 → 下载 → 删除 | 下载内容与编辑一致 |

#### 4.7 File (`file.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 10 | File CRUD | 建目录 → 上传文件 → 下载 → 删除 | 目录树正确展示 |

#### 4.8 Variable (`variable.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 11 | 四级变量 CRUD | 分别建项目级、主机组级、环境级变量（同 key） | 树形视图显示优先级标记（胜出 scope 高亮） |

#### 4.9 Environment (`environment.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 12 | Environment + EnvConfig CRUD | 建环境 → 添加配置项 → 编辑配置项 → 删除配置项 → 删除环境 | 配置项列表正确 |

#### 4.10 Tag (`tag.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 13 | Tag CRUD | 建标签 → 编辑 → 删除 | 列表正确 |
| 14 | Task 关联标签 | 建标签 → 在 Task 编辑器中关联该标签 → 保存 | Task 的标签下拉显示已选标签 |

#### 4.11 Playbook (`playbook.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 15 | Playbook CRUD | 建剧本 → 编辑 → 删除 | 列表正确 |

### 第二层：端到端协作场景（3 条）

#### 4.12 完整 Playbook 构建 (`integration.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 16 | 全链路组装 | 建主机组+主机 → 建 Role+Task+Handler → 建 Tag → 建环境+EnvConfig → 建四级变量 → 组装 Playbook（选主机组/环境/Role/Tag） → 导出 YAML | YAML 包含 hosts、roles、vars、tags，变量按优先级合并 |

#### 4.13 权限隔离 (`integration.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 17 | MEMBER 不能改他人数据 | Alice 建 Playbook → 加 Bob 为 MEMBER → Bob 打开 Playbook → 尝试添加 Role | 操作失败（按钮不可见或 403 错误提示） |

#### 4.14 完整 YAML 生成 (`integration.spec.ts`)
| # | 用例 | 步骤 | 验证 |
|---|------|------|------|
| 18 | 带变量的 YAML | 建带 extraVars 的 Playbook → 关联含变量的 Role → 导出 YAML | YAML 中 extraVars 覆盖低优先级同名变量 |

## 5. 实施步骤

### Step 1: 框架搭建
- 安装 Playwright: `npm install -D @playwright/test && npx playwright install chromium`
- 创建 `playwright.config.ts`（baseURL: `http://localhost:5173`，timeout 30s，retries: 0）
- 创建 `e2e/fixtures.ts`：封装登录 helper、API 清理 helper

### Step 2: 基础模块测试（15 条）
按用例编号 1-15 顺序实现，每条用例独立（通过 fixtures 的清理机制保证测试隔离）。

### Step 3: 集成场景测试（3 条）
用例 16-18 依赖多个模块数据，放在最后实现。

### Step 4: 验证
- 手动启动后端 + 前端
- 执行 `npx playwright test`，确认全部通过
- 执行 `npx playwright test --ui`，可视化确认测试过程

## 6. 关键文件

- `frontend/playwright.config.ts` — 新建
- `frontend/e2e/fixtures.ts` — 新建，共享 helper
- `frontend/e2e/*.spec.ts` — 新建，12 个测试文件
- `frontend/package.json` — 添加 `@playwright/test` 依赖和 `test:e2e` script

## 7. 验证方式

```bash
# 终端 1：启动后端
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2：启动前端
cd frontend && npm run dev

# 终端 3：跑 E2E
cd frontend && npx playwright test
```

预期结果：18 条测试全部通过，耗时约 2-3 分钟。
