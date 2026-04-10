import request from './request';
import type {
  Project,
  ProjectMember,
  CreateProjectRequest,
  UpdateProjectRequest,
  AddMemberRequest,
  UpdateMemberRoleRequest,
} from '../types/entity/Project';

export function createProject(data: CreateProjectRequest) {
  return request.post<Project>('/api/projects', data);
}

export function getMyProjects() {
  return request.get<Project[]>('/api/projects');
}

export function getProject(id: number) {
  return request.get<Project>(`/api/projects/${id}`);
}

export function updateProject(id: number, data: UpdateProjectRequest) {
  return request.put<Project>(`/api/projects/${id}`, data);
}

export function deleteProject(id: number) {
  return request.delete<void>(`/api/projects/${id}`);
}

export function getMembers(projectId: number) {
  return request.get<ProjectMember[]>(`/api/projects/${projectId}/members`);
}

export function addMember(projectId: number, data: AddMemberRequest) {
  return request.post<ProjectMember>(`/api/projects/${projectId}/members`, data);
}

export function removeMember(projectId: number, userId: number) {
  return request.delete<void>(`/api/projects/${projectId}/members/${userId}`);
}

export function updateMemberRole(
  projectId: number,
  userId: number,
  data: UpdateMemberRoleRequest
) {
  return request.put<ProjectMember>(
    `/api/projects/${projectId}/members/${userId}`,
    data
  );
}
