import { useCallback, useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Popconfirm, Space, Switch, Table, message, Collapse, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, DownOutlined, QuestionCircleOutlined, EyeOutlined, CopyOutlined } from '@ant-design/icons';
import type { Handler, CreateHandlerRequest, UpdateHandlerRequest } from '../../types/entity/Task';
import { createHandler, getHandlers, updateHandler, deleteHandler } from '../../api/handler';
import ModuleSelect from '../../components/role/ModuleSelect';
import { ModuleParamsGrid, ExtraParamsInput } from '../../components/role/ModuleParamsForm';
import { getModuleDefinition } from '../../constants/ansibleModules';
import { taskToYaml } from '../../utils/taskToYaml';

interface RoleHandlersProps {
  roleId: number;
}

/** Merge moduleParams + extraParams into a JSON string for the args field */
function buildArgsJson(
  moduleParams: Record<string, unknown> | undefined,
  extraParams: { key: string; value: string }[] | undefined,
): string {
  const result: Record<string, unknown> = {};
  if (moduleParams) {
    for (const [k, v] of Object.entries(moduleParams)) {
      if (v !== undefined && v !== '' && v !== null) {
        result[k] = v;
      }
    }
  }
  if (extraParams) {
    for (const item of extraParams) {
      if (item.key) {
        result[item.key] = item.value;
      }
    }
  }
  return Object.keys(result).length > 0 ? JSON.stringify(result) : '';
}

/** Parse args JSON string into moduleParams + extraParams for form population */
function parseArgsToForm(
  argsJson: string | undefined,
  moduleName: string | undefined,
): { moduleParams: Record<string, unknown>; extraParams: { key: string; value: string }[] } {
  const moduleParams: Record<string, unknown> = {};
  const extraParams: { key: string; value: string }[] = [];
  if (!argsJson) return { moduleParams, extraParams };

  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(argsJson);
  } catch {
    extraParams.push({ key: '', value: argsJson });
    return { moduleParams, extraParams };
  }

  const moduleDef = moduleName ? getModuleDefinition(moduleName) : undefined;
  const knownParams = new Set(moduleDef?.params.map((p) => p.name) ?? []);

  for (const [k, v] of Object.entries(parsed)) {
    if (knownParams.has(k)) {
      moduleParams[k] = v;
    } else {
      extraParams.push({ key: k, value: String(v) });
    }
  }
  return { moduleParams, extraParams };
}

export default function RoleHandlers({ roleId }: RoleHandlersProps) {
  const [handlers, setHandlers] = useState<Handler[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingHandler, setEditingHandler] = useState<Handler | null>(null);
  const [selectedModule, setSelectedModule] = useState<string | undefined>(undefined);
  const [previewYaml, setPreviewYaml] = useState<string>('');
  const [previewOpen, setPreviewOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchHandlers = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getHandlers(roleId);
      setHandlers(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchHandlers();
  }, [fetchHandlers]);

  const handleCreate = () => {
    setEditingHandler(null);
    setSelectedModule(undefined);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (handler: Handler) => {
    setEditingHandler(handler);
    const { moduleParams, extraParams } = parseArgsToForm(handler.args, handler.module);
    setSelectedModule(handler.module);
    form.setFieldsValue({
      name: handler.name,
      module: handler.module,
      moduleParams,
      extraParams: extraParams.length > 0 ? extraParams : undefined,
      whenCondition: handler.whenCondition,
      register: handler.register,
      become: handler.become || false,
      becomeUser: handler.becomeUser,
      ignoreErrors: handler.ignoreErrors || false,
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await deleteHandler(id);
    message.success('已删除');
    fetchHandlers();
  };

  const handlePreviewHandler = (handler: Handler) => {
    setPreviewYaml(taskToYaml({
      name: handler.name,
      module: handler.module,
      args: handler.args,
      whenCondition: handler.whenCondition,
      register: handler.register,
      become: handler.become,
      becomeUser: handler.becomeUser,
      ignoreErrors: handler.ignoreErrors,
    }));
    setPreviewOpen(true);
  };

  const handlePreviewForm = () => {
    const values = form.getFieldsValue();
    const args = buildArgsJson(values.moduleParams, values.extraParams);
    setPreviewYaml(
      taskToYaml({
        name: values.name,
        module: values.module,
        args: args || undefined,
        whenCondition: values.whenCondition,
        register: values.register,
        become: values.become,
        becomeUser: values.becomeUser,
        ignoreErrors: values.ignoreErrors,
      }),
    );
    setPreviewOpen(true);
  };

  const handleCopyYaml = async () => {
    await navigator.clipboard.writeText(previewYaml);
    message.success('已复制');
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();

    const moduleDef = values.module ? getModuleDefinition(values.module) : undefined;
    if (moduleDef?.validate) {
      const errors = moduleDef.validate(values.moduleParams || {});
      if (Object.keys(errors).length > 0) {
        const fieldErrors = Object.entries(errors).map(([field, msg]) => ({
          name: ['moduleParams', field],
          errors: [msg],
        }));
        form.setFields(fieldErrors);
        return;
      }
    }

    const args = buildArgsJson(values.moduleParams, values.extraParams);

    if (editingHandler) {
      const data: UpdateHandlerRequest = {
        name: values.name,
        module: values.module,
        args: args || undefined,
        whenCondition: values.whenCondition,
        register: values.register,
        become: values.become || false,
        becomeUser: values.becomeUser,
        ignoreErrors: values.ignoreErrors || false,
      };
      await updateHandler(editingHandler.id, data);
      message.success('已更新');
    } else {
      const data: CreateHandlerRequest = {
        name: values.name,
        module: values.module,
        args: args || undefined,
        whenCondition: values.whenCondition,
        register: values.register,
        become: values.become || false,
        becomeUser: values.becomeUser,
        ignoreErrors: values.ignoreErrors || false,
      };
      await createHandler(roleId, data);
      message.success('已创建');
    }
    setModalOpen(false);
    fetchHandlers();
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 120,
    },
    {
      title: 'When',
      dataIndex: 'whenCondition',
      key: 'whenCondition',
      render: (val: string) => val || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: unknown, record: Handler) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handlePreviewHandler(record)}
          />
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加 Handler
        </Button>
      </div>
      <Table
        dataSource={handlers}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="middle"
      />
      <Modal
        title={editingHandler ? '编辑 Handler' : '创建 Handler'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        width={800}
        destroyOnClose
        footer={
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <Button icon={<EyeOutlined />} onClick={handlePreviewForm}>
              预览 YAML
            </Button>
            <Space>
              <Button onClick={() => setModalOpen(false)}>取消</Button>
              <Button type="primary" onClick={handleSubmit}>确定</Button>
            </Space>
          </div>
        }
      >
        <Form form={form} layout="vertical">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item
              name="name"
              label="名称"
              rules={[{ required: true, message: '请输入 Handler 名称' }]}
            >
              <Input placeholder="例如: Restart nginx" />
            </Form.Item>
          </div>
          <Form.Item
            name="module"
            label="模块"
            rules={[{ required: true, message: '请选择 Ansible 模块' }]}
          >
            <ModuleSelect
              onChange={(val) => {
                setSelectedModule(val);
                form.setFieldsValue({ moduleParams: undefined, extraParams: undefined });
              }}
            />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <ModuleParamsGrid moduleName={selectedModule} />
          </div>
          <ExtraParamsInput />
          <Collapse
            ghost
            bordered={false}
            expandIcon={({ isActive }) => <DownOutlined rotate={isActive ? 90 : 0} />}
            items={[
              {
                key: 'advanced',
                forceRender: true,
                label: '高级选项',
                children: (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
                    <Form.Item
                      name="whenCondition"
                      label={
                        <span>
                          When 条件
                          <Tooltip title="Handler 执行的前置条件表达式">
                            <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                          </Tooltip>
                        </span>
                      }
                    >
                      <Input placeholder="例如: ansible_os_family == 'Debian'" />
                    </Form.Item>
                    <Form.Item
                      name="register"
                      label={
                        <span>
                          Register
                          <Tooltip title="将 Handler 输出保存到变量中，供其他任务使用">
                            <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                          </Tooltip>
                        </span>
                      }
                    >
                      <Input placeholder="例如: restart_result" />
                    </Form.Item>
                    <Form.Item
                      name="become"
                      label={
                        <span>
                          提权 (become)
                          <Tooltip title="是否使用提权（sudo）执行此 Handler">
                            <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                          </Tooltip>
                        </span>
                      }
                      valuePropName="checked"
                    >
                      <Switch />
                    </Form.Item>
                    <Form.Item
                      name="becomeUser"
                      label={
                        <span>
                          提权用户 (become_user)
                          <Tooltip title="提权后切换到的用户，默认 root">
                            <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                          </Tooltip>
                        </span>
                      }
                    >
                      <Input placeholder="root" />
                    </Form.Item>
                    <Form.Item
                      name="ignoreErrors"
                      label={
                        <span>
                          忽略错误 (ignore_errors)
                          <Tooltip title="任务失败时是否继续执行后续任务">
                            <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                          </Tooltip>
                        </span>
                      }
                      valuePropName="checked"
                    >
                      <Switch />
                    </Form.Item>
                  </div>
                ),
              },
            ]}
          />
        </Form>
      </Modal>
      <Modal
        title="YAML 预览"
        open={previewOpen}
        onCancel={() => setPreviewOpen(false)}
        footer={
          <Button icon={<CopyOutlined />} onClick={handleCopyYaml}>
            复制
          </Button>
        }
        width={600}
        zIndex={1100}
      >
        <pre
          style={{
            background: '#f5f5f5',
            padding: 16,
            borderRadius: 6,
            fontSize: 13,
            lineHeight: 1.6,
            overflow: 'auto',
            maxHeight: 400,
          }}
        >
          {previewYaml}
        </pre>
      </Modal>
    </div>
  );
}
