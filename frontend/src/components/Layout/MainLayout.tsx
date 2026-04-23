import { useEffect, useState } from 'react';
import {
  LogoutOutlined,
  ProjectOutlined,
  UserOutlined,
  LockOutlined,
  MailOutlined,
  CalendarOutlined,
} from '@ant-design/icons';
import {
  Avatar,
  Dropdown,
  Layout,
  Modal,
  Form,
  Input,
  Descriptions,
  Divider,
  message,
  Spin,
  type MenuProps,
} from 'antd';
import { useNavigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import { userApi } from '../../api/user';
import styles from './MainLayout.module.css';

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout, fetchUser, setUser } = useAuthStore();
  const [profileOpen, setProfileOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [profileForm] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  const handleProfileOpen = () => {
    if (user) {
      profileForm.setFieldsValue({ email: user.email });
      setProfileOpen(true);
    }
  };

  const handlePasswordOpen = () => {
    passwordForm.resetFields();
    setPasswordOpen(true);
  };

  const handleProfileSave = async () => {
    if (!user) return;
    try {
      const values = await profileForm.validateFields();
      if (values.email === user.email) {
        message.info('没有修改');
        return;
      }
      setProfileSaving(true);
      const res = await userApi.updateUser(user.id, { email: values.email });
      setUser(res.data);
      message.success('邮箱修改成功');
      setProfileOpen(false);
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      const msg =
        (error as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ||
        (error as { message?: string })?.message ||
        '操作失败';
      message.error(msg);
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordSave = async () => {
    if (!user) return;
    try {
      const values = await passwordForm.validateFields();
      setPasswordSaving(true);
      await userApi.updateUser(user.id, {
        oldPassword: values.oldPassword,
        password: values.newPassword,
      });
      message.success('密码修改成功');
      setPasswordOpen(false);
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      const msg =
        (error as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ||
        (error as { message?: string })?.message ||
        '操作失败';
      message.error(msg);
    } finally {
      setPasswordSaving(false);
    }
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
      onClick: handleProfileOpen,
    },
    {
      key: 'password',
      icon: <LockOutlined />,
      label: '修改密码',
      onClick: handlePasswordOpen,
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true,
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header className={styles.header}>
        <div className={styles.logo} onClick={() => navigate('/projects')}>
          <ProjectOutlined className={styles.logoIcon} />
          <span className={styles.logoText}>Ansible Playbook Studio</span>
        </div>
        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <div className={styles.userArea}>
            <Avatar
              size="small"
              style={{ backgroundColor: '#3b82f6', fontSize: 13 }}
            >
              {user?.username?.[0]?.toUpperCase() || <Spin size="small" />}
            </Avatar>
            <span className={styles.username}>{user?.username ?? ''}</span>
          </div>
        </Dropdown>
      </Layout.Header>
      <Layout.Content className={styles.content}>
        <Outlet />
      </Layout.Content>

      {/* 个人信息 Modal */}
      <Modal
        title="个人信息"
        open={profileOpen}
        onOk={handleProfileSave}
        onCancel={() => setProfileOpen(false)}
        confirmLoading={profileSaving}
        okText="保存"
        width={440}
        destroyOnClose
      >
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <Avatar
            size={56}
            style={{
              backgroundColor: '#3b82f6',
              fontSize: 22,
              marginBottom: 8,
            }}
          >
            {user?.username?.[0]?.toUpperCase()}
          </Avatar>
        </div>

        <Descriptions column={1} size="small" colon={false}>
          <Descriptions.Item
            label={
              <span style={{ color: '#64748b' }}>
                <UserOutlined style={{ marginRight: 6 }} />
                用户名
              </span>
            }
          >
            {user?.username}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span style={{ color: '#64748b' }}>
                <CalendarOutlined style={{ marginRight: 6 }} />
                注册时间
              </span>
            }
          >
            {user?.createdAt
              ? new Date(user.createdAt).toLocaleDateString('zh-CN')
              : ''}
          </Descriptions.Item>
        </Descriptions>

        <Divider style={{ margin: '16px 0' }} />

        <Form form={profileForm} layout="vertical">
          <Form.Item
            name="email"
            label={
              <span>
                <MailOutlined style={{ marginRight: 6 }} />
                邮箱
              </span>
            }
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '邮箱格式不正确' },
            ]}
          >
            <Input placeholder="your@email.com" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 修改密码 Modal */}
      <Modal
        title="修改密码"
        open={passwordOpen}
        onOk={handlePasswordSave}
        onCancel={() => setPasswordOpen(false)}
        confirmLoading={passwordSaving}
        okText="确认修改"
        width={420}
        destroyOnClose
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            name="oldPassword"
            label="旧密码"
            rules={[{ required: true, message: '请输入旧密码' }]}
          >
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码至少 8 个字符' },
              { max: 100, message: '密码最多 100 个字符' },
            ]}
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (value && value !== getFieldValue('newPassword')) {
                    return Promise.reject(new Error('两次输入的密码不一致'));
                  }
                  return Promise.resolve();
                },
              }),
            ]}
          >
            <Input.Password placeholder="再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
}
