import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
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

  const handleCreate = () => {
    setEditingTemplate(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = async (record: Template) => {
    const detail = await getTemplate(record.id);
    setEditingTemplate(detail);
    form.setFieldsValue({
      name: detail.name,
      parentDir: detail.parentDir,
      targetPath: detail.targetPath,
      content: detail.content,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingTemplate) {
      const data: UpdateTemplateRequest = {
        name: values.name,
        parentDir: values.parentDir,
        targetPath: values.targetPath,
        content: values.content,
      };
      await updateTemplate(editingTemplate.id, data);
      message.success('模板已更新');
    } else {
      const data: CreateTemplateRequest = {
        name: values.name,
        parentDir: values.parentDir,
        targetPath: values.targetPath,
        content: values.content,
      };
      await createTemplate(roleId, data);
      message.success('模板已创建');
    }
    setModalOpen(false);
    fetchData();
  };

  const handleDelete = async (id: number) => {
    await deleteTemplate(id);
    message.success('模板已删除');
    fetchData();
  };

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
        width={720}
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
          <Form.Item name="content" label="模板内容">
            <Input.TextArea
              rows={12}
              placeholder="Jinja2 模板内容"
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
