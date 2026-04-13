import type { ThemeConfig } from 'antd';

export const theme: ThemeConfig = {
  token: {
    colorPrimary: '#3b82f6',
    colorBgContainer: '#ffffff',
    colorBgLayout: '#f1f5f9',
    borderRadius: 6,
    fontSize: 14,
    colorText: '#1e293b',
    colorTextSecondary: '#64748b',
    colorBorder: '#e2e8f0',
    colorSuccess: '#10b981',
    colorWarning: '#f59e0b',
    colorError: '#ef4444',
  },
};

/** Colors not covered by Ant Design tokens — use in CSS Modules and inline styles */
export const colors = {
  headerBg: '#0f172a',
  siderBg: '#1e293b',
  siderItemActiveBg: '#3b82f6',
  siderTextColor: '#94a3b8',
  siderTextActiveColor: '#f1f5f9',
  siderGroupLabel: '#475569',
  tagAdminBg: '#1e3a5f',
  tagAdminColor: '#3b82f6',
  tagMemberBg: '#1e293b',
  tagMemberColor: '#94a3b8',
} as const;
