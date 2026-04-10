import { useEffect } from 'react';
import { Layout, Menu } from 'antd';
import {
  TeamOutlined,
  AppstoreOutlined,
  SettingOutlined,
  DatabaseOutlined,
  TagsOutlined,
  CloudOutlined,
  CodeOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useParams, useLocation } from 'react-router-dom';
import { useProjectStore } from '../../stores/projectStore';
import { getProject } from '../../api/project';

const { Sider, Content } = Layout;

export default function ProjectLayout() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (id) {
      getProject(Number(id)).then(setCurrentProject);
    }
    return () => setCurrentProject(null);
  }, [id, setCurrentProject]);

  const menuItems = [
    { key: 'roles', icon: <CodeOutlined />, label: 'Roles' },
    { key: 'host-groups', icon: <DatabaseOutlined />, label: 'Host Groups' },
    { key: 'variables', icon: <AppstoreOutlined />, label: 'Variables' },
    { key: 'environments', icon: <CloudOutlined />, label: 'Environments' },
    { key: 'tags', icon: <TagsOutlined />, label: 'Tags' },
    { key: 'playbooks', icon: <PlayCircleOutlined />, label: 'Playbooks' },
    { key: 'members', icon: <TeamOutlined />, label: 'Members' },
    { key: 'settings', icon: <SettingOutlined />, label: 'Settings' },
  ];

  const currentKey = location.pathname.split('/').pop() || 'roles';

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Sider width={200} style={{ background: '#fff' }}>
        <div style={{ padding: '16px', fontWeight: 'bold', fontSize: 16 }}>
          {currentProject?.name || 'Loading...'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[currentKey]}
          items={menuItems}
          onClick={({ key }) => navigate(`/projects/${id}/${key}`)}
        />
      </Sider>
      <Content style={{ padding: 24, background: '#fff', margin: 0 }}>
        <Outlet />
      </Content>
    </Layout>
  );
}
