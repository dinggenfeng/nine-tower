import { useEffect, useState, useCallback } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  InputNumber,
  Select,
  message,
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
import { colors } from '../../theme';
import PageHeader from '../../components/PageHeader';

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
    role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER',
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
            onChange={(value) =>
              handleRoleChange(
                record.userId,
                value as 'PROJECT_ADMIN' | 'PROJECT_MEMBER',
              )
            }
            options={[
              { value: 'PROJECT_ADMIN', label: '管理员' },
              { value: 'PROJECT_MEMBER', label: '成员' },
            ]}
            style={{ width: 120 }}
          />
        ) : (
          <span
            style={{
              background:
                role === 'PROJECT_ADMIN'
                  ? colors.tagAdminBg
                  : colors.tagMemberBg,
              color:
                role === 'PROJECT_ADMIN'
                  ? colors.tagAdminColor
                  : colors.tagMemberColor,
              fontSize: 12,
              padding: '2px 8px',
              borderRadius: 4,
              fontWeight: 500,
            }}
          >
            {role === 'PROJECT_ADMIN' ? '管理员' : '成员'}
          </span>
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
            title: '操作',
            key: 'action',
            render: (_: unknown, record: ProjectMember) => (
              <Popconfirm
                title="确认移除此成员？"
                onConfirm={() => handleRemove(record.userId)}
              >
                <Button type="link" danger size="small">
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
      <PageHeader
        title="成员管理"
        description="管理项目成员和权限"
        action={
          isAdmin ? (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setAddModalOpen(true)}
            >
              添加成员
            </Button>
          ) : undefined
        }
      />

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
