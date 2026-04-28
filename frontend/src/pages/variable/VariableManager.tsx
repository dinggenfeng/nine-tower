import { useEffect, useState, useCallback, useMemo } from "react";
import {
  Button,
  Table,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Space,
  Empty,
  Segmented,
  Tree,
  Card,
  Typography,
  Tag,
  Tooltip,
} from "antd";
import {
  PlusOutlined,
  TableOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ScanOutlined,
  CopyOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import { useParams } from "react-router-dom";
import type {
  Variable,
  VariableScope,
  CreateVariableRequest,
  DetectedVariableRow,
  BatchVariableSaveItem,
} from "../../types/entity/Variable";
import type { ReactNode } from "react";
import {
  listVariables,
  createVariable,
  updateVariable,
  deleteVariable,
  detectVariables,
  batchSaveVariables,
} from "../../api/variable";
import { listEnvironments } from "../../api/environment";
import { getHostGroups } from "../../api/host";
import { getRoles } from "../../api/role";
import { getRoleVariables, getRoleDefaults } from "../../api/roleVariable";
import type { Environment } from "../../types/entity/Environment";
import type { HostGroup } from "../../types/entity/Host";
import type { Role } from "../../types/entity/Role";
import type { RoleVariable, RoleDefaultVariable } from "../../types/entity/RoleVariable";
import { resolveVariablePriority, type VariableScopeKind } from "../../utils/variablePriority";

const scopeLabels: Record<VariableScope, string> = {
  PROJECT: "项目级",
  HOSTGROUP: "主机组级",
  ENVIRONMENT: "环境级",
};

type ViewMode = "table" | "tree";

interface TreeVariableNode {
  key: string;
  title: ReactNode;
  selectable?: boolean;
  children?: TreeVariableNode[];
}

export default function VariableManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [variables, setVariables] = useState<Variable[]>([]);
  const [loading, setLoading] = useState(false);
  const [scope, setScope] = useState<VariableScope>("PROJECT");
  const [scopeId, setScopeId] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVar, setEditingVar] = useState<Variable | null>(null);
  const [saving, setSaving] = useState(false);
  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [form] = Form.useForm();

  // Tree view state
  const [viewMode, setViewMode] = useState<ViewMode>("table");
  const [treeData, setTreeData] = useState<TreeVariableNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);

  // Variable detection state
  const [detecting, setDetecting] = useState(false);
  const [detectedVars, setDetectedVars] = useState<DetectedVariableRow[]>([]);
  const [savingVars, setSavingVars] = useState(false);

  interface CopyModalState {
    open: boolean;
    sourceVar: DetectedVariableRow | null;
    targetType: "project" | "role";
    targetRoleId?: number;
  }
  const [copyModal, setCopyModal] = useState<CopyModalState>({
    open: false,
    sourceVar: null,
    targetType: "role",
  });

  useEffect(() => {
    if (!pid) return;
    getHostGroups(pid).then(setHostGroups);
    listEnvironments(pid).then(setEnvironments);
  }, [pid]);

  const fetchVariables = useCallback(async () => {
    if (!pid) return;
    setLoading(true);
    try {
      // For PROJECT scope, pass pid as scopeId to filter by project
      const data = await listVariables(pid, scope, scope === "PROJECT" ? pid : scopeId);
      setVariables(data);
    } finally {
      setLoading(false);
    }
  }, [pid, scope, scopeId]);

  useEffect(() => {
    if (viewMode === "table") {
      fetchVariables();
    }
  }, [fetchVariables, viewMode]);

  // Build tree data by fetching variables across all scopes
  const fetchTreeData = useCallback(async () => {
    if (!pid) return;
    setTreeLoading(true);
    try {
      const [projectVars, hostgroupVars, envVars, roles] = await Promise.all([
        listVariables(pid, "PROJECT").catch(() => [] as Variable[]),
        listVariables(pid, "HOSTGROUP").catch(() => [] as Variable[]),
        listVariables(pid, "ENVIRONMENT").catch(() => [] as Variable[]),
        getRoles(pid).catch(() => [] as Role[]),
      ]);

      // Fetch role variables and defaults for all roles
      const roleVarPromises = roles.map((role) =>
        getRoleVariables(role.id)
          .then((vars) => ({ roleId: role.id, vars }))
          .catch(() => ({ roleId: role.id, vars: [] as RoleVariable[] }))
      );
      const roleDefaultPromises = roles.map((role) =>
        getRoleDefaults(role.id)
          .then((defaults) => ({ roleId: role.id, defaults }))
          .catch(() => ({ roleId: role.id, defaults: [] as RoleDefaultVariable[] }))
      );

      const [roleVarResults, roleDefaultResults] = await Promise.all([
        Promise.all(roleVarPromises),
        Promise.all(roleDefaultPromises),
      ]);

      const roleVarsMap = new Map<number, RoleVariable[]>();
      roleVarResults.forEach((r) => roleVarsMap.set(r.roleId, r.vars));

      const roleDefaultsMap = new Map<number, RoleDefaultVariable[]>();
      roleDefaultResults.forEach((r) => roleDefaultsMap.set(r.roleId, r.defaults));

      const { duplicateKeys, winningScope } = resolveVariablePriority({
        projectVars,
        hostgroupVars,
        environmentVars: envVars,
        roleVars: roleVarResults.flatMap((r) => r.vars),
        roleDefaults: roleDefaultResults.flatMap((r) => r.defaults),
      });

      const scopeNameMap: Record<VariableScopeKind, string> = {
        ENVIRONMENT: "环境级",
        HOSTGROUP: "主机组级",
        PROJECT: "项目级",
        ROLE_VARS: "Role 级",
        ROLE_DEFAULTS: "Role 默认",
      };

      const buildTitle = (
        key: string,
        value: string,
        currentScope: VariableScopeKind
      ): React.ReactNode => {
        if (!duplicateKeys.has(key)) return `${key} = ${value}`;
        const winner = winningScope.get(key);
        if (winner === currentScope) {
          return (
            <span>
              {key} = {value}{" "}
              <Typography.Text type="success" style={{ fontSize: 12 }}>
                <CheckCircleOutlined /> 生效中
              </Typography.Text>
            </span>
          );
        }
        return (
          <span>
            {key} = {value}{" "}
            <Typography.Text type="warning" style={{ fontSize: 12 }}>
              <WarningOutlined /> 被{scopeNameMap[winner!]}覆盖
            </Typography.Text>
          </span>
        );
      };

      // Group hostgroup vars by scopeId
      const hostGroupVarsMap = new Map<number, Variable[]>();
      hostgroupVars.forEach((v) => {
        const list = hostGroupVarsMap.get(v.scopeId) || [];
        list.push(v);
        hostGroupVarsMap.set(v.scopeId, list);
      });

      // Group environment vars by scopeId
      const envVarsMap = new Map<number, Variable[]>();
      envVars.forEach((v) => {
        const list = envVarsMap.get(v.scopeId) || [];
        list.push(v);
        envVarsMap.set(v.scopeId, list);
      });

      // Resolve names
      const hostGroupNameMap = new Map(hostGroups.map((h) => [h.id, h.name]));
      const envNameMap = new Map(environments.map((e) => [e.id, e.name]));

      const nodes: TreeVariableNode[] = [];

      // Environment scope (highest priority)
      const envChildren: TreeVariableNode[] = [];
      envVarsMap.forEach((vars, envScopeId) => {
        const envName = envNameMap.get(envScopeId) || `环境 #${envScopeId}`;
        envChildren.push({
          key: `env-${envScopeId}`,
          title: `${envName}/ (${vars.length})`,
          children: vars.map((v) => ({
            key: `env-var-${v.id}`,
            title: buildTitle(v.key, v.value, "ENVIRONMENT"),
          })),
        });
      });
      nodes.push({
        key: "scope-ENVIRONMENT",
        title: `环境级变量 (${envVars.length})`,
        children:
          envChildren.length > 0
            ? envChildren
            : [{ key: "env-empty", title: "(无)", selectable: false }],
      });

      // HostGroup scope
      const hgChildren: TreeVariableNode[] = [];
      hostGroupVarsMap.forEach((vars, hgScopeId) => {
        const hgName = hostGroupNameMap.get(hgScopeId) || `主机组 #${hgScopeId}`;
        hgChildren.push({
          key: `hg-${hgScopeId}`,
          title: `${hgName}/ (${vars.length})`,
          children: vars.map((v) => ({
            key: `hg-var-${v.id}`,
            title: buildTitle(v.key, v.value, "HOSTGROUP"),
          })),
        });
      });
      nodes.push({
        key: "scope-HOSTGROUP",
        title: `主机组级变量 (${hostgroupVars.length})`,
        children:
          hgChildren.length > 0
            ? hgChildren
            : [{ key: "hg-empty", title: "(无)", selectable: false }],
      });

      // Project scope
      const projectChildren: TreeVariableNode[] = projectVars.map((v) => ({
        key: `project-var-${v.id}`,
        title: buildTitle(v.key, v.value, "PROJECT"),
      }));
      nodes.push({
        key: "scope-PROJECT",
        title: `项目级变量 (${projectVars.length})`,
        children:
          projectChildren.length > 0
            ? projectChildren
            : [{ key: "project-empty", title: "(无)", selectable: false }],
      });

      // Role vars
      const roleVarsChildren: TreeVariableNode[] = [];
      roles.forEach((role) => {
        const vars = roleVarsMap.get(role.id) || [];
        if (vars.length > 0) {
          roleVarsChildren.push({
            key: `role-vars-${role.id}`,
            title: `${role.name}/ (${vars.length})`,
            children: vars.map((v) => ({
              key: `role-var-${v.id}`,
              title: buildTitle(v.key, v.value, "ROLE_VARS"),
            })),
          });
        }
      });
      const totalRoleVars = roleVarResults.reduce((sum, r) => sum + r.vars.length, 0);
      nodes.push({
        key: "scope-ROLE_VARS",
        title: `Role 级变量 (${totalRoleVars})`,
        children:
          roleVarsChildren.length > 0
            ? roleVarsChildren
            : [{ key: "role-vars-empty", title: "(无)", selectable: false }],
      });

      // Role defaults
      const roleDefaultsChildren: TreeVariableNode[] = [];
      roles.forEach((role) => {
        const defaults = roleDefaultsMap.get(role.id) || [];
        if (defaults.length > 0) {
          roleDefaultsChildren.push({
            key: `role-defaults-${role.id}`,
            title: `${role.name}/ (${defaults.length})`,
            children: defaults.map((d) => ({
              key: `role-default-${d.id}`,
              title: buildTitle(d.key, d.value, "ROLE_DEFAULTS"),
            })),
          });
        }
      });
      const totalRoleDefaults = roleDefaultResults.reduce((sum, r) => sum + r.defaults.length, 0);
      nodes.push({
        key: "scope-ROLE_DEFAULTS",
        title: `Role 默认变量 (${totalRoleDefaults})`,
        children:
          roleDefaultsChildren.length > 0
            ? roleDefaultsChildren
            : [
                {
                  key: "role-defaults-empty",
                  title: "(无)",
                  selectable: false,
                },
              ],
      });

      setTreeData(nodes);
    } finally {
      setTreeLoading(false);
    }
  }, [pid, hostGroups, environments]);

  useEffect(() => {
    if (viewMode === "tree") {
      fetchTreeData();
    }
  }, [viewMode, fetchTreeData]);

  const handleScopeChange = (newScope: VariableScope) => {
    setScope(newScope);
    setScopeId(undefined);
  };

  const handleCreate = () => {
    setEditingVar(null);
    form.resetFields();
    form.setFieldsValue({ scope, scopeId });
    setModalOpen(true);
  };

  const handleEdit = (v: Variable) => {
    setEditingVar(v);
    form.setFieldsValue({ key: v.key, value: v.value });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    setSaving(true);
    try {
      const values = await form.validateFields();
      if (editingVar) {
        await updateVariable(editingVar.id, {
          key: values.key,
          value: values.value,
        });
        message.success("更新成功");
      } else {
        const data: CreateVariableRequest = {
          scope: values.scope,
          scopeId: values.scopeId ?? pid,
          key: values.key,
          value: values.value,
        };
        await createVariable(pid, data);
        message.success("创建成功");
      }
      setModalOpen(false);
      if (viewMode === "table") {
        fetchVariables();
      } else {
        fetchTreeData();
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (varId: number) => {
    await deleteVariable(varId);
    message.success("删除成功");
    if (viewMode === "table") {
      fetchVariables();
    } else {
      fetchTreeData();
    }
  };

  const handleDetect = useCallback(async () => {
    if (!pid) return;
    setDetecting(true);
    try {
      const vars = await detectVariables(pid);
      const rows: DetectedVariableRow[] = vars.map((v) => {
        const scopeType = v.suggestedScope === "PROJECT" ? "project" : "role";
        const firstOccurrence = v.occurrences[0];
        return {
          ...v,
          scopeType,
          targetRoleId: scopeType === "role" ? firstOccurrence.roleId : undefined,
          userValue: "",
          rowKey: v.key + "-" + (scopeType === "role" ? firstOccurrence.roleId : "proj"),
        };
      });
      setDetectedVars(rows);
      message.success(`检测到 ${rows.length} 个未注册变量`);
    } catch {
      message.error("变量探测失败");
      setDetectedVars([]);
    } finally {
      setDetecting(false);
    }
  }, [pid]);

  const updateRowScope = useCallback(
    (rowKey: string, scopeType: "project" | "role", targetRoleId?: number) => {
      setDetectedVars((prev) =>
        prev.map((r) => (r.rowKey === rowKey ? { ...r, scopeType, targetRoleId } : r))
      );
    },
    []
  );

  const updateRowValue = useCallback((rowKey: string, value: string) => {
    setDetectedVars((prev) =>
      prev.map((r) => (r.rowKey === rowKey ? { ...r, userValue: value } : r))
    );
  }, []);

  const deleteRow = useCallback((rowKey: string) => {
    setDetectedVars((prev) => prev.filter((r) => r.rowKey !== rowKey));
  }, []);

  const handleCopy = useCallback((record: DetectedVariableRow) => {
    setCopyModal({
      open: true,
      sourceVar: record,
      targetType: record.scopeType === "project" ? "role" : "role",
    });
  }, []);

  const confirmCopy = useCallback(() => {
    if (!copyModal.sourceVar) return;
    const src = copyModal.sourceVar;
    const newRowKey =
      src.key +
      "-" +
      copyModal.targetType +
      "-" +
      (copyModal.targetRoleId ?? "proj") +
      "-" +
      Date.now();
    const newRow: DetectedVariableRow = {
      ...src,
      scopeType: copyModal.targetType,
      targetRoleId: copyModal.targetType === "role" ? copyModal.targetRoleId : undefined,
      rowKey: newRowKey,
    };
    setDetectedVars((prev) => [...prev, newRow]);
    setCopyModal({ open: false, sourceVar: null, targetType: "role" });
  }, [copyModal]);

  const handleBatchSave = useCallback(async () => {
    if (!pid) return;
    // Frontend validation: check duplicates within batch
    const seen = new Map<string, string>();
    const errors: string[] = [];
    for (const row of detectedVars) {
      const id =
        row.scopeType === "project"
          ? `project:${pid}:${row.key}`
          : `role:${row.targetRoleId}:${row.key}`;
      if (seen.has(id)) {
        errors.push(`变量 "${row.key}" 重复`);
      }
      seen.set(id, row.rowKey);
    }
    if (errors.length > 0) {
      message.error(errors.join(", "));
      return;
    }

    setSavingVars(true);
    try {
      const items: BatchVariableSaveItem[] = detectedVars.map((row) => ({
        key: row.key,
        saveAs: row.scopeType === "project" ? "VARIABLE" : "ROLE_VARIABLE",
        scope: row.scopeType === "project" ? "PROJECT" : undefined,
        roleId: row.scopeType === "role" ? row.targetRoleId : undefined,
        value: row.userValue || undefined,
      }));

      const results = await batchSaveVariables(pid, items);
      const failed = results.filter((r) => !r.success);
      const succeeded = results.filter((r) => r.success);

      if (succeeded.length > 0) {
        message.success(`成功保存 ${succeeded.length} 个变量`);
        setDetectedVars((prev) => prev.filter((_, i) => results[i]?.success));
        fetchVariables();
      }
      if (failed.length > 0) {
        const msgs = failed.map((f) => f.error).join("; ");
        message.error(`保存失败: ${msgs}`);
      }
    } catch {
      message.error("批量保存失败");
    } finally {
      setSavingVars(false);
    }
  }, [pid, detectedVars, fetchVariables]);

  const scopeOptions = (): { label: string; value: number }[] => {
    if (scope === "HOSTGROUP") return hostGroups.map((h) => ({ label: h.name, value: h.id }));
    if (scope === "ENVIRONMENT") return environments.map((e) => ({ label: e.name, value: e.id }));
    return [];
  };

  const columns = [
    { title: "Key", dataIndex: "key", key: "key" },
    { title: "Value", dataIndex: "value", key: "value" },
    {
      title: "操作",
      key: "action",
      render: (_: unknown, record: Variable) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const defaultExpandedKeys = useMemo(() => treeData.map((n) => n.key), [treeData]);

  return (
    <div>
      {/* Variable Priority Info Card */}
      <Card size="small" style={{ marginBottom: 16 }} title="变量优先级说明">
        <Typography.Text type="secondary">变量优先级（从高到低）：</Typography.Text>
        <ol style={{ margin: "4px 0 0", paddingLeft: 20 }}>
          <li>
            <Typography.Text strong>环境级变量</Typography.Text>
            <Typography.Text type="secondary">
              {" "}
              — 最高优先级，适用于特定环境（如 production）
            </Typography.Text>
          </li>
          <li>
            <Typography.Text strong>主机组级变量</Typography.Text>
            <Typography.Text type="secondary"> — 适用于特定主机组</Typography.Text>
          </li>
          <li>
            <Typography.Text strong>项目级变量</Typography.Text>
            <Typography.Text type="secondary"> — 适用于整个项目的全局变量</Typography.Text>
          </li>
          <li>
            <Typography.Text strong>Role 级变量（vars/）</Typography.Text>
            <Typography.Text type="secondary"> — Role 内部变量</Typography.Text>
          </li>
          <li>
            <Typography.Text strong>Role 默认变量（defaults/）</Typography.Text>
            <Typography.Text type="secondary"> — 最低优先级</Typography.Text>
          </li>
        </ol>
      </Card>

      {/* View mode toggle and controls */}
      <div
        style={{
          marginBottom: 16,
          display: "flex",
          gap: 12,
          alignItems: "center",
        }}
      >
        <Segmented
          value={viewMode}
          onChange={(val) => setViewMode(val as ViewMode)}
          options={[
            {
              label: "表格视图",
              value: "table",
              icon: <TableOutlined />,
            },
            {
              label: "树形视图",
              value: "tree",
              icon: <ApartmentOutlined />,
            },
          ]}
        />
        {viewMode === "table" && (
          <>
            <Select
              value={scope}
              onChange={handleScopeChange}
              style={{ width: 140 }}
              options={Object.entries(scopeLabels).map(([k, v]) => ({
                label: v,
                value: k,
              }))}
            />
            {scope !== "PROJECT" && (
              <Select
                value={scopeId}
                onChange={setScopeId}
                style={{ width: 200 }}
                placeholder={`选择${scopeLabels[scope]}`}
                options={scopeOptions()}
              />
            )}
          </>
        )}
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建变量
        </Button>
        <Button
          icon={<ScanOutlined />}
          onClick={handleDetect}
          loading={detecting}
          style={{ borderStyle: "dashed" }}
        >
          变量探测
        </Button>
      </div>

      {detectedVars.length > 0 && (
        <Card
          size="small"
          title={
            <Space>
              <span>探测结果</span>
              <Tag color="blue">{detectedVars.length} 个变量</Tag>
            </Space>
          }
          extra={
            <Space>
              <Button onClick={() => setDetectedVars([])}>取消</Button>
              <Button type="primary" onClick={handleBatchSave} loading={savingVars}>
                批量保存 ({detectedVars.length}条)
              </Button>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          <Table
            rowKey="rowKey"
            dataSource={detectedVars}
            pagination={false}
            size="small"
            columns={[
              {
                title: "变量名",
                dataIndex: "key",
                width: 180,
                render: (key: string) => (
                  <code
                    style={{
                      background: "#f5f5f5",
                      padding: "2px 6px",
                      borderRadius: 4,
                    }}
                  >
                    {key}
                  </code>
                ),
              },
              {
                title: "来源",
                dataIndex: "occurrences",
                width: 300,
                render: (occurrences: DetectedVariableRow["occurrences"]) => (
                  <div>
                    {occurrences.map((o, i) => (
                      <div key={i} style={{ fontSize: 12, color: "#666" }}>
                        {o.roleName} ·{" "}
                        {o.type === "TASK" ? "任务" : o.type === "HANDLER" ? "Handler" : "模板"} ·{" "}
                        {o.entityName}
                      </div>
                    ))}
                  </div>
                ),
              },
              {
                title: "作用域",
                width: 220,
                render: (_: unknown, record: DetectedVariableRow) => (
                  <Space>
                    <Select
                      value={record.scopeType}
                      onChange={(val) =>
                        updateRowScope(
                          record.rowKey,
                          val,
                          val === "role" ? record.occurrences[0]?.roleId : undefined
                        )
                      }
                      style={{ width: 100 }}
                      size="small"
                      options={[
                        { label: "项目级", value: "project" },
                        { label: "Role 级", value: "role" },
                      ]}
                    />
                    {record.scopeType === "role" && (
                      <Select
                        value={record.targetRoleId}
                        onChange={(val) => updateRowScope(record.rowKey, "role", val)}
                        style={{ width: 100 }}
                        size="small"
                        options={Array.from(
                          new Map(record.occurrences.map((o) => [o.roleId, o.roleName]))
                        ).map(([id, name]) => ({ label: name, value: id }))}
                      />
                    )}
                  </Space>
                ),
              },
              {
                title: "值（可选）",
                dataIndex: "userValue",
                width: 160,
                render: (_: unknown, record: DetectedVariableRow) => (
                  <Input
                    size="small"
                    placeholder="输入变量值"
                    value={record.userValue}
                    onChange={(e) => updateRowValue(record.rowKey, e.target.value)}
                  />
                ),
              },
              {
                title: "操作",
                width: 100,
                render: (_: unknown, record: DetectedVariableRow) => (
                  <Space>
                    <Tooltip title="复制">
                      <Button
                        type="text"
                        size="small"
                        icon={<CopyOutlined />}
                        onClick={() => handleCopy(record)}
                      />
                    </Tooltip>
                    <Tooltip title="删除">
                      <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => deleteRow(record.rowKey)}
                      />
                    </Tooltip>
                  </Space>
                ),
              },
            ]}
          />
        </Card>
      )}

      {/* Table View */}
      {viewMode === "table" && (
        <>
          {variables.length === 0 && !loading && (
            <Empty description="暂无变量" style={{ marginTop: 40 }} />
          )}
          <Table rowKey="id" columns={columns} dataSource={variables} loading={loading} />
        </>
      )}

      {/* Tree View */}
      {viewMode === "tree" && (
        <>
          {treeData.length === 0 && !treeLoading && (
            <Empty description="暂无变量" style={{ marginTop: 40 }} />
          )}
          <Tree
            showLine
            defaultExpandedKeys={defaultExpandedKeys}
            treeData={treeData}
            style={{ marginTop: 8 }}
          />
        </>
      )}

      <Modal
        title={editingVar ? "编辑变量" : "新建变量"}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          {!editingVar && (
            <>
              <Form.Item
                name="scope"
                label="作用域"
                rules={[{ required: true, message: "请选择作用域" }]}
              >
                <Select
                  options={Object.entries(scopeLabels).map(([k, v]) => ({
                    label: v,
                    value: k,
                  }))}
                />
              </Form.Item>
              {form.getFieldValue("scope") !== "PROJECT" && (
                <Form.Item
                  name="scopeId"
                  label="关联对象"
                  rules={[{ required: true, message: "请选择关联对象" }]}
                >
                  <Select options={scopeOptions()} />
                </Form.Item>
              )}
            </>
          )}
          <Form.Item name="key" label="Key" rules={[{ required: true, message: "请输入变量名" }]}>
            <Input maxLength={100} placeholder="变量名，如 APP_PORT" />
          </Form.Item>
          <Form.Item name="value" label="Value">
            <Input.TextArea rows={3} placeholder="变量值" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="复制变量到其他作用域"
        open={copyModal.open}
        onOk={confirmCopy}
        onCancel={() => setCopyModal({ open: false, sourceVar: null, targetType: "role" })}
        okText="确认复制"
        cancelText="取消"
      >
        {copyModal.sourceVar && (
          <div style={{ padding: "8px 0" }}>
            <p>
              变量：<code>{copyModal.sourceVar.key}</code>
            </p>
            <p>
              复制到：
              <Select
                value={copyModal.targetType}
                onChange={(val) =>
                  setCopyModal((prev) => ({
                    ...prev,
                    targetType: val,
                    targetRoleId: undefined,
                  }))
                }
                style={{ width: 120, marginLeft: 8 }}
                size="small"
                options={[
                  { label: "项目级", value: "project" },
                  { label: "Role 级", value: "role" },
                ]}
              />
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}
