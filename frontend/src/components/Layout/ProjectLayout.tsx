import { useEffect } from "react";
import { Layout } from "antd";
import {
  TeamOutlined,
  SettingOutlined,
  DatabaseOutlined,
  TagsOutlined,
  CloudOutlined,
  CodeOutlined,
  PlayCircleOutlined,
  AppstoreOutlined,
} from "@ant-design/icons";
import { Outlet, useNavigate, useParams, useLocation } from "react-router-dom";
import { useProjectStore } from "../../stores/projectStore";
import { getProject } from "../../api/project";
import styles from "./ProjectLayout.module.css";

const navGroups = [
  {
    label: "导航",
    items: [
      { key: "roles", icon: <CodeOutlined />, label: "Roles" },
      { key: "host-groups", icon: <DatabaseOutlined />, label: "主机组" },
      { key: "variables", icon: <AppstoreOutlined />, label: "变量" },
      { key: "environments", icon: <CloudOutlined />, label: "环境" },
      { key: "tags", icon: <TagsOutlined />, label: "标签" },
      { key: "playbooks", icon: <PlayCircleOutlined />, label: "剧本" },
    ],
  },
  {
    label: "管理",
    items: [
      { key: "members", icon: <TeamOutlined />, label: "成员" },
      { key: "settings", icon: <SettingOutlined />, label: "设置" },
    ],
  },
];

const keyToLabel: Record<string, string> = {};
const allNavKeys = new Set<string>();
navGroups.forEach((g) =>
  g.items.forEach((item) => {
    keyToLabel[item.key] = item.label;
    allNavKeys.add(item.key);
  })
);

export default function ProjectLayout() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (id) {
      getProject(Number(id)).then(setCurrentProject);
    }
    return () => setCurrentProject(null);
  }, [id, setCurrentProject]);

  const pathSegments = location.pathname.split("/").filter(Boolean);
  // Derive currentKey by matching path segments against known nav keys
  // Path structure: /projects/:id/:navKey/...
  const currentKey =
    pathSegments.length >= 3 && allNavKeys.has(pathSegments[2])
      ? pathSegments[2]
      : "roles";

  return (
    <Layout style={{ minHeight: "100%" }}>
      <Layout.Sider width={200} className={styles.sider}>
        <div className={styles.siderHeader}>{currentProject?.name || "加载中..."}</div>
        {navGroups.map((group) => (
          <div key={group.label}>
            <div className={styles.groupLabel}>{group.label}</div>
            <div className={styles.navList}>
              {group.items.map((item) => (
                <div
                  key={item.key}
                  className={`${styles.navItem} ${currentKey === item.key ? styles.navItemActive : ""}`}
                  role="button"
                  tabIndex={0}
                  onClick={() => navigate(`/projects/${id}/${item.key}`)}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") navigate(`/projects/${id}/${item.key}`); }}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </Layout.Sider>
      <Layout.Content className={styles.content}>
        <div className={styles.breadcrumb}>
          <span
            className={styles.breadcrumbLink}
            role="button"
            tabIndex={0}
            onClick={() => navigate("/projects")}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") navigate("/projects"); }}
          >
            项目
          </span>
          {" / "}
          <span
            className={styles.breadcrumbLink}
            role="button"
            tabIndex={0}
            onClick={() => navigate(`/projects/${id}/roles`)}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") navigate(`/projects/${id}/roles`); }}
          >
            {currentProject?.name || "..."}
          </span>
          {" / "}
          <span className={styles.breadcrumbCurrent}>{keyToLabel[currentKey] || currentKey}</span>
        </div>
        <div className={styles.contentCard}>
          <Outlet />
        </div>
      </Layout.Content>
    </Layout>
  );
}
