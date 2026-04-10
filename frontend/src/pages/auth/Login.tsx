import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Card, Form, Input, message, Typography } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { authApi, LoginPayload } from "../../api/auth";
import { useAuthStore } from "../../stores/authStore";

const { Title } = Typography;

export default function Login() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<LoginPayload>();

  const onFinish = async (values: LoginPayload) => {
    try {
      const res = await authApi.login(values);
      login(res.data.token, res.data.user);
      message.success("Login successful");
      navigate("/projects");
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? "Login failed");
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f0f2f5",
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={2} style={{ textAlign: "center", marginBottom: 32 }}>
          Ansible Playbook System
        </Title>
        <Form form={form} onFinish={onFinish} layout="vertical" size="large">
          <Form.Item
            name="username"
            rules={[{ required: true, message: "Please enter your username" }]}
          >
            <Input prefix={<UserOutlined />} placeholder="Username" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: "Please enter your password" }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Login
            </Button>
          </Form.Item>
          <div style={{ textAlign: "center" }}>
            Don&apos;t have an account? <Link to="/register">Register</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}
