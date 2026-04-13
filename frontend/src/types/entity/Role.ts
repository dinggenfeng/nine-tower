export interface Role {
  id: number;
  projectId: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
}
