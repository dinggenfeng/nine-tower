import request from "./request";
import type { RoleFile, CreateFileRequest, UpdateFileRequest } from "../types/entity/RoleFile";

export async function createFile(roleId: number, data: CreateFileRequest): Promise<RoleFile> {
  const res = await request.post<RoleFile>(`/roles/${roleId}/files`, data, {
    params: data.textContent ? { textContent: data.textContent } : undefined,
  });
  return res.data;
}

export async function getFiles(roleId: number): Promise<RoleFile[]> {
  const res = await request.get<RoleFile[]>(`/roles/${roleId}/files`);
  return res.data;
}

export async function getFile(id: number): Promise<RoleFile> {
  const res = await request.get<RoleFile>(`/files/${id}`);
  return res.data;
}

export async function updateFile(id: number, data: UpdateFileRequest): Promise<RoleFile> {
  const res = await request.put<RoleFile>(`/files/${id}`, data);
  return res.data;
}

export async function deleteFile(id: number): Promise<void> {
  await request.delete(`/files/${id}`);
}

export function getFileDownloadUrl(id: number): string {
  return `/api/files/${id}/download`;
}
