import { useEffect, useState } from 'react';
import { Button, Card, Empty, List, Modal, Form, Input, message, Tag } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { Project, CreateProjectRequest } from '../../types/entity/Project';
import { getMyProjects, createProject, deleteProject } from '../../api/project';

const { TextArea } = Input;

export default function ProjectList() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm<CreateProjectRequest>();
  const navigate = useNavigate();

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const data = await getMyProjects();
      setProjects(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createProject(values);
    message.success('Project created');
    setCreateModalOpen(false);
    form.resetFields();
    fetchProjects();
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Delete project?',
      content: 'This action cannot be undone.',
      onOk: async () => {
        await deleteProject(id);
        message.success('Project deleted');
        fetchProjects();
      },
    });
  };

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>My Projects</h2>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
        >
          New Project
        </Button>
      </div>

      <List
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4 }}
        loading={loading}
        dataSource={projects}
        locale={{ emptyText: <Empty description="No projects yet" /> }}
        renderItem={(project) => (
          <List.Item>
            <Card
              hoverable
              onClick={() => navigate(`/projects/${project.id}/roles`)}
              actions={[
                project.myRole === 'PROJECT_ADMIN' && (
                  <SettingOutlined
                    key="settings"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/projects/${project.id}/settings`);
                    }}
                  />
                ),
                project.myRole === 'PROJECT_ADMIN' && (
                  <DeleteOutlined
                    key="delete"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(project.id);
                    }}
                  />
                ),
              ].filter(Boolean)}
            >
              <Card.Meta
                title={
                  <span>
                    {project.name}{' '}
                    <Tag color={project.myRole === 'PROJECT_ADMIN' ? 'blue' : 'default'}>
                      {project.myRole === 'PROJECT_ADMIN' ? 'Admin' : 'Member'}
                    </Tag>
                  </span>
                }
                description={project.description || 'No description'}
              />
            </Card>
          </List.Item>
        )}
      />

      <Modal
        title="Create Project"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => {
          setCreateModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Project Name"
            rules={[{ required: true, message: 'Please enter a project name' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
