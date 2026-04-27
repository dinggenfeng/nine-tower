import request from "./request";
import type { RoleFile, CreateFileRequest, UpdateFileRequest } from "../types/entity/RoleFile";

export async function createFile(roleId: number, data: CreateFileRequest): Promise<RoleFile> {
  const res = await request.post<RoleFile>(`/roles/${roleId}/files`, data);
  return res.data;
}

export async function uploadFile(
  roleId: number,
  file: File,
  parentDir?: string
): Promise<RoleFile> {
  const formData = new FormData();
  formData.append("file", file);
  if (parentDir) {
    formData.append("parentDir", parentDir);
  }
  const token = localStorage.getItem("token");
  const resp = await fetch(`/api/roles/${roleId}/files/upload`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({ message: "Upload failed" }));
    throw new Error(err.message || "Upload failed");
  }
  const result = await resp.json();
  return result.data;
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

export async function downloadFile(id: number): Promise<void> {
  const token = localStorage.getItem("token");
  const resp = await fetch(`/api/files/${id}/download`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!resp.ok) throw new Error("下载失败");
  const blob = await resp.blob();
  const filename =
    resp.headers.get("Content-Disposition")?.match(/filename="(.+?)"/)?.[1] ?? "file";
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
