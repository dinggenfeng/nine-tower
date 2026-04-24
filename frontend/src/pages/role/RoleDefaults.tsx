import { useCallback, useEffect, useState } from "react";
import { Button, Form, Input, Modal, Popconfirm, Space, Table, message } from "antd";
import { PlusOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";
import type {
  RoleDefaultVariable,
  CreateRoleDefaultVariableRequest,
  UpdateRoleDefaultVariableRequest,
} from "../../types/entity/RoleVariable";
import {
  createRoleDefault,
  getRoleDefaults,
  updateRoleDefault,
  deleteRoleDefault,
} from "../../api/roleVariable";

interface RoleDefaultsProps {
  roleId: number;
}

export default function RoleDefaults({ roleId }: RoleDefaultsProps) {
  const [defaults, setDefaults] = useState<RoleDefaultVariable[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVar, setEditingVar] = useState<RoleDefaultVariable | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getRoleDefaults(roleId);
      setDefaults(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = () => {
    setEditingVar(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (variable: RoleDefaultVariable) => {
    setEditingVar(variable);
    form.setFieldsValue({ key: variable.key, value: variable.value });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    setSaving(true);
    try {
      const values = await form.validateFields();
      if (editingVar) {
        const data: UpdateRoleDefaultVariableRequest = {
          key: values.key,
          value: values.value,
        };
        await updateRoleDefault(editingVar.id, data);
        message.success("默认变量已更新");
      } else {
        const data: CreateRoleDefaultVariableRequest = {
          key: values.key,
          value: values.value,
        };
        await createRoleDefault(roleId, data);
        message.success("默认变量已创建");
      }
      setModalOpen(false);
      fetchData();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    await deleteRoleDefault(id);
    message.success("默认变量已删除");
    fetchData();
  };

  const columns = [
    { title: "Key", dataIndex: "key", key: "key" },
    { title: "Value", dataIndex: "value", key: "value" },
    {
      title: "操作",
      key: "action",
      render: (_: unknown, record: RoleDefaultVariable) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
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
          添加默认变量
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={defaults}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editingVar ? "编辑默认变量" : "添加默认变量"}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="key" label="Key" rules={[{ required: true, message: "请输入变量名" }]}>
            <Input placeholder="例如: http_port" />
          </Form.Item>
          <Form.Item name="value" label="Value">
            <Input.TextArea rows={3} placeholder="默认值" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
