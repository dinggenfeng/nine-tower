import { useEffect, useState, useCallback, useRef } from "react";
import { Table, Button, Modal, Form, Select, message, Popconfirm } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { useParams } from "react-router-dom";
import type { ProjectMember, AddMemberRequest } from "../../types/entity/Project";
import { getMembers, addMember, removeMember, updateMemberRole } from "../../api/project";
import { userApi } from "../../api/user";
import type { User } from "../../types/entity/User";
import { useProjectStore } from "../../stores/projectStore";
import { useAuthStore } from "../../stores/authStore";
import { colors } from "../../theme";
import PageHeader from "../../components/PageHeader";

export default function MemberManagement() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [adding, setAdding] = useState(false);
  const [form] = Form.useForm<AddMemberRequest>();
  const [userOptions, setUserOptions] = useState<User[]>([]);
  const [userSearchLoading, setUserSearchLoading] = useState(false);
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { currentProject } = useProjectStore();
  const isAdmin = currentProject?.myRole === "PROJECT_ADMIN";
  const currentUser = useAuthStore((s) => s.user);
  const adminCount = members.filter((m) => m.role === "PROJECT_ADMIN").length;

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
    setAdding(true);
    try {
      const values = await form.validateFields();
      await addMember(projectId, values);
      message.success("成员添加成功");
      setAddModalOpen(false);
      form.resetFields();
      fetchMembers();
    } catch (error: unknown) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      const msg =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (error as { message?: string })?.message ||
        "添加失败";
      message.error(msg);
    } finally {
      setAdding(false);
    }
  };

  const handleUserSearch = (keyword: string) => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    if (!keyword) {
      setUserOptions([]);
      return;
    }
    searchTimer.current = setTimeout(async () => {
      setUserSearchLoading(true);
      try {
        const result = await userApi.searchUsers(keyword);
        setUserOptions(result.content);
      } finally {
        setUserSearchLoading(false);
      }
    }, 300);
  };

  const handleRemove = async (userId: number) => {
    const target = members.find((m) => m.userId === userId);
    if (target?.role === "PROJECT_ADMIN" && adminCount <= 1) {
      message.warning("该成员是最后一个管理员，无法移除");
      return;
    }
    try {
      await removeMember(projectId, userId);
      message.success("成员已移除");
      fetchMembers();
    } catch (error: unknown) {
      const msg =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (error as { message?: string })?.message ||
        "移除失败";
      message.error(msg);
    }
  };

  const handleRoleChange = async (userId: number, role: "PROJECT_ADMIN" | "PROJECT_MEMBER") => {
    const target = members.find((m) => m.userId === userId);
    if (target?.role === "PROJECT_ADMIN" && role !== "PROJECT_ADMIN" && adminCount <= 1) {
      message.warning("该成员是最后一个管理员，无法降级");
      return;
    }
    try {
      await updateMemberRole(projectId, userId, { role });
      message.success("角色更新成功");
      fetchMembers();
    } catch (error: unknown) {
      const msg =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (error as { message?: string })?.message ||
        "角色更新失败";
      message.error(msg);
    }
  };

  const columns = [
    { title: "用户名", dataIndex: "username", key: "username" },
    { title: "邮箱", dataIndex: "email", key: "email" },
    {
      title: "角色",
      dataIndex: "role",
      key: "role",
      render: (role: string, record: ProjectMember) => {
        const isSelf = record.userId === currentUser?.id;
        const roleLabel = role === "PROJECT_ADMIN" ? "管理员" : "成员";
        if (!isAdmin || isSelf) {
          return (
            <span
              style={{
                background: role === "PROJECT_ADMIN" ? colors.tagAdminBg : colors.tagMemberBg,
                color: role === "PROJECT_ADMIN" ? colors.tagAdminColor : colors.tagMemberColor,
                fontSize: 12,
                padding: "2px 8px",
                borderRadius: 4,
                fontWeight: 500,
              }}
            >
              {roleLabel}
            </span>
          );
        }
        return (
          <Select
            value={role}
            onChange={(value) =>
              handleRoleChange(record.userId, value as "PROJECT_ADMIN" | "PROJECT_MEMBER")
            }
            options={[
              { value: "PROJECT_ADMIN", label: "管理员" },
              { value: "PROJECT_MEMBER", label: "成员" },
            ]}
            style={{ width: 120 }}
          />
        );
      },
    },
    {
      title: "加入时间",
      dataIndex: "joinedAt",
      key: "joinedAt",
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    ...(isAdmin
      ? [
          {
            title: "操作",
            key: "action",
            render: (_: unknown, record: ProjectMember) => {
              if (record.userId === currentUser?.id) return null;
              return (
                <Popconfirm title="确认移除此成员？" onConfirm={() => handleRemove(record.userId)}>
                  <Button type="link" danger size="small">
                    移除
                  </Button>
                </Popconfirm>
              );
            },
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
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddModalOpen(true)}>
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
        confirmLoading={adding}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="userId"
            label="选择用户"
            rules={[{ required: true, message: "请选择用户" }]}
          >
            <Select
              showSearch
              placeholder="搜索用户名"
              filterOption={false}
              onSearch={handleUserSearch}
              loading={userSearchLoading}
              notFoundContent="输入关键词搜索用户"
            >
              {userOptions.map((u) => (
                <Select.Option key={u.id} value={u.id}>
                  {u.username} ({u.email})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true, message: "请选择角色" }]}>
            <Select
              options={[
                { value: "PROJECT_ADMIN", label: "管理员" },
                { value: "PROJECT_MEMBER", label: "成员" },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
