import { LogoutOutlined, ProjectOutlined } from "@ant-design/icons";
import { Avatar, Dropdown, Layout, MenuProps, Space, Typography } from "antd";
import { useNavigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";

const { Header, Content } = Layout;

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const userMenuItems: MenuProps["items"] = [
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "Logout",
      onClick: () => {
        logout();
        navigate("/login");
      },
    },
  ];

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          background: "#fff",
          padding: "0 24px",
          borderBottom: "1px solid #f0f0f0",
        }}
      >
        <Space
          style={{ cursor: "pointer" }}
          onClick={() => navigate("/projects")}
        >
          <ProjectOutlined style={{ fontSize: 20 }} />
          <Typography.Text strong style={{ fontSize: 16 }}>
            Ansible Playbook System
          </Typography.Text>
        </Space>
        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space style={{ cursor: "pointer" }}>
            <Avatar>{user?.username?.[0]?.toUpperCase()}</Avatar>
            <Typography.Text>{user?.username}</Typography.Text>
          </Space>
        </Dropdown>
      </Header>
      <Content style={{ padding: "24px", background: "#f0f2f5" }}>
        <Outlet />
      </Content>
    </Layout>
  );
}
