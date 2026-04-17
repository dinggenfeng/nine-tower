import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  message,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from '../../types/entity/Template';
import {
  createTemplate,
  getTemplates,
  getTemplate,
  updateTemplate,
  deleteTemplate,
} from '../../api/template';

interface RoleTemplatesProps {
  roleId: number;
}

export default function RoleTemplates({ roleId }: RoleTemplatesProps) {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<Template | null>(null);
  const [contentTemplate, setContentTemplate] = useState<Template | null>(null);
  const [contentValue, setContentValue] = useState('');
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getTemplates(roleId);
      setTemplates(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // --- 基本信息 Modal ---
  const handleCreate = () => {
    setEditingTemplate(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: Template) => {
    setEditingTemplate(record);
    form.setFieldsValue({
      name: record.name,
      parentDir: record.parentDir,
      targetPath: record.targetPath,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingTemplate) {
        const data: UpdateTemplateRequest = {
          name: values.name,
          parentDir: values.parentDir,
          targetPath: values.targetPath,
        };
        await updateTemplate(editingTemplate.id, data);
        message.success('模板已更新');
      } else {
        const data: CreateTemplateRequest = {
          name: values.name,
          parentDir: values.parentDir,
          targetPath: values.targetPath,
        };
        await createTemplate(roleId, data);
        message.success('模板已创建');
      }
      setModalOpen(false);
      fetchData();
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleDelete = async (id: number) => {
    await deleteTemplate(id);
    message.success('模板已删除');
    fetchData();
  };

  // --- 内容编辑 ---
  const handleEditContent = async (record: Template) => {
    const detail = await getTemplate(record.id);
    setContentTemplate(detail);
    setContentValue(detail.content || '');
  };

  const handleSaveContent = async () => {
    if (!contentTemplate) return;
    setSaving(true);
    try {
      await updateTemplate(contentTemplate.id, { content: contentValue });
      message.success('模板内容已保存');
      setContentTemplate(null);
      fetchData();
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err) {
        message.error((err as { message: string }).message);
      }
    } finally {
      setSaving(false);
    }
  };

  // --- 内容编辑视图 ---
  if (contentTemplate) {
    return (
      <div>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => setContentTemplate(null)}
            style={{ color: '#64748b', padding: '4px 8px' }}
          >
            返回模板列表
          </Button>
          <Button type="primary" onClick={handleSaveContent} loading={saving}>
            保存
          </Button>
        </div>
        <Card
          title={
            <Space>
              <FileTextOutlined />
              <span>{contentTemplate.parentDir ? `${contentTemplate.parentDir}/` : ''}{contentTemplate.name}</span>
            </Space>
          }
          size="small"
          style={{ marginBottom: 16 }}
        >
          <span style={{ color: '#64748b' }}>
            目标路径: {contentTemplate.targetPath || '未设置'}
          </span>
        </Card>
        <Input.TextArea
          value={contentValue}
          onChange={(e) => setContentValue(e.target.value)}
          rows={20}
          placeholder="Jinja2 模板内容"
          style={{ fontFamily: 'monospace', fontSize: 13 }}
        />
      </div>
    );
  }

  // --- 列表视图 ---
  const columns = [
    { title: '文件名', dataIndex: 'name', key: 'name' },
    {
      title: '目录',
      dataIndex: 'parentDir',
      key: 'parentDir',
      render: (dir: string | null) => dir || '/',
    },
    { title: '目标路径', dataIndex: 'targetPath', key: 'targetPath' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Template) => (
        <Space>
          <Button
            type="link"
            icon={<FileTextOutlined />}
            onClick={() => handleEditContent(record)}
          >
            编辑内容
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加模板
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={templates}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editingTemplate ? '编辑模板' : '添加模板'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="文件名"
            rules={[{ required: true, message: '请输入模板文件名' }]}
          >
            <Input placeholder="例如: nginx.conf.j2" />
          </Form.Item>
          <Form.Item name="parentDir" label="目录">
            <Input placeholder="例如: nginx/conf.d（留空表示根目录）" />
          </Form.Item>
          <Form.Item name="targetPath" label="目标路径">
            <Input placeholder="例如: /etc/nginx/nginx.conf" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
