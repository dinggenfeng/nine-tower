import request from "./request";
import type { Role, CreateRoleRequest, UpdateRoleRequest } from "../types/entity/Role";

export async function createRole(projectId: number, data: CreateRoleRequest): Promise<Role> {
  const res = await request.post<Role>(`/projects/${projectId}/roles`, data);
  return res.data;
}

export async function getRoles(projectId: number): Promise<Role[]> {
  const res = await request.get<Role[]>(`/projects/${projectId}/roles`);
  return res.data;
}

export async function getRole(id: number): Promise<Role> {
  const res = await request.get<Role>(`/roles/${id}`);
  return res.data;
}

export async function updateRole(id: number, data: UpdateRoleRequest): Promise<Role> {
  const res = await request.put<Role>(`/roles/${id}`, data);
  return res.data;
}

export async function deleteRole(id: number): Promise<void> {
  await request.delete(`/roles/${id}`);
}
