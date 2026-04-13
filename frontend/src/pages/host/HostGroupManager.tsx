import { useEffect, useState, useCallback } from 'react';
import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  List,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'antd';
import {
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  HostGroup,
  Host,
  CreateHostGroupRequest,
  CreateHostRequest,
} from '../../types/entity/Host';
import {
  createHostGroup,
  deleteHostGroup,
  getHostGroups,
  updateHostGroup,
  createHost,
  deleteHost,
  getHosts,
  updateHost,
} from '../../api/host';

const { TextArea } = Input;

export default function HostGroupManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [selectedHostGroup, setSelectedHostGroup] = useState<HostGroup | null>(null);
  const [hosts, setHosts] = useState<Host[]>([]);
  const [loading, setLoading] = useState(false);
  const [hgModalOpen, setHgModalOpen] = useState(false);
  const [hostModalOpen, setHostModalOpen] = useState(false);
  const [editingHg, setEditingHg] = useState<HostGroup | null>(null);
  const [editingHost, setEditingHost] = useState<Host | null>(null);
  const [hgForm] = Form.useForm<CreateHostGroupRequest>();
  const [hostForm] = Form.useForm<CreateHostRequest>();

  const fetchHostGroups = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getHostGroups(pid);
      setHostGroups(data);
    } finally {
      setLoading(false);
    }
  }, [pid]);

  const fetchHosts = useCallback(async (hgId: number) => {
    setLoading(true);
    try {
      setHosts(await getHosts(hgId));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHostGroups();
  }, [fetchHostGroups]);

  const handleSelectHostGroup = (hg: HostGroup) => {
    setSelectedHostGroup(hg);
    fetchHosts(hg.id);
  };

  const handleCreateHg = async () => {
    const values = await hgForm.validateFields();
    await createHostGroup(pid, values);
    message.success('主机组创建成功');
    setHgModalOpen(false);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleUpdateHg = async () => {
    if (!editingHg) return;
    const values = await hgForm.validateFields();
    await updateHostGroup(editingHg.id, values);
    message.success('主机组更新成功');
    setHgModalOpen(false);
    setEditingHg(null);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleDeleteHg = async (hgId: number) => {
    await deleteHostGroup(hgId);
    message.success('主机组已删除');
    if (selectedHostGroup?.id === hgId) {
      setSelectedHostGroup(null);
      setHosts([]);
    }
    fetchHostGroups();
  };

  const openHgModal = (hg?: HostGroup) => {
    if (hg) {
      setEditingHg(hg);
      hgForm.setFieldsValue({ name: hg.name, description: hg.description });
    } else {
      setEditingHg(null);
      hgForm.resetFields();
    }
    setHgModalOpen(true);
  };

  const handleCreateHost = async () => {
    if (!selectedHostGroup) return;
    const values = await hostForm.validateFields();
    await createHost(selectedHostGroup.id, values);
    message.success('主机创建成功');
    setHostModalOpen(false);
    hostForm.resetFields();
    fetchHosts(selectedHostGroup.id);
  };

  const handleUpdateHost = async () => {
    if (!editingHost) return;
    const values = await hostForm.validateFields();
    await updateHost(editingHost.id, values);
    message.success('主机更新成功');
    setHostModalOpen(false);
    setEditingHost(null);
    hostForm.resetFields();
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const handleDeleteHost = async (hostId: number) => {
    await deleteHost(hostId);
    message.success('主机已删除');
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const openHostModal = (host?: Host) => {
    if (host) {
      setEditingHost(host);
      hostForm.setFieldsValue({
        name: host.name,
        ip: host.ip,
        port: host.port,
        ansibleUser: host.ansibleUser,
        ansibleBecome: host.ansibleBecome,
      });
    } else {
      setEditingHost(null);
      hostForm.resetFields();
    }
    setHostModalOpen(true);
  };

  const hostColumns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: 'SSH用户', dataIndex: 'ansibleUser', key: 'ansibleUser' },
    {
      title: '提权',
      dataIndex: 'ansibleBecome',
      key: 'ansibleBecome',
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '是' : '否'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Host) => (
        <Space>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openHostModal(record)} />
          </Tooltip>
          <Popconfirm title="确认删除此主机？" onConfirm={() => handleDeleteHost(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', gap: 16, height: 'calc(100vh - 180px)' }}>
      {/* Left: HostGroup list */}
      <Card
        title="主机组"
        extra={
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openHgModal()}>
            新建
          </Button>
        }
        style={{ width: 320, overflow: 'auto' }}
        bodyStyle={{ padding: 0 }}
      >
        <List
          loading={loading}
          dataSource={hostGroups}
          locale={{ emptyText: <Empty description="暂无主机组" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          renderItem={(hg) => (
            <List.Item
              style={{
                padding: '12px 16px',
                cursor: 'pointer',
                background: selectedHostGroup?.id === hg.id ? '#f0f5ff' : undefined,
                borderLeft: selectedHostGroup?.id === hg.id ? '2px solid #1677ff' : '2px solid transparent',
              }}
              onClick={() => handleSelectHostGroup(hg)}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <DatabaseOutlined />
                    <span>{hg.name}</span>
                  </Space>
                }
                description={hg.description || '无描述'}
              />
              <Space onClick={(e) => e.stopPropagation()}>
                <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openHgModal(hg)} />
                <Popconfirm title="确认删除此主机组？" onConfirm={() => handleDeleteHg(hg.id)}>
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            </List.Item>
          )}
        />
      </Card>

      {/* Right: Host list */}
      <Card
        title={selectedHostGroup ? `主机 — ${selectedHostGroup.name}` : '请选择主机组'}
        extra={
          selectedHostGroup && (
            <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openHostModal()}>
              新建主机
            </Button>
          )
        }
        style={{ flex: 1, overflow: 'auto' }}
        bodyStyle={{ padding: 0 }}
      >
        {selectedHostGroup ? (
          <Table
            columns={hostColumns}
            dataSource={hosts}
            rowKey="id"
            loading={loading}
            pagination={false}
            locale={{ emptyText: <Empty description="该主机组下暂无主机" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          />
        ) : (
          <Empty description="请从左侧选择一个主机组" style={{ marginTop: 80 }} />
        )}
      </Card>

      {/* HostGroup Modal */}
      <Modal
        title={editingHg ? '编辑主机组' : '新建主机组'}
        open={hgModalOpen}
        onOk={editingHg ? handleUpdateHg : handleCreateHg}
        onCancel={() => { setHgModalOpen(false); hgForm.resetFields(); }}
      >
        <Form form={hgForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Host Modal */}
      <Modal
        title={editingHost ? '编辑主机' : '新建主机'}
        open={hostModalOpen}
        onOk={editingHost ? handleUpdateHost : handleCreateHost}
        onCancel={() => { setHostModalOpen(false); hostForm.resetFields(); }}
      >
        <Form form={hostForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="ip" label="IP" rules={[{ required: true, message: '请输入IP' }]}>
            <Input maxLength={45} placeholder="192.168.1.10" />
          </Form.Item>
          <Form.Item name="port" label="端口" initialValue={22}>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="ansibleUser" label="SSH用户">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="ansibleSshPass" label="SSH密码（加密存储）">
            <Input.Password maxLength={500} placeholder="留空则不更新" />
          </Form.Item>
          <Form.Item name="ansibleBecome" label="提权" valuePropName="checked" initialValue={false}>
            <input type="checkbox" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
