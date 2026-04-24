import { useEffect, useState, useCallback } from "react";
import {
  Button,
  Checkbox,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
} from "antd";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { useParams } from "react-router-dom";
import type {
  HostGroup,
  Host,
  CreateHostGroupRequest,
  CreateHostRequest,
} from "../../types/entity/Host";
import {
  createHostGroup,
  deleteHostGroup,
  getHostGroups,
  updateHostGroup,
  createHost,
  deleteHost,
  getHosts,
  updateHost,
} from "../../api/host";
import styles from "./HostGroupManager.module.css";

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
    message.success("主机组创建成功");
    setHgModalOpen(false);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleUpdateHg = async () => {
    if (!editingHg) return;
    const values = await hgForm.validateFields();
    await updateHostGroup(editingHg.id, values);
    message.success("主机组更新成功");
    setHgModalOpen(false);
    setEditingHg(null);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleDeleteHg = async (hgId: number) => {
    await deleteHostGroup(hgId);
    message.success("主机组已删除");
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
    message.success("主机创建成功");
    setHostModalOpen(false);
    hostForm.resetFields();
    fetchHosts(selectedHostGroup.id);
  };

  const handleUpdateHost = async () => {
    if (!editingHost) return;
    const values = await hostForm.validateFields();
    await updateHost(editingHost.id, values);
    message.success("主机更新成功");
    setHostModalOpen(false);
    setEditingHost(null);
    hostForm.resetFields();
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const handleDeleteHost = async (hostId: number) => {
    await deleteHost(hostId);
    message.success("主机已删除");
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
    {
      title: "名称",
      dataIndex: "name",
      key: "name",
      render: (name: string) => <span style={{ fontWeight: 500 }}>{name}</span>,
    },
    {
      title: "IP",
      dataIndex: "ip",
      key: "ip",
      render: (ip: string) => <span className={styles.ipCell}>{ip}</span>,
    },
    { title: "端口", dataIndex: "port", key: "port" },
    { title: "SSH用户", dataIndex: "ansibleUser", key: "ansibleUser" },
    {
      title: "提权",
      dataIndex: "ansibleBecome",
      key: "ansibleBecome",
      render: (v: boolean) => <Tag color={v ? "green" : "default"}>{v ? "是" : "否"}</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_: unknown, record: Host) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => openHostModal(record)}
            />
          </Tooltip>
          <Popconfirm title="确认删除此主机？" onConfirm={() => handleDeleteHost(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      {/* Left: HostGroup list */}
      <div className={styles.leftPanel}>
        <div className={styles.leftHeader}>
          <span className={styles.leftTitle}>主机组</span>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => openHgModal()}
            aria-label="新建主机组"
          />
        </div>
        <div className={styles.groupList}>
          {hostGroups.length === 0 && !loading && (
            <Empty
              description="暂无主机组"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              style={{ marginTop: 40 }}
            />
          )}
          {hostGroups.map((hg) => {
            const isActive = selectedHostGroup?.id === hg.id;
            return (
              <div
                key={hg.id}
                className={`${styles.groupItem} ${isActive ? styles.groupItemActive : ""}`}
                onClick={() => handleSelectHostGroup(hg)}
              >
                <div>
                  <div className={styles.groupName}>{hg.name}</div>
                  <div className={styles.groupCount}>{hg.description || "无描述"}</div>
                </div>
                <Space className={styles.groupActions} onClick={(e) => e.stopPropagation()}>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => openHgModal(hg)}
                    style={{ color: "inherit" }}
                  />
                  <Popconfirm title="确认删除此主机组？" onConfirm={() => handleDeleteHg(hg.id)}>
                    <Button
                      type="text"
                      size="small"
                      icon={<DeleteOutlined />}
                      style={{ color: "inherit" }}
                    />
                  </Popconfirm>
                </Space>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right: Host list */}
      <div className={styles.rightPanel}>
        <div className={styles.rightHeader}>
          <div>
            <span className={styles.rightTitle}>
              {selectedHostGroup ? selectedHostGroup.name : "请选择主机组"}
            </span>
            {selectedHostGroup && (
              <span className={styles.rightSubtitle}>{hosts.length} 台主机</span>
            )}
          </div>
          {selectedHostGroup && (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => openHostModal()}
            >
              添加主机
            </Button>
          )}
        </div>
        <div style={{ flex: 1, overflow: "auto" }}>
          {selectedHostGroup ? (
            <Table
              columns={hostColumns}
              dataSource={hosts}
              rowKey="id"
              loading={loading}
              pagination={false}
              locale={{
                emptyText: (
                  <Empty description="该主机组下暂无主机" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ),
              }}
            />
          ) : (
            <Empty description="请从左侧选择一个主机组" style={{ marginTop: 80 }} />
          )}
        </div>
      </div>

      {/* HostGroup Modal */}
      <Modal
        title={editingHg ? "编辑主机组" : "新建主机组"}
        open={hgModalOpen}
        onOk={editingHg ? handleUpdateHg : handleCreateHg}
        onCancel={() => {
          setHgModalOpen(false);
          hgForm.resetFields();
        }}
      >
        <Form form={hgForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Host Modal */}
      <Modal
        title={editingHost ? "编辑主机" : "新建主机"}
        open={hostModalOpen}
        onOk={editingHost ? handleUpdateHost : handleCreateHost}
        onCancel={() => {
          setHostModalOpen(false);
          hostForm.resetFields();
        }}
      >
        <Form form={hostForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="ip" label="IP" rules={[{ required: true, message: "请输入IP" }]}>
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
            <Checkbox />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
