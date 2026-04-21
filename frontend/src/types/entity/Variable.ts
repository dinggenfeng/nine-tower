export type VariableScope = 'PROJECT' | 'HOSTGROUP' | 'ENVIRONMENT';

export interface Variable {
  id: number;
  scope: VariableScope;
  scopeId: number;
  key: string;
  value: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVariableRequest {
  scope: VariableScope;
  scopeId: number;
  key: string;
  value?: string;
}

export interface UpdateVariableRequest {
  key: string;
  value?: string;
}
