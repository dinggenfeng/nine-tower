export type VariableScope = "PROJECT" | "HOSTGROUP" | "ENVIRONMENT" | "ROLE_VARS" | "ROLE_DEFAULTS";

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

export interface VariableOccurrence {
  roleId: number;
  roleName: string;
  type: "TASK" | "HANDLER" | "TEMPLATE";
  entityId: number;
  entityName: string;
  field: string;
}

export interface DetectedVariable {
  key: string;
  occurrences: VariableOccurrence[];
  suggestedScope: "ROLE_VARS" | "PROJECT";
}

export interface DetectedVariableRow extends DetectedVariable {
  scope: VariableScope;
  scopeId?: number;
  userValue: string;
  rowKey: string;
}

export interface BatchVariableSaveItem {
  key: string;
  scope: VariableScope;
  scopeId?: number;
  value?: string;
}
