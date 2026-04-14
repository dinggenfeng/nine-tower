import { useEffect, useState } from 'react';
import { Button, Card, Empty, Skeleton, Tabs } from 'antd';
import { ArrowLeftOutlined, InboxOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import type { Role } from '../../types/entity/Role';
import { getRole } from '../../api/role';
import RoleTasks from './RoleTasks';
import RoleHandlers from './RoleHandlers';

function ComingSoon() {
  return (
    <Empty
      image={<InboxOutlined style={{ fontSize: 48, color: '#94a3b8' }} />}
      description={
        <span style={{ color: '#64748b' }}>即将推出</span>
      }
      style={{ padding: '48px 0' }}
    />
  );
}

export default function RoleDetail() {
  const { id, roleId } = useParams<{ id: string; roleId: string }>();
  const navigate = useNavigate();
  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (roleId) {
      getRole(Number(roleId)).then((r) => {
        setRole(r);
        setLoading(false);
      });
    }
  }, [roleId]);

  if (loading) {
    return <Skeleton active />;
  }

  const tabItems = [
    { key: 'tasks', label: 'Tasks', children: <RoleTasks roleId={Number(roleId)} /> },
    { key: 'handlers', label: 'Handlers', children: <RoleHandlers roleId={Number(roleId)} /> },
    { key: 'templates', label: 'Templates', children: <ComingSoon /> },
    { key: 'files', label: 'Files', children: <ComingSoon /> },
    { key: 'vars', label: 'Vars', children: <ComingSoon /> },
    { key: 'defaults', label: 'Defaults', children: <ComingSoon /> },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(`/projects/${id}/roles`)}
          style={{ color: '#64748b', padding: '4px 8px' }}
        >
          返回 Roles
        </Button>
      </div>
      <Card
        style={{ marginBottom: 16 }}
        title={
          <span style={{ fontSize: 18, fontWeight: 600 }}>{role?.name}</span>
        }
      >
        <p style={{ color: '#64748b', margin: 0 }}>
          {role?.description || '无描述'}
        </p>
      </Card>
      <Card bodyStyle={{ padding: 0 }}>
        <Tabs
          defaultActiveKey="tasks"
          items={tabItems}
          style={{ padding: '0 24px' }}
        />
      </Card>
    </div>
  );
}
