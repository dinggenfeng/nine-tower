export interface RoleVariable {
  id: number;
  roleId: number;
  key: string;
  value: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateRoleVariableRequest {
  key: string;
  value?: string;
}

export interface UpdateRoleVariableRequest {
  key?: string;
  value?: string;
}

export interface RoleDefaultVariable {
  id: number;
  roleId: number;
  key: string;
  value: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateRoleDefaultVariableRequest {
  key: string;
  value?: string;
}

export interface UpdateRoleDefaultVariableRequest {
  key?: string;
  value?: string;
}
