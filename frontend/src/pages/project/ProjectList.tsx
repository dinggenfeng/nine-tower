import { useEffect, useState } from 'react';
import { Button, Empty, List, Modal, Form, Input, message, Dropdown, type MenuProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  SettingOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { Project, CreateProjectRequest } from '../../types/entity/Project';
import { getMyProjects, createProject, deleteProject } from '../../api/project';
import { colors } from '../../theme';

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
    message.success('项目创建成功');
    setCreateModalOpen(false);
    form.resetFields();
    fetchProjects();
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除项目？',
      content: '此操作无法撤销。',
      onOk: async () => {
        await deleteProject(id);
        message.success('项目已删除');
        fetchProjects();
      },
    });
  };

  return (
    <div style={{ padding: 24 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: 8,
        }}
      >
        <h2 style={{ margin: 0, color: '#1e293b' }}>我的项目</h2>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
        >
          新建项目
        </Button>
      </div>
      <div style={{ color: '#64748b', fontSize: 14, marginBottom: 20 }}>
        共 {projects.length} 个项目
      </div>

      <List
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4 }}
        loading={loading}
        dataSource={projects}
        locale={{
          emptyText: (
            <Empty description="暂无项目">
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setCreateModalOpen(true)}
              >
                创建第一个项目
              </Button>
            </Empty>
          ),
        }}
        renderItem={(project) => {
          const isAdmin = project.myRole === 'PROJECT_ADMIN';
          const initial = project.name[0]?.toUpperCase() || 'P';

          const menuItems: MenuProps['items'] = [
            ...(isAdmin
              ? [
                  {
                    key: 'settings',
                    icon: <SettingOutlined />,
                    label: '项目设置',
                    onClick: () => navigate(`/projects/${project.id}/settings`),
                  },
                  {
                    key: 'delete',
                    icon: <DeleteOutlined />,
                    label: '删除项目',
                    danger: true,
                    onClick: () => handleDelete(project.id),
                  },
                ]
              : []),
          ];

          return (
            <List.Item>
              <div
                onClick={() => navigate(`/projects/${project.id}/roles`)}
                style={{
                  background: '#fff',
                  borderRadius: 8,
                  padding: 16,
                  border: '1px solid #e2e8f0',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)';
                  e.currentTarget.style.borderColor = '#cbd5e1';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.04)';
                  e.currentTarget.style.borderColor = '#e2e8f0';
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                  }}
                >
                  <div
                    style={{
                      width: 32,
                      height: 32,
                      background: '#1e293b',
                      borderRadius: 6,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 14,
                      fontWeight: 600,
                      color: '#f1f5f9',
                    }}
                  >
                    {initial}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span
                      style={{
                        background: isAdmin ? colors.tagAdminBg : colors.tagMemberBg,
                        color: isAdmin ? colors.tagAdminColor : colors.tagMemberColor,
                        fontSize: 11,
                        padding: '2px 8px',
                        borderRadius: 4,
                        fontWeight: 500,
                      }}
                    >
                      {isAdmin ? '管理员' : '成员'}
                    </span>
                    {menuItems && menuItems.length > 0 && (
                      <Dropdown
                        menu={{ items: menuItems }}
                        trigger={['click']}
                        placement="bottomRight"
                      >
                        <MoreOutlined
                          onClick={(e) => e.stopPropagation()}
                          style={{ color: '#94a3b8', fontSize: 16 }}
                        />
                      </Dropdown>
                    )}
                  </div>
                </div>
                <div
                  style={{
                    fontSize: 15,
                    fontWeight: 600,
                    color: '#1e293b',
                    marginTop: 12,
                  }}
                >
                  {project.name}
                </div>
                <div
                  style={{
                    fontSize: 13,
                    color: '#64748b',
                    marginTop: 4,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {project.description || '暂无描述'}
                </div>
              </div>
            </List.Item>
          );
        }}
      />

      <Modal
        title="创建项目"
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
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
