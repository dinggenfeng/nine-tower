import request from "./request";
import type {
  Project,
  ProjectMember,
  CreateProjectRequest,
  UpdateProjectRequest,
  AddMemberRequest,
  UpdateMemberRoleRequest,
} from "../types/entity/Project";

export async function createProject(data: CreateProjectRequest): Promise<Project> {
  const res = await request.post<Project>("/projects", data);
  return res.data;
}

export async function getMyProjects(): Promise<Project[]> {
  const res = await request.get<Project[]>("/projects");
  return res.data;
}

export async function getProject(id: number): Promise<Project> {
  const res = await request.get<Project>(`/projects/${id}`);
  return res.data;
}

export async function updateProject(id: number, data: UpdateProjectRequest): Promise<Project> {
  const res = await request.put<Project>(`/projects/${id}`, data);
  return res.data;
}

export async function deleteProject(id: number): Promise<void> {
  await request.delete<void>(`/projects/${id}`);
}

export async function getMembers(projectId: number): Promise<ProjectMember[]> {
  const res = await request.get<ProjectMember[]>(`/projects/${projectId}/members`);
  return res.data;
}

export async function addMember(projectId: number, data: AddMemberRequest): Promise<ProjectMember> {
  const res = await request.post<ProjectMember>(`/projects/${projectId}/members`, data);
  return res.data;
}

export async function removeMember(projectId: number, userId: number): Promise<void> {
  await request.delete<void>(`/projects/${projectId}/members/${userId}`);
}

export async function updateMemberRole(
  projectId: number,
  userId: number,
  data: UpdateMemberRoleRequest
): Promise<ProjectMember> {
  const res = await request.put<ProjectMember>(`/projects/${projectId}/members/${userId}`, data);
  return res.data;
}
