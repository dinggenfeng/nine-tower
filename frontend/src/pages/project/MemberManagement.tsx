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
    message.success('Member added');
    setAddModalOpen(false);
    form.resetFields();
    fetchMembers();
  };

  const handleRemove = async (userId: number) => {
    await removeMember(projectId, userId);
    message.success('Member removed');
    fetchMembers();
  };

  const handleRoleChange = async (
    userId: number,
    role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER'
  ) => {
    await updateMemberRole(projectId, userId, { role });
    message.success('Role updated');
    fetchMembers();
  };

  const columns = [
    { title: 'Username', dataIndex: 'username', key: 'username' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      render: (role: string, record: ProjectMember) =>
        isAdmin ? (
          <Select
            value={role}
            onChange={(value) => handleRoleChange(record.userId, value as 'PROJECT_ADMIN' | 'PROJECT_MEMBER')}
            options={[
              { value: 'PROJECT_ADMIN', label: 'Admin' },
              { value: 'PROJECT_MEMBER', label: 'Member' },
            ]}
            style={{ width: 120 }}
          />
        ) : (
          <Tag color={role === 'PROJECT_ADMIN' ? 'blue' : 'default'}>
            {role === 'PROJECT_ADMIN' ? 'Admin' : 'Member'}
          </Tag>
        ),
    },
    {
      title: 'Joined',
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
                title="Remove this member?"
                onConfirm={() => handleRemove(record.userId)}
              >
                <Button type="link" danger>
                  Remove
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
        <h2 style={{ margin: 0 }}>Members</h2>
        {isAdmin && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setAddModalOpen(true)}
          >
            Add Member
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
        title="Add Member"
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
            label="User ID"
            rules={[{ required: true, message: 'Please enter the user ID' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item
            name="role"
            label="Role"
            rules={[{ required: true, message: 'Please select a role' }]}
          >
            <Select
              options={[
                { value: 'PROJECT_ADMIN', label: 'Admin' },
                { value: 'PROJECT_MEMBER', label: 'Member' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
