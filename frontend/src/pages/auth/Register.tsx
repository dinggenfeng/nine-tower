import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Input, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, RegisterPayload } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import styles from './Login.module.css';

export default function Register() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm<RegisterPayload>();

  const onFinish = async (values: RegisterPayload) => {
    try {
      const res = await authApi.register(values);
      login(res.data.token, res.data.user);
      message.success('注册成功');
      navigate('/projects');
    } catch (err: unknown) {
      const error = err as { message?: string };
      message.error(error?.message ?? '注册失败');
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
          <div className={styles.formTitle}>创建账号</div>
          <div className={styles.formSubtitle}>注册新账号以开始使用</div>
          <Form form={form} onFinish={onFinish} layout="vertical" size="large">
            <Form.Item
              name="username"
              rules={[
                { required: true, message: '请输入用户名' },
                { min: 3, message: '用户名至少3个字符' },
                { max: 50, message: '用户名最多50个字符' },
              ]}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" />
            </Form.Item>
            <Form.Item
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱' },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="邮箱" />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码至少8个字符' },
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                注 册
              </Button>
            </Form.Item>
          </Form>
          <div className={styles.formFooter}>
            已有账号？<Link to="/login">登录</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
