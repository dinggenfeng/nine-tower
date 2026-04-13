import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Input, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, LoginPayload } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import styles from './Login.module.css';

export default function Login() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<LoginPayload>();

  const onFinish = async (values: LoginPayload) => {
    try {
      const res = await authApi.login(values);
      login(res.data.token, res.data.user);
      message.success('登录成功');
      navigate('/projects');
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? '登录失败');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.brandSection}>
        <div className={styles.brandContent}>
          <div className={styles.brandLabel}>Ansible</div>
          <div className={styles.brandTitle}>Playbook Studio</div>
          <div className={styles.brandDescription}>
            可视化开发和管理你的 Ansible Playbook
          </div>
          <div className={styles.brandTags}>
            <span className={styles.brandTag}>项目管理</span>
            <span className={styles.brandTag}>主机管理</span>
            <span className={styles.brandTag}>角色编排</span>
            <span className={styles.brandTag}>剧本开发</span>
          </div>
        </div>
      </div>
      <div className={styles.formSection}>
        <div className={styles.formWrapper}>
          <div className={styles.formTitle}>欢迎回来</div>
          <div className={styles.formSubtitle}>登录以继续</div>
          <Form form={form} onFinish={onFinish} layout="vertical" size="large">
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                登 录
              </Button>
            </Form.Item>
          </Form>
          <div className={styles.formFooter}>
            还没有账号？<Link to="/register">立即注册</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
