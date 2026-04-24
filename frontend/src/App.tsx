import { lazy, Suspense } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import MainLayout from "./components/Layout/MainLayout";
import ProjectLayout from "./components/Layout/ProjectLayout";
import { useAuthStore } from "./stores/authStore";
import { setNavigate } from "./api/navigate";
import { Spin } from "antd";

const Login = lazy(() => import("./pages/auth/Login"));
const Register = lazy(() => import("./pages/auth/Register"));
const ProjectList = lazy(() => import("./pages/project/ProjectList"));
const ProjectSettings = lazy(() => import("./pages/project/ProjectSettings"));
const MemberManagement = lazy(() => import("./pages/project/MemberManagement"));
const HostGroupManager = lazy(() => import("./pages/host/HostGroupManager"));
const RoleList = lazy(() => import("./pages/role/RoleList"));
const RoleDetail = lazy(() => import("./pages/role/RoleDetail"));
const TagManager = lazy(() => import("./pages/tag/TagManager"));
const EnvironmentManager = lazy(() => import("./pages/environment/EnvironmentManager"));
const VariableManager = lazy(() => import("./pages/variable/VariableManager"));
const PlaybookList = lazy(() => import("./pages/playbook/PlaybookList"));
const PlaybookBuilder = lazy(() => import("./pages/playbook/PlaybookBuilder"));

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function PageLoader() {
  return (
    <div style={{ display: "flex", justifyContent: "center", padding: "100px 0" }}>
      <Spin size="large" />
    </div>
  );
}

export default function App() {
  const navigate = useNavigate();
  setNavigate(navigate);

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <MainLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/projects" replace />} />
          <Route path="projects" element={<ProjectList />} />
          <Route path="projects/:id" element={<ProjectLayout />}>
            <Route path="settings" element={<ProjectSettings />} />
            <Route path="members" element={<MemberManagement />} />
            <Route path="host-groups" element={<HostGroupManager />} />
            <Route path="roles" element={<RoleList />} />
            <Route path="roles/:roleId" element={<RoleDetail />} />
            <Route path="tags" element={<TagManager />} />
            <Route path="environments" element={<EnvironmentManager />} />
            <Route path="variables" element={<VariableManager />} />
            <Route path="playbooks" element={<PlaybookList />} />
            <Route path="playbooks/:pbId" element={<PlaybookBuilder />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
