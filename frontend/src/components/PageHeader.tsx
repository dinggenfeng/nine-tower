import { Typography } from "antd";
import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export default function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-start",
        marginBottom: 20,
        paddingBottom: 16,
        borderBottom: "1px solid #f1f5f9",
      }}
    >
      <div>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        {description && (
          <Typography.Text type="secondary" style={{ marginTop: 4, display: "block" }}>
            {description}
          </Typography.Text>
        )}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}
