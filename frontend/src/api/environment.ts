import request from './request';
import type {
  Environment,
  CreateEnvironmentRequest,
  UpdateEnvironmentRequest,
  EnvConfigRequest,
  EnvConfig,
} from '../types/entity/Environment';

export async function createEnvironment(
  projectId: number,
  data: CreateEnvironmentRequest
): Promise<Environment> {
  const res = await request.post<Environment>(
    `/projects/${projectId}/environments`,
    data
  );
  return res.data;
}

export async function listEnvironments(
  projectId: number
): Promise<Environment[]> {
  const res = await request.get<Environment[]>(
    `/projects/${projectId}/environments`
  );
  return res.data;
}

export async function getEnvironment(envId: number): Promise<Environment> {
  const res = await request.get<Environment>(`/environments/${envId}`);
  return res.data;
}

export async function updateEnvironment(
  envId: number,
  data: UpdateEnvironmentRequest
): Promise<Environment> {
  const res = await request.put<Environment>(`/environments/${envId}`, data);
  return res.data;
}

export async function deleteEnvironment(envId: number): Promise<void> {
  await request.delete(`/environments/${envId}`);
}

export async function addConfig(
  envId: number,
  data: EnvConfigRequest
): Promise<EnvConfig> {
  const res = await request.post<EnvConfig>(
    `/environments/${envId}/configs`,
    data
  );
  return res.data;
}

export async function removeConfig(configId: number): Promise<void> {
  await request.delete(`/env-configs/${configId}`);
}
