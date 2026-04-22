import { useEffect, useState, useCallback } from 'react';
import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Typography,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  Environment,
  EnvConfig,
  CreateEnvironmentRequest,
  UpdateEnvironmentRequest,
  EnvConfigRequest,
} from '../../types/entity/Environment';
import {
  listEnvironments,
  createEnvironment,
  updateEnvironment,
  deleteEnvironment,
  addConfig,
  updateConfig,
  removeConfig,
} from '../../api/environment';

const { Text } = Typography;

export default function EnvironmentManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [loading, setLoading] = useState(false);
  const [envModalOpen, setEnvModalOpen] = useState(false);
  const [editingEnv, setEditingEnv] = useState<Environment | null>(null);
  const [configModalOpen, setConfigModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<EnvConfig | null>(null);
  const [activeEnvId, setActiveEnvId] = useState<number | null>(null);
  const [envForm] = Form.useForm<CreateEnvironmentRequest>();
  const [configForm] = Form.useForm<EnvConfigRequest>();

  const fetchEnvironments = useCallback(async () => {
    if (!pid) return;
    setLoading(true);
    try {
      const data = await listEnvironments(pid);
      setEnvironments(data);
    } finally {
      setLoading(false);
    }
  }, [pid]);

  useEffect(() => {
    fetchEnvironments();
  }, [fetchEnvironments]);

  const handleCreateEnv = () => {
    setEditingEnv(null);
    envForm.resetFields();
    setEnvModalOpen(true);
  };

  const handleEditEnv = (env: Environment) => {
    setEditingEnv(env);
    envForm.setFieldsValue({ name: env.name, description: env.description ?? '' });
    setEnvModalOpen(true);
  };

  const handleDeleteEnv = async (envId: number) => {
    await deleteEnvironment(envId);
    message.success('删除成功');
    fetchEnvironments();
  };

  const handleEnvSubmit = async () => {
    const values = await envForm.validateFields();
    if (editingEnv) {
      const updateData: UpdateEnvironmentRequest = {
        name: values.name,
        description: values.description,
      };
      await updateEnvironment(editingEnv.id, updateData);
      message.success('更新成功');
    } else {
      await createEnvironment(pid, values);
      message.success('创建成功');
    }
    setEnvModalOpen(false);
    fetchEnvironments();
  };

  const handleAddConfig = (envId: number) => {
    setEditingConfig(null);
    setActiveEnvId(envId);
    configForm.resetFields();
    setConfigModalOpen(true);
  };

  const handleEditConfig = (envId: number, config: EnvConfig) => {
    setEditingConfig(config);
    setActiveEnvId(envId);
    configForm.setFieldsValue({
      configKey: config.configKey,
      configValue: config.configValue,
    });
    setConfigModalOpen(true);
  };

  const handleConfigSubmit = async () => {
    const values = await configForm.validateFields();
    if (editingConfig) {
      await updateConfig(editingConfig.id, values);
      message.success('配置项已更新');
    } else {
      await addConfig(activeEnvId!, values);
      message.success('配置项已添加');
    }
    setConfigModalOpen(false);
    fetchEnvironments();
  };

  const handleRemoveConfig = async (configId: number) => {
    await removeConfig(configId);
    message.success('配置项已删除');
    fetchEnvironments();
  };

  const configColumns = (envId: number) => [
    { title: 'Key', dataIndex: 'configKey', key: 'key' },
    { title: 'Value', dataIndex: 'configValue', key: 'value' },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: EnvConfig) => (
        <Space>
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditConfig(envId, record)}
          />
          <Popconfirm
            title="确定删除？"
            onConfirm={() => handleRemoveConfig(record.id)}
          >
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreateEnv}
        >
          新建环境
        </Button>
      </div>

      {environments.length === 0 && !loading && (
        <Empty description="暂无环境" style={{ marginTop: 40 }} />
      )}

      {environments.map((env) => (
        <Card
          key={env.id}
          title={env.name}
          extra={
            <Space>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEditEnv(env)}
              >
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                onClick={() => handleAddConfig(env.id)}
              >
                添加配置
              </Button>
              <Popconfirm
                title="确定删除此环境？"
                onConfirm={() => handleDeleteEnv(env.id)}
              >
                <Button type="link" size="small" danger>
                  删除
                </Button>
              </Popconfirm>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          {env.description && (
            <Text type="secondary" style={{ marginBottom: 8, display: 'block' }}>
              {env.description}
            </Text>
          )}
          <Table
            size="small"
            pagination={false}
            rowKey="id"
            columns={configColumns(env.id)}
            dataSource={env.configs}
            locale={{
              emptyText: (
                <Empty
                  description="暂无配置项"
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
              ),
            }}
          />
        </Card>
      ))}

      <Modal
        title={editingEnv ? '编辑环境' : '新建环境'}
        open={envModalOpen}
        onOk={handleEnvSubmit}
        onCancel={() => setEnvModalOpen(false)}
      >
        <Form form={envForm} layout="vertical">
          <Form.Item
            name="name"
            label="环境名称"
            rules={[{ required: true, message: '请输入环境名称' }]}
          >
            <Input
              maxLength={100}
              placeholder="例如: dev, staging, production"
            />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea
              maxLength={500}
              placeholder="环境描述（可选）"
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingConfig ? '编辑配置项' : '添加配置项'}
        open={configModalOpen}
        onOk={handleConfigSubmit}
        onCancel={() => setConfigModalOpen(false)}
      >
        <Form form={configForm} layout="vertical">
          <Form.Item
            name="configKey"
            label="Key"
            rules={[{ required: true, message: '请输入配置键' }]}
          >
            <Input maxLength={100} placeholder="例如: DB_HOST" />
          </Form.Item>
          <Form.Item
            name="configValue"
            label="Value"
            rules={[{ required: true, message: '请输入配置值' }]}
          >
            <Input.TextArea placeholder="配置值" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
