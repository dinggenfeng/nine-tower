import request from './request';
import type {
  HostGroup,
  Host,
  CreateHostGroupRequest,
  UpdateHostGroupRequest,
  CreateHostRequest,
  UpdateHostRequest,
} from '../types/entity/Host';

// HostGroup APIs
export async function createHostGroup(
  projectId: number,
  data: CreateHostGroupRequest
): Promise<HostGroup> {
  const res = await request.post<HostGroup>(
    `/api/projects/${projectId}/host-groups`,
    data
  );
  return res.data;
}

export async function getHostGroups(projectId: number): Promise<HostGroup[]> {
  const res = await request.get<HostGroup[]>(
    `/api/projects/${projectId}/host-groups`
  );
  return res.data;
}

export async function getHostGroup(id: number): Promise<HostGroup> {
  const res = await request.get<HostGroup>(`/api/host-groups/${id}`);
  return res.data;
}

export async function updateHostGroup(
  id: number,
  data: UpdateHostGroupRequest
): Promise<HostGroup> {
  const res = await request.put<HostGroup>(`/api/host-groups/${id}`, data);
  return res.data;
}

export async function deleteHostGroup(id: number): Promise<void> {
  await request.delete(`/api/host-groups/${id}`);
}

// Host APIs
export async function createHost(
  hostGroupId: number,
  data: CreateHostRequest
): Promise<Host> {
  const res = await request.post<Host>(
    `/api/host-groups/${hostGroupId}/hosts`,
    data
  );
  return res.data;
}

export async function getHosts(hostGroupId: number): Promise<Host[]> {
  const res = await request.get<Host[]>(
    `/api/host-groups/${hostGroupId}/hosts`
  );
  return res.data;
}

export async function getHost(id: number): Promise<Host> {
  const res = await request.get<Host>(`/api/hosts/${id}`);
  return res.data;
}

export async function updateHost(
  id: number,
  data: UpdateHostRequest
): Promise<Host> {
  const res = await request.put<Host>(`/api/hosts/${id}`, data);
  return res.data;
}

export async function deleteHost(id: number): Promise<void> {
  await request.delete(`/api/hosts/${id}`);
}
