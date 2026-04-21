import { Navigate, Route, Routes } from "react-router-dom";
import MainLayout from "./components/Layout/MainLayout";
import ProjectLayout from "./components/Layout/ProjectLayout";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import ProjectList from "./pages/project/ProjectList";
import ProjectSettings from "./pages/project/ProjectSettings";
import MemberManagement from "./pages/project/MemberManagement";
import HostGroupManager from "./pages/host/HostGroupManager";
import RoleList from "./pages/role/RoleList";
import RoleDetail from "./pages/role/RoleDetail";
import EnvironmentManager from "./pages/environment/EnvironmentManager";
import { useAuthStore } from "./stores/authStore";

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
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
          <Route path="environments" element={<EnvironmentManager />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
