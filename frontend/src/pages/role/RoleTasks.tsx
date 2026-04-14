import { useCallback, useEffect, useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { Task, CreateTaskRequest, UpdateTaskRequest } from '../../types/entity/Task';
import type { Handler } from '../../types/entity/Task';
import { createTask, getTasks, updateTask, deleteTask } from '../../api/task';
import { getHandlers } from '../../api/handler';

interface RoleTasksProps {
  roleId: number;
}

export default function RoleTasks({ roleId }: RoleTasksProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [handlers, setHandlers] = useState<Handler[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [taskList, handlerList] = await Promise.all([
        getTasks(roleId),
        getHandlers(roleId),
      ]);
      setTasks(taskList);
      setHandlers(handlerList);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = () => {
    setEditingTask(null);
    form.resetFields();
    form.setFieldValue('taskOrder', tasks.length + 1);
    setModalOpen(true);
  };

  const handleEdit = (task: Task) => {
    setEditingTask(task);
    form.setFieldsValue({
      name: task.name,
      module: task.module,
      args: task.args,
      whenCondition: task.whenCondition,
      loop: task.loop,
      until: task.until,
      register: task.register,
      notify: task.notify,
      taskOrder: task.taskOrder,
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await deleteTask(id);
    message.success('已删除');
    fetchData();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingTask) {
      const data: UpdateTaskRequest = { ...values };
      await updateTask(editingTask.id, data);
      message.success('已更新');
    } else {
      const data: CreateTaskRequest = { ...values };
      await createTask(roleId, data);
      message.success('已创建');
    }
    setModalOpen(false);
    fetchData();
  };

  const columns = [
    {
      title: '顺序',
      dataIndex: 'taskOrder',
      key: 'taskOrder',
      width: 70,
    },
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
      title: 'Notify',
      dataIndex: 'notify',
      key: 'notify',
      render: (notify: string[]) =>
        notify?.map((n) => <Tag key={n}>{n}</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: Task) => (
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
          添加 Task
        </Button>
      </div>
      <Table
        dataSource={tasks}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="middle"
      />
      <Modal
        title={editingTask ? '编辑 Task' : '创建 Task'}
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
            rules={[{ required: true, message: '请输入 Task 名称' }]}
          >
            <Input placeholder="例如: Install nginx" />
          </Form.Item>
          <Form.Item
            name="module"
            label="模块"
            rules={[{ required: true, message: '请输入 Ansible 模块名' }]}
          >
            <Input placeholder="例如: apt, yum, service, copy" />
          </Form.Item>
          <Form.Item name="args" label="参数 (JSON)">
            <Input.TextArea rows={3} placeholder='{"name": "nginx", "state": "present"}' />
          </Form.Item>
          <Form.Item name="whenCondition" label="When 条件">
            <Input placeholder="例如: ansible_os_family == 'Debian'" />
          </Form.Item>
          <Form.Item name="loop" label="Loop">
            <Input placeholder="例如: {{ packages }}" />
          </Form.Item>
          <Form.Item name="until" label="Until">
            <Input placeholder="例如: result.rc == 0" />
          </Form.Item>
          <Form.Item name="register" label="Register">
            <Input placeholder="例如: install_result" />
          </Form.Item>
          <Form.Item name="notify" label="Notify (Handler)">
            <Select
              mode="multiple"
              placeholder="选择要通知的 Handler"
              options={handlers.map((h) => ({ label: h.name, value: h.name }))}
            />
          </Form.Item>
          <Form.Item
            name="taskOrder"
            label="顺序"
            rules={[{ required: true, message: '请输入顺序' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
