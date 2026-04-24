import request from "./request";
import type {
  RoleVariable,
  CreateRoleVariableRequest,
  UpdateRoleVariableRequest,
  RoleDefaultVariable,
  CreateRoleDefaultVariableRequest,
  UpdateRoleDefaultVariableRequest,
} from "../types/entity/RoleVariable";

// Role Variable APIs
export async function createRoleVariable(
  roleId: number,
  data: CreateRoleVariableRequest
): Promise<RoleVariable> {
  const res = await request.post<RoleVariable>(`/roles/${roleId}/vars`, data);
  return res.data;
}

export async function getRoleVariables(roleId: number): Promise<RoleVariable[]> {
  const res = await request.get<RoleVariable[]>(`/roles/${roleId}/vars`);
  return res.data;
}

export async function updateRoleVariable(
  id: number,
  data: UpdateRoleVariableRequest
): Promise<RoleVariable> {
  const res = await request.put<RoleVariable>(`/role-vars/${id}`, data);
  return res.data;
}

export async function deleteRoleVariable(id: number): Promise<void> {
  await request.delete(`/role-vars/${id}`);
}

// Role Default Variable APIs
export async function createRoleDefault(
  roleId: number,
  data: CreateRoleDefaultVariableRequest
): Promise<RoleDefaultVariable> {
  const res = await request.post<RoleDefaultVariable>(`/roles/${roleId}/defaults`, data);
  return res.data;
}

export async function getRoleDefaults(roleId: number): Promise<RoleDefaultVariable[]> {
  const res = await request.get<RoleDefaultVariable[]>(`/roles/${roleId}/defaults`);
  return res.data;
}

export async function updateRoleDefault(
  id: number,
  data: UpdateRoleDefaultVariableRequest
): Promise<RoleDefaultVariable> {
  const res = await request.put<RoleDefaultVariable>(`/role-defaults/${id}`, data);
  return res.data;
}

export async function deleteRoleDefault(id: number): Promise<void> {
  await request.delete(`/role-defaults/${id}`);
}
