import { useEffect, useState } from 'react';
import { Card, Skeleton, Tabs } from 'antd';
import { useParams } from 'react-router-dom';
import type { Role } from '../../types/entity/Role';
import { getRole } from '../../api/role';

const { TabPane } = Tabs;

export default function RoleDetail() {
  const { roleId } = useParams<{ id: string; roleId: string }>();
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

  return (
    <div>
      <Card title={role?.name} style={{ marginBottom: 16 }}>
        <p>{role?.description || '无描述'}</p>
      </Card>
      <Card bodyStyle={{ padding: 0 }}>
        <Tabs defaultActiveKey="tasks" style={{ padding: '0 24px' }}>
          <TabPane tab="Tasks" key="tasks">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Handlers" key="handlers">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Templates" key="templates">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Files" key="files">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Vars" key="vars">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Defaults" key="defaults">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
}
