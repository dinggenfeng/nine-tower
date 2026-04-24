import { useEffect, useState, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Table, Modal, Form, Input, message, Popconfirm, Space } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import type { Playbook } from "../../types/entity/Playbook";
import { listPlaybooks, createPlaybook, deletePlaybook } from "../../api/playbook";

export default function PlaybookList() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);
  const navigate = useNavigate();
  const [playbooks, setPlaybooks] = useState<Playbook[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<{ name: string; description: string }>();

  const fetchPlaybooks = useCallback(async () => {
    if (!pid) return;
    setLoading(true);
    try {
      const data = await listPlaybooks(pid);
      setPlaybooks(data);
    } finally {
      setLoading(false);
    }
  }, [pid]);

  useEffect(() => {
    fetchPlaybooks();
  }, [fetchPlaybooks]);

  const handleCreate = async () => {
    setCreating(true);
    try {
      const values = await form.validateFields();
      await createPlaybook(pid, values);
      message.success("创建成功");
      setModalOpen(false);
      form.resetFields();
      fetchPlaybooks();
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number) => {
    await deletePlaybook(id);
    message.success("删除成功");
    fetchPlaybooks();
  };

  const columns = [
    {
      title: "名称",
      dataIndex: "name",
      key: "name",
      render: (name: string, record: Playbook) => (
        <Button type="link" onClick={() => navigate(`/projects/${pid}/playbooks/${record.id}`)}>
          {name}
        </Button>
      ),
    },
    { title: "描述", dataIndex: "description", key: "description" },
    {
      title: "Roles",
      key: "roles",
      render: (_: unknown, r: Playbook) => r.roleIds.length,
    },
    {
      title: "操作",
      key: "action",
      render: (_: unknown, record: Playbook) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => navigate(`/projects/${pid}/playbooks/${record.id}`)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
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
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          新建 Playbook
        </Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={playbooks} loading={loading} />
      <Modal
        title="新建 Playbook"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
        confirmLoading={creating}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input placeholder="例如: deploy.yml" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="Playbook 描述（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
