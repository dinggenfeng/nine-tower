export interface Playbook {
  id: number;
  projectId: number;
  name: string;
  description: string;
  extraVars: string;
  roleIds: number[];
  hostGroupIds: number[];
  tagIds: number[];
  environmentIds: number[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlaybookRequest {
  name: string;
  description?: string;
  extraVars?: string;
}

export interface UpdatePlaybookRequest {
  name: string;
  description?: string;
  extraVars?: string;
}
