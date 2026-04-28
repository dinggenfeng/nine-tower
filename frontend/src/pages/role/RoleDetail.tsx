import { useEffect, useState } from "react";
import { Button, Card, Skeleton, Tabs } from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { useParams, useNavigate } from "react-router-dom";
import type { Role } from "../../types/entity/Role";
import { getRole } from "../../api/role";
import RoleTasks from "./RoleTasks";
import RoleHandlers from "./RoleHandlers";
import RoleTemplates from "./RoleTemplates";
import RoleFiles from "./RoleFiles";

export default function RoleDetail() {
  const { id, roleId } = useParams<{ id: string; roleId: string }>();
  const navigate = useNavigate();
  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (roleId) {
      getRole(Number(roleId)).then((r) => {
        setRole(r);
        setLoading(false);
      });
    }
  }, [roleId]);

  if (loading) {
    return <Skeleton active />;
  }

  const tabItems = [
    { key: "tasks", label: "Tasks", children: <RoleTasks roleId={Number(roleId)} /> },
    { key: "handlers", label: "Handlers", children: <RoleHandlers roleId={Number(roleId)} /> },
    { key: "templates", label: "Templates", children: <RoleTemplates roleId={Number(roleId)} /> },
    { key: "files", label: "Files", children: <RoleFiles roleId={Number(roleId)} /> },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(`/projects/${id}/roles`)}
          style={{ color: "#64748b", padding: "4px 8px" }}
        >
          返回 Roles
        </Button>
      </div>
      <Card
        style={{ marginBottom: 16 }}
        title={<span style={{ fontSize: 18, fontWeight: 600 }}>{role?.name}</span>}
      >
        <p style={{ color: "#64748b", margin: 0 }}>{role?.description || "无描述"}</p>
      </Card>
      <Card bodyStyle={{ padding: 0 }}>
        <Tabs defaultActiveKey="tasks" items={tabItems} style={{ padding: "0 24px" }} />
      </Card>
    </div>
  );
}
