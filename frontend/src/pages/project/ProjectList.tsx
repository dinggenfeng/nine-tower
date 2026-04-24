import { useEffect, useState } from "react";
import { Button, Modal, Form, Input, message, Dropdown, Spin, type MenuProps } from "antd";
import {
  PlusOutlined,
  DeleteOutlined,
  SettingOutlined,
  MoreOutlined,
  FolderOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { Project, CreateProjectRequest } from "../../types/entity/Project";
import { getMyProjects, createProject, deleteProject } from "../../api/project";
import styles from "./ProjectList.module.css";

const { TextArea } = Input;

const avatarGradients = [
  "linear-gradient(135deg, #3b82f6, #1d4ed8)",
  "linear-gradient(135deg, #6366f1, #4f46e5)",
  "linear-gradient(135deg, #8b5cf6, #7c3aed)",
  "linear-gradient(135deg, #06b6d4, #0891b2)",
  "linear-gradient(135deg, #10b981, #059669)",
  "linear-gradient(135deg, #f59e0b, #d97706)",
  "linear-gradient(135deg, #ef4444, #dc2626)",
  "linear-gradient(135deg, #ec4899, #db2777)",
  "linear-gradient(135deg, #64748b, #475569)",
  "linear-gradient(135deg, #0ea5e9, #0284c7)",
];

function getAvatarStyle(name: string) {
  const code = name.charCodeAt(0) || 0;
  return { background: avatarGradients[code % avatarGradients.length] };
}

function formatDate(dateStr: string) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default function ProjectList() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<CreateProjectRequest>();
  const navigate = useNavigate();

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const data = await getMyProjects();
      setProjects(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreate = async () => {
    setCreating(true);
    try {
      const values = await form.validateFields();
      await createProject(values);
      message.success("项目创建成功");
      setCreateModalOpen(false);
      form.resetFields();
      fetchProjects();
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: "确认删除项目？",
      content: "此操作无法撤销。",
      onOk: async () => {
        await deleteProject(id);
        message.success("项目已删除");
        fetchProjects();
      },
    });
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <h2 className={styles.title}>我的项目</h2>
          <span className={styles.count}>共 {projects.length} 个项目</span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
          新建项目
        </Button>
      </div>

      {loading ? (
        <div style={{ textAlign: "center", padding: "80px 0" }}>
          <Spin size="large" />
        </div>
      ) : projects.length === 0 ? (
        <div className={styles.emptyWrap}>
          <div className={styles.emptyIconWrap}>
            <FolderOutlined />
          </div>
          <div className={styles.emptyTitle}>暂无项目</div>
          <div className={styles.emptyDesc}>创建你的第一个 Ansible Playbook 项目</div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
            创建第一个项目
          </Button>
        </div>
      ) : (
        <div className={styles.grid}>
          {projects.map((project, index) => {
            const isAdmin = project.myRole === "PROJECT_ADMIN";
            const initial = project.name[0]?.toUpperCase() || "P";

            const menuItems: MenuProps["items"] = [
              ...(isAdmin
                ? [
                    {
                      key: "settings",
                      icon: <SettingOutlined />,
                      label: "项目设置",
                      onClick: () => navigate(`/projects/${project.id}/settings`),
                    },
                    {
                      key: "delete",
                      icon: <DeleteOutlined />,
                      label: "删除项目",
                      danger: true,
                      onClick: () => handleDelete(project.id),
                    },
                  ]
                : []),
            ];

            return (
              <div
                key={project.id}
                className={`${styles.card} ${styles.cardAnimated}`}
                style={{ animationDelay: `${index * 0.05}s` }}
                role="button"
                tabIndex={0}
                onClick={() => navigate(`/projects/${project.id}/roles`)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") navigate(`/projects/${project.id}/roles`);
                }}
              >
                <div className={styles.cardTop}>
                  <div className={styles.avatar} style={getAvatarStyle(project.name)}>
                    {initial}
                  </div>
                  <div className={styles.cardActions}>
                    <span
                      className={`${styles.roleBadge} ${isAdmin ? styles.roleAdmin : styles.roleMember}`}
                    >
                      {isAdmin ? "管理员" : "成员"}
                    </span>
                    {menuItems.length > 0 && (
                      <div onClick={(e) => e.stopPropagation()}>
                        <Dropdown
                          menu={{ items: menuItems }}
                          trigger={["click"]}
                          placement="bottomRight"
                        >
                          <span className={styles.menuBtn}>
                            <MoreOutlined />
                          </span>
                        </Dropdown>
                      </div>
                    )}
                  </div>
                </div>
                <h3 className={styles.cardName}>{project.name}</h3>
                <div className={styles.cardDesc}>{project.description || "暂无描述"}</div>
                <div className={styles.cardFooter}>
                  <ClockCircleOutlined style={{ fontSize: 12 }} />
                  <span style={{ marginLeft: 4 }}>{formatDate(project.createdAt)}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Modal
        title="创建项目"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => {
          setCreateModalOpen(false);
          form.resetFields();
        }}
        confirmLoading={creating}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="项目名称"
            rules={[{ required: true, message: "请输入项目名称" }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
