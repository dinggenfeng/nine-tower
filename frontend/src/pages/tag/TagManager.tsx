import { useEffect, useState, useCallback } from "react";
import { Button, Table, Modal, Form, Input, message, Popconfirm, Space } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { useParams } from "react-router-dom";
import type { Tag } from "../../types/entity/Tag";
import { listTags, createTag, updateTag, deleteTag } from "../../api/tag";

export default function TagManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTag, setEditingTag] = useState<Tag | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<{ name: string }>();

  const fetchTags = useCallback(async () => {
    if (!pid) return;
    setLoading(true);
    try {
      const data = await listTags(pid);
      setTags(data);
    } finally {
      setLoading(false);
    }
  }, [pid]);

  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  const handleCreate = () => {
    setEditingTag(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (tag: Tag) => {
    setEditingTag(tag);
    form.setFieldsValue({ name: tag.name });
    setModalOpen(true);
  };

  const handleDelete = async (tagId: number) => {
    await deleteTag(tagId);
    message.success("删除成功");
    fetchTags();
  };

  const handleSubmit = async () => {
    setSaving(true);
    try {
      const values = await form.validateFields();
      if (editingTag) {
        await updateTag(editingTag.id, values);
        message.success("更新成功");
      } else {
        await createTag(pid, values);
        message.success("创建成功");
      }
      setModalOpen(false);
      fetchTags();
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    { title: "标签名称", dataIndex: "name", key: "name" },
    {
      title: "操作",
      key: "action",
      render: (_: unknown, record: Tag) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建标签
        </Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={tags} loading={loading} />
      <Modal
        title={editingTag ? "编辑标签" : "新建标签"}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="标签名称"
            rules={[{ required: true, message: "请输入标签名称" }]}
          >
            <Input maxLength={100} placeholder="例如: web, db, production" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
