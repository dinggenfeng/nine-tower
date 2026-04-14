import { useCallback, useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Popconfirm, Space, Table, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { Handler, CreateHandlerRequest, UpdateHandlerRequest } from '../../types/entity/Task';
import { createHandler, getHandlers, updateHandler, deleteHandler } from '../../api/handler';

interface RoleHandlersProps {
  roleId: number;
}

export default function RoleHandlers({ roleId }: RoleHandlersProps) {
  const [handlers, setHandlers] = useState<Handler[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingHandler, setEditingHandler] = useState<Handler | null>(null);
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
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (handler: Handler) => {
    setEditingHandler(handler);
    form.setFieldsValue({
      name: handler.name,
      module: handler.module,
      args: handler.args,
      whenCondition: handler.whenCondition,
      register: handler.register,
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await deleteHandler(id);
    message.success('已删除');
    fetchHandlers();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingHandler) {
      const data: UpdateHandlerRequest = { ...values };
      await updateHandler(editingHandler.id, data);
      message.success('已更新');
    } else {
      const data: CreateHandlerRequest = { ...values };
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
      width: 120,
      render: (_: unknown, record: Handler) => (
        <Space size="small">
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
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入 Handler 名称' }]}
          >
            <Input placeholder="例如: Restart nginx" />
          </Form.Item>
          <Form.Item
            name="module"
            label="模块"
            rules={[{ required: true, message: '请输入 Ansible 模块名' }]}
          >
            <Input placeholder="例如: service, systemd" />
          </Form.Item>
          <Form.Item name="args" label="参数 (JSON)">
            <Input.TextArea rows={3} placeholder='{"name": "nginx", "state": "restarted"}' />
          </Form.Item>
          <Form.Item name="whenCondition" label="When 条件">
            <Input placeholder="例如: ansible_os_family == 'Debian'" />
          </Form.Item>
          <Form.Item name="register" label="Register">
            <Input placeholder="例如: restart_result" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
