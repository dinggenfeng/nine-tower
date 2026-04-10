import { useEffect, useState } from 'react';
import { Form, Input, Button, message, Card } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import type { UpdateProjectRequest } from '../../types/entity/Project';
import { updateProject, deleteProject } from '../../api/project';
import { useProjectStore } from '../../stores/projectStore';

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
      message.success('Project updated');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = () => {
    deleteProject(Number(id)).then(() => {
      message.success('Project deleted');
      navigate('/projects');
    });
  };

  if (currentProject?.myRole !== 'PROJECT_ADMIN') {
    return <div>Only project admins can access settings.</div>;
  }

  return (
    <div>
      <h2>Project Settings</h2>
      <Card style={{ maxWidth: 600 }}>
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item
            name="name"
            label="Project Name"
            rules={[{ required: true, message: 'Project name is required' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              Save
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card style={{ maxWidth: 600, marginTop: 24 }} title="Danger Zone">
        <Button danger onClick={handleDelete}>
          Delete Project
        </Button>
      </Card>
    </div>
  );
}
