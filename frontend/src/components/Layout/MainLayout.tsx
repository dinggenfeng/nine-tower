import { LogoutOutlined, ProjectOutlined } from '@ant-design/icons';
import { Avatar, Dropdown, Layout, type MenuProps } from 'antd';
import { useNavigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import styles from './MainLayout.module.css';

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
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
            <Avatar size="small" style={{ backgroundColor: '#3b82f6' }}>
              {user?.username?.[0]?.toUpperCase()}
            </Avatar>
            <span className={styles.username}>{user?.username}</span>
          </div>
        </Dropdown>
      </Layout.Header>
      <Layout.Content className={styles.content}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
