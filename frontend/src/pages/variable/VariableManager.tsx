import { useEffect, useState, useCallback } from 'react';
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
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
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
import type { Environment } from '../../types/entity/Environment';
import type { HostGroup } from '../../types/entity/Host';

const scopeLabels: Record<VariableScope, string> = {
  PROJECT: '项目级',
  HOSTGROUP: '主机组级',
  ENVIRONMENT: '环境级',
};

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
    fetchVariables();
  }, [fetchVariables]);

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
    fetchVariables();
  };

  const handleDelete = async (varId: number) => {
    await deleteVariable(varId);
    message.success('删除成功');
    fetchVariables();
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

  return (
    <div>
      <div
        style={{
          marginBottom: 16,
          display: 'flex',
          gap: 12,
          alignItems: 'center',
        }}
      >
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建变量
        </Button>
      </div>

      {variables.length === 0 && !loading && (
        <Empty description="暂无变量" style={{ marginTop: 40 }} />
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={variables}
        loading={loading}
      />

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
