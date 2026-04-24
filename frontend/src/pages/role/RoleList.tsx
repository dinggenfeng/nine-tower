import { useEffect, useState, useCallback } from "react";
import { Button, Form, Input, message, Modal, Popconfirm, Space, Table } from "antd";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { useNavigate, useParams } from "react-router-dom";
import type { Role, CreateRoleRequest } from "../../types/entity/Role";
import { createRole, deleteRole, getRoles, updateRole } from "../../api/role";
import PageHeader from "../../components/PageHeader";

export default function RoleList() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);
  const navigate = useNavigate();

  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [form] = Form.useForm<CreateRoleRequest>();

  const fetchRoles = useCallback(async () => {
    setLoading(true);
    try {
      setRoles(await getRoles(pid));
    } finally {
      setLoading(false);
    }
  }, [pid]);

  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createRole(pid, values);
    message.success("Role 创建成功");
    setModalOpen(false);
    form.resetFields();
    fetchRoles();
  };

  const handleUpdate = async () => {
    if (!editingRole) return;
    const values = await form.validateFields();
    await updateRole(editingRole.id, values);
    message.success("Role 更新成功");
    setModalOpen(false);
    setEditingRole(null);
    form.resetFields();
    fetchRoles();
  };

  const handleDelete = async (roleId: number) => {
    await deleteRole(roleId);
    message.success("Role 已删除");
    fetchRoles();
  };

  const openModal = (role?: Role) => {
    if (role) {
      setEditingRole(role);
      form.setFieldsValue({ name: role.name, description: role.description });
    } else {
      setEditingRole(null);
      form.resetFields();
    }
    setModalOpen(true);
  };

  const columns = [
    {
      title: "名称",
      dataIndex: "name",
      key: "name",
      render: (name: string, record: Role) => (
        <a
          onClick={() => navigate(`/projects/${pid}/roles/${record.id}`)}
          style={{ color: "#3b82f6", fontWeight: 500 }}
        >
          {name}
        </a>
      ),
    },
    { title: "描述", dataIndex: "description", key: "description" },
    {
      title: "操作",
      key: "action",
      width: 120,
      render: (_: unknown, record: Role) => (
        <Space>
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openModal(record)}
          />
          <Popconfirm title="确认删除此 Role？" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Roles"
        description="管理 Ansible Roles"
        action={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新建 Role
          </Button>
        }
      />

      <Table
        columns={columns}
        dataSource={roles}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingRole ? "编辑 Role" : "新建 Role"}
        open={modalOpen}
        onOk={editingRole ? handleUpdate : handleCreate}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
