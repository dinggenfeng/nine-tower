import { useEffect, useState, useCallback, useMemo } from 'react';
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
} from 'antd';
import {
  PlusOutlined,
  TableOutlined,
  ApartmentOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  Variable,
  VariableScope,
  CreateVariableRequest,
} from '../../types/entity/Variable';
import {
  listVariables,
  createVariable,
  updateVariable,
  deleteVariable,
} from '../../api/variable';
import { listEnvironments } from '../../api/environment';
import { getHostGroups } from '../../api/host';
import { getRoles } from '../../api/role';
import { getRoleVariables, getRoleDefaults } from '../../api/roleVariable';
import type { Environment } from '../../types/entity/Environment';
import type { HostGroup } from '../../types/entity/Host';
import type { Role } from '../../types/entity/Role';
import type { RoleVariable, RoleDefaultVariable } from '../../types/entity/RoleVariable';

const scopeLabels: Record<VariableScope, string> = {
  PROJECT: '项目级',
  HOSTGROUP: '主机组级',
  ENVIRONMENT: '环境级',
};

type ViewMode = 'table' | 'tree';

interface TreeVariableNode {
  key: string;
  title: string;
  selectable?: boolean;
  children?: TreeVariableNode[];
}

export default function VariableManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [variables, setVariables] = useState<Variable[]>([]);
  const [loading, setLoading] = useState(false);
  const [scope, setScope] = useState<VariableScope>('PROJECT');
  const [scopeId, setScopeId] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVar, setEditingVar] = useState<Variable | null>(null);
  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [form] = Form.useForm();

  // Tree view state
  const [viewMode, setViewMode] = useState<ViewMode>('table');
  const [treeData, setTreeData] = useState<TreeVariableNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);

  useEffect(() => {
    if (!pid) return;
    getHostGroups(pid).then(setHostGroups);
    listEnvironments(pid).then(setEnvironments);
  }, [pid]);

  const fetchVariables = useCallback(async () => {
    if (!pid) return;
    setLoading(true);
    try {
      const data = await listVariables(pid, scope, scopeId);
      setVariables(data);
    } finally {
      setLoading(false);
    }
  }, [pid, scope, scopeId]);

  useEffect(() => {
    if (viewMode === 'table') {
      fetchVariables();
    }
  }, [fetchVariables, viewMode]);

  // Build tree data by fetching variables across all scopes
  const fetchTreeData = useCallback(async () => {
    if (!pid) return;
    setTreeLoading(true);
    try {
      const [projectVars, hostgroupVars, envVars, roles] = await Promise.all([
        listVariables(pid, 'PROJECT').catch(() => [] as Variable[]),
        listVariables(pid, 'HOSTGROUP').catch(() => [] as Variable[]),
        listVariables(pid, 'ENVIRONMENT').catch(() => [] as Variable[]),
        getRoles(pid).catch(() => [] as Role[]),
      ]);

      // Fetch role variables and defaults for all roles
      const roleVarPromises = roles.map((role) =>
        getRoleVariables(role.id)
          .then((vars) => ({ roleId: role.id, vars }))
          .catch(() => ({ roleId: role.id, vars: [] as RoleVariable[] })),
      );
      const roleDefaultPromises = roles.map((role) =>
        getRoleDefaults(role.id)
          .then((defaults) => ({ roleId: role.id, defaults }))
          .catch(() => ({ roleId: role.id, defaults: [] as RoleDefaultVariable[] })),
      );

      const [roleVarResults, roleDefaultResults] = await Promise.all([
        Promise.all(roleVarPromises),
        Promise.all(roleDefaultPromises),
      ]);

      const roleVarsMap = new Map<number, RoleVariable[]>();
      roleVarResults.forEach((r) => roleVarsMap.set(r.roleId, r.vars));

      const roleDefaultsMap = new Map<number, RoleDefaultVariable[]>();
      roleDefaultResults.forEach((r) => roleDefaultsMap.set(r.roleId, r.defaults));

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
            title: `${v.key} = ${v.value}`,
          })),
        });
      });
      nodes.push({
        key: 'scope-ENVIRONMENT',
        title: `环境级变量 (${envVars.length})`,
        children:
          envChildren.length > 0
            ? envChildren
            : [{ key: 'env-empty', title: '(无)', selectable: false }],
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
            title: `${v.key} = ${v.value}`,
          })),
        });
      });
      nodes.push({
        key: 'scope-HOSTGROUP',
        title: `主机组级变量 (${hostgroupVars.length})`,
        children:
          hgChildren.length > 0
            ? hgChildren
            : [{ key: 'hg-empty', title: '(无)', selectable: false }],
      });

      // Project scope
      const projectChildren: TreeVariableNode[] = projectVars.map((v) => ({
        key: `project-var-${v.id}`,
        title: `${v.key} = ${v.value}`,
      }));
      nodes.push({
        key: 'scope-PROJECT',
        title: `项目级变量 (${projectVars.length})`,
        children:
          projectChildren.length > 0
            ? projectChildren
            : [{ key: 'project-empty', title: '(无)', selectable: false }],
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
              title: `${v.key} = ${v.value}`,
            })),
          });
        }
      });
      const totalRoleVars = roleVarResults.reduce(
        (sum, r) => sum + r.vars.length,
        0,
      );
      nodes.push({
        key: 'scope-ROLE_VARS',
        title: `Role 级变量 (${totalRoleVars})`,
        children:
          roleVarsChildren.length > 0
            ? roleVarsChildren
            : [{ key: 'role-vars-empty', title: '(无)', selectable: false }],
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
              title: `${d.key} = ${d.value}`,
            })),
          });
        }
      });
      const totalRoleDefaults = roleDefaultResults.reduce(
        (sum, r) => sum + r.defaults.length,
        0,
      );
      nodes.push({
        key: 'scope-ROLE_DEFAULTS',
        title: `Role 默认变量 (${totalRoleDefaults})`,
        children:
          roleDefaultsChildren.length > 0
            ? roleDefaultsChildren
            : [
                {
                  key: 'role-defaults-empty',
                  title: '(无)',
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
    if (viewMode === 'tree') {
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
    const values = await form.validateFields();
    if (editingVar) {
      await updateVariable(editingVar.id, {
        key: values.key,
        value: values.value,
      });
      message.success('更新成功');
    } else {
      const data: CreateVariableRequest = {
        scope: values.scope,
        scopeId: values.scopeId,
        key: values.key,
        value: values.value,
      };
      await createVariable(pid, data);
      message.success('创建成功');
    }
    setModalOpen(false);
    if (viewMode === 'table') {
      fetchVariables();
    } else {
      fetchTreeData();
    }
  };

  const handleDelete = async (varId: number) => {
    await deleteVariable(varId);
    message.success('删除成功');
    if (viewMode === 'table') {
      fetchVariables();
    } else {
      fetchTreeData();
    }
  };

  const scopeOptions = (): { label: string; value: number }[] => {
    if (scope === 'HOSTGROUP')
      return hostGroups.map((h) => ({ label: h.name, value: h.id }));
    if (scope === 'ENVIRONMENT')
      return environments.map((e) => ({ label: e.name, value: e.id }));
    return [];
  };

  const columns = [
    { title: 'Key', dataIndex: 'key', key: 'key' },
    { title: 'Value', dataIndex: 'value', key: 'value' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Variable) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const defaultExpandedKeys = useMemo(
    () => treeData.map((n) => n.key),
    [treeData],
  );

  return (
    <div>
      {/* Variable Priority Info Card */}
      <Card
        size="small"
        style={{ marginBottom: 16 }}
        title="变量优先级说明"
      >
        <Typography.Text type="secondary">
          变量优先级（从高到低）：
        </Typography.Text>
        <ol style={{ margin: '4px 0 0', paddingLeft: 20 }}>
          <li>
            <Typography.Text strong>环境级变量</Typography.Text>
            <Typography.Text type="secondary">
              {' '}
              — 最高优先级，适用于特定环境（如 production）
            </Typography.Text>
          </li>
          <li>
            <Typography.Text strong>主机组级变量</Typography.Text>
            <Typography.Text type="secondary">
              {' '}
              — 适用于特定主机组
            </Typography.Text>
          </li>
          <li>
            <Typography.Text strong>Role 级变量（vars/）</Typography.Text>
            <Typography.Text type="secondary">
              {' '}
              — Role 内部变量
            </Typography.Text>
          </li>
          <li>
            <Typography.Text strong>Role 默认变量（defaults/）</Typography.Text>
            <Typography.Text type="secondary">
              {' '}
              — 最低优先级
            </Typography.Text>
          </li>
        </ol>
      </Card>

      {/* View mode toggle and controls */}
      <div
        style={{
          marginBottom: 16,
          display: 'flex',
          gap: 12,
          alignItems: 'center',
        }}
      >
        <Segmented
          value={viewMode}
          onChange={(val) => setViewMode(val as ViewMode)}
          options={[
            {
              label: '表格视图',
              value: 'table',
              icon: <TableOutlined />,
            },
            {
              label: '树形视图',
              value: 'tree',
              icon: <ApartmentOutlined />,
            },
          ]}
        />
        {viewMode === 'table' && (
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
            {scope !== 'PROJECT' && (
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
      </div>

      {/* Table View */}
      {viewMode === 'table' && (
        <>
          {variables.length === 0 && !loading && (
            <Empty description="暂无变量" style={{ marginTop: 40 }} />
          )}
          <Table
            rowKey="id"
            columns={columns}
            dataSource={variables}
            loading={loading}
          />
        </>
      )}

      {/* Tree View */}
      {viewMode === 'tree' && (
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
        title={editingVar ? '编辑变量' : '新建变量'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          {!editingVar && (
            <>
              <Form.Item
                name="scope"
                label="作用域"
                rules={[{ required: true, message: '请选择作用域' }]}
              >
                <Select
                  options={Object.entries(scopeLabels).map(([k, v]) => ({
                    label: v,
                    value: k,
                  }))}
                />
              </Form.Item>
              {form.getFieldValue('scope') !== 'PROJECT' && (
                <Form.Item
                  name="scopeId"
                  label="关联对象"
                  rules={[{ required: true, message: '请选择关联对象' }]}
                >
                  <Select options={scopeOptions()} />
                </Form.Item>
              )}
            </>
          )}
          <Form.Item
            name="key"
            label="Key"
            rules={[{ required: true, message: '请输入变量名' }]}
          >
            <Input maxLength={100} placeholder="变量名，如 APP_PORT" />
          </Form.Item>
          <Form.Item name="value" label="Value">
            <Input.TextArea rows={3} placeholder="变量值" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
