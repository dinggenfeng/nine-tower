import request from "./request";
import type {
  Variable,
  CreateVariableRequest,
  UpdateVariableRequest,
  VariableScope,
  DetectedVariable,
  BatchVariableSaveItem,
} from "../types/entity/Variable";

export async function createVariable(
  projectId: number,
  data: CreateVariableRequest
): Promise<Variable> {
  const res = await request.post<Variable>(`/projects/${projectId}/variables`, data);
  return res.data;
}

export async function listVariables(
  projectId: number,
  scope?: VariableScope,
  scopeId?: number
): Promise<Variable[]> {
  const params: Record<string, string | number> = {};
  if (scope != null) {
    params.scope = scope;
  }
  if (scopeId != null) {
    params.scopeId = scopeId;
  }
  const res = await request.get<Variable[]>(`/projects/${projectId}/variables`, { params });
  return res.data;
}

export async function getVariable(varId: number): Promise<Variable> {
  const res = await request.get<Variable>(`/variables/${varId}`);
  return res.data;
}

export async function updateVariable(
  varId: number,
  data: UpdateVariableRequest
): Promise<Variable> {
  const res = await request.put<Variable>(`/variables/${varId}`, data);
  return res.data;
}

export async function deleteVariable(varId: number): Promise<void> {
  await request.delete(`/variables/${varId}`);
}

export async function detectVariables(projectId: number): Promise<DetectedVariable[]> {
  const res = await request.get<DetectedVariable[]>(`/projects/${projectId}/detect-variables`);
  return res.data;
}

export async function batchSaveVariables(
  projectId: number,
  items: BatchVariableSaveItem[]
): Promise<Array<{ index: number; success: boolean; key?: string; error?: string }>> {
  const res = await request.post<
    Array<{ index: number; success: boolean; key?: string; error?: string }>
  >(`/projects/${projectId}/variables/batch`, items);
  return res.data;
}
