export type VariableScope = "PROJECT" | "HOSTGROUP" | "ENVIRONMENT";

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
  suggestedScope: "ROLE" | "PROJECT";
}

export interface DetectedVariableRow extends DetectedVariable {
  /** The user-selected scope target */
  scopeType: "project" | "role";
  /** Role ID when scopeType is "role" */
  targetRoleId?: number;
  /** Value filled by user */
  userValue: string;
  /** Unique key for table row */
  rowKey: string;
}

export interface BatchVariableSaveItem {
  key: string;
  saveAs: "VARIABLE" | "ROLE_VARIABLE";
  scope?: string;
  roleId?: number;
  value?: string;
}
