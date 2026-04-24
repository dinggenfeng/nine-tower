import { useEffect, useState } from "react";
import { Form, Input, Button, message, Card } from "antd";
import { useParams, useNavigate } from "react-router-dom";
import type { UpdateProjectRequest } from "../../types/entity/Project";
import { updateProject, deleteProject } from "../../api/project";
import { useProjectStore } from "../../stores/projectStore";
import PageHeader from "../../components/PageHeader";

const { TextArea } = Input;

export default function ProjectSettings() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm<UpdateProjectRequest>();
  const [loading, setLoading] = useState(false);
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (currentProject) {
      form.setFieldsValue({
        name: currentProject.name,
        description: currentProject.description,
      });
    }
  }, [currentProject, form]);

  const handleUpdate = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const updated = await updateProject(Number(id), values);
      setCurrentProject(updated);
      message.success("项目更新成功");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = () => {
    deleteProject(Number(id)).then(() => {
      message.success("项目已删除");
      navigate("/projects");
    });
  };

  if (currentProject?.myRole !== "PROJECT_ADMIN") {
    return <div>仅项目管理员可访问设置。</div>;
  }

  return (
    <div>
      <PageHeader title="项目设置" description="管理项目基本信息" />
      <Card style={{ maxWidth: 600 }}>
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item
            name="name"
            label="项目名称"
            rules={[{ required: true, message: "请输入项目名称" }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              保存
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="危险区域"
        style={{
          maxWidth: 600,
          marginTop: 24,
          borderLeft: "3px solid #ef4444",
          background: "#fef2f2",
        }}
      >
        <p style={{ color: "#64748b", marginBottom: 16 }}>
          删除项目后，所有数据将永久丢失且无法恢复。
        </p>
        <Button danger onClick={handleDelete}>
          删除项目
        </Button>
      </Card>
    </div>
  );
}
