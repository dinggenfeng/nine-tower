import request from './request';
import type {
  Project,
  ProjectMember,
  CreateProjectRequest,
  UpdateProjectRequest,
  AddMemberRequest,
  UpdateMemberRoleRequest,
} from '../types/entity/Project';

export async function createProject(data: CreateProjectRequest): Promise<Project> {
  const res = await request.post<Project>('/api/projects', data);
  return res.data;
}

export async function getMyProjects(): Promise<Project[]> {
  const res = await request.get<Project[]>('/api/projects');
  return res.data;
}

export async function getProject(id: number): Promise<Project> {
  const res = await request.get<Project>(`/api/projects/${id}`);
  return res.data;
}

export async function updateProject(id: number, data: UpdateProjectRequest): Promise<Project> {
  const res = await request.put<Project>(`/api/projects/${id}`, data);
  return res.data;
}

export async function deleteProject(id: number): Promise<void> {
  await request.delete<void>(`/api/projects/${id}`);
}

export async function getMembers(projectId: number): Promise<ProjectMember[]> {
  const res = await request.get<ProjectMember[]>(`/api/projects/${projectId}/members`);
  return res.data;
}

export async function addMember(projectId: number, data: AddMemberRequest): Promise<ProjectMember> {
  const res = await request.post<ProjectMember>(`/api/projects/${projectId}/members`, data);
  return res.data;
}

export async function removeMember(projectId: number, userId: number): Promise<void> {
  await request.delete<void>(`/api/projects/${projectId}/members/${userId}`);
}

export async function updateMemberRole(
  projectId: number,
  userId: number,
  data: UpdateMemberRoleRequest
): Promise<ProjectMember> {
  const res = await request.put<ProjectMember>(
    `/api/projects/${projectId}/members/${userId}`,
    data
  );
  return res.data;
}
