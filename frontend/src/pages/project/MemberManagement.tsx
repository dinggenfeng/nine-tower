import { useEffect, useState, useCallback } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  InputNumber,
  Select,
  message,
  Tag,
  Popconfirm,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  ProjectMember,
  AddMemberRequest,
} from '../../types/entity/Project';
import {
  getMembers,
  addMember,
  removeMember,
  updateMemberRole,
} from '../../api/project';
import { useProjectStore } from '../../stores/projectStore';

export default function MemberManagement() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [form] = Form.useForm<AddMemberRequest>();
  const { currentProject } = useProjectStore();
  const isAdmin = currentProject?.myRole === 'PROJECT_ADMIN';

  const fetchMembers = useCallback(async () => {
    setLoading(true);
    try {
      setMembers(await getMembers(projectId));
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const handleAdd = async () => {
    const values = await form.validateFields();
    await addMember(projectId, values);
    message.success('成员添加成功');
    setAddModalOpen(false);
    form.resetFields();
    fetchMembers();
  };

  const handleRemove = async (userId: number) => {
    await removeMember(projectId, userId);
    message.success('成员已移除');
    fetchMembers();
  };

  const handleRoleChange = async (
    userId: number,
    role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER'
  ) => {
    await updateMemberRole(projectId, userId, { role });
    message.success('角色更新成功');
    fetchMembers();
  };

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string, record: ProjectMember) =>
        isAdmin ? (
          <Select
            value={role}
            onChange={(value) => handleRoleChange(record.userId, value as 'PROJECT_ADMIN' | 'PROJECT_MEMBER')}
            options={[
              { value: 'PROJECT_ADMIN', label: '管理员' },
              { value: 'PROJECT_MEMBER', label: '成员' },
            ]}
            style={{ width: 120 }}
          />
        ) : (
          <Tag color={role === 'PROJECT_ADMIN' ? 'blue' : 'default'}>
            {role === 'PROJECT_ADMIN' ? '管理员' : '成员'}
          </Tag>
        ),
    },
    {
      title: '加入时间',
      dataIndex: 'joinedAt',
      key: 'joinedAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    ...(isAdmin
      ? [
          {
            title: 'Action',
            key: 'action',
            render: (_: unknown, record: ProjectMember) => (
              <Popconfirm
                title="确认移除此成员？"
                onConfirm={() => handleRemove(record.userId)}
              >
                <Button type="link" danger>
                  移除
                </Button>
              </Popconfirm>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>成员管理</h2>
        {isAdmin && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setAddModalOpen(true)}
          >
            添加成员
          </Button>
        )}
      </div>

      <Table
        columns={columns}
        dataSource={members}
        rowKey="userId"
        loading={loading}
        pagination={false}
      />

      <Modal
        title="添加成员"
        open={addModalOpen}
        onOk={handleAdd}
        onCancel={() => {
          setAddModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="userId"
            label="用户 ID"
            rules={[{ required: true, message: '请输入用户 ID' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select
              options={[
                { value: 'PROJECT_ADMIN', label: '管理员' },
                { value: 'PROJECT_MEMBER', label: '成员' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
