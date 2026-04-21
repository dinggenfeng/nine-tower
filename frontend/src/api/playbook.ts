import request from './request';
import type {
  Playbook,
  CreatePlaybookRequest,
  UpdatePlaybookRequest,
} from '../types/entity/Playbook';

export async function listPlaybooks(projectId: number): Promise<Playbook[]> {
  const res = await request.get<Playbook[]>(
    `/projects/${projectId}/playbooks`
  );
  return res.data;
}

export async function getPlaybook(playbookId: number): Promise<Playbook> {
  const res = await request.get<Playbook>(`/playbooks/${playbookId}`);
  return res.data;
}

export async function createPlaybook(
  projectId: number,
  data: CreatePlaybookRequest
): Promise<Playbook> {
  const res = await request.post<Playbook>(
    `/projects/${projectId}/playbooks`,
    data
  );
  return res.data;
}

export async function updatePlaybook(
  playbookId: number,
  data: UpdatePlaybookRequest
): Promise<Playbook> {
  const res = await request.put<Playbook>(`/playbooks/${playbookId}`, data);
  return res.data;
}

export async function deletePlaybook(playbookId: number): Promise<void> {
  await request.delete(`/playbooks/${playbookId}`);
}

export async function addRole(
  playbookId: number,
  roleId: number
): Promise<void> {
  await request.post(`/playbooks/${playbookId}/roles`, { roleId });
}

export async function removeRole(
  playbookId: number,
  roleId: number
): Promise<void> {
  await request.delete(`/playbooks/${playbookId}/roles/${roleId}`);
}

export async function reorderRoles(
  playbookId: number,
  roleIds: number[]
): Promise<void> {
  await request.put(`/playbooks/${playbookId}/roles/order`, roleIds);
}

export async function addHostGroup(
  playbookId: number,
  hostGroupId: number
): Promise<void> {
  await request.post(`/playbooks/${playbookId}/host-groups/${hostGroupId}`);
}

export async function removeHostGroup(
  playbookId: number,
  hostGroupId: number
): Promise<void> {
  await request.delete(`/playbooks/${playbookId}/host-groups/${hostGroupId}`);
}

export async function addTag(
  playbookId: number,
  tagId: number
): Promise<void> {
  await request.post(`/playbooks/${playbookId}/tags/${tagId}`);
}

export async function removeTag(
  playbookId: number,
  tagId: number
): Promise<void> {
  await request.delete(`/playbooks/${playbookId}/tags/${tagId}`);
}

export async function generateYaml(playbookId: number): Promise<string> {
  const res = await request.get<string>(`/playbooks/${playbookId}/yaml`);
  return res.data;
}
