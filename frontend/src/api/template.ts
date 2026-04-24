import request from "./request";
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from "../types/entity/Template";

export async function createTemplate(
  roleId: number,
  data: CreateTemplateRequest
): Promise<Template> {
  const res = await request.post<Template>(`/roles/${roleId}/templates`, data);
  return res.data;
}

export async function getTemplates(roleId: number): Promise<Template[]> {
  const res = await request.get<Template[]>(`/roles/${roleId}/templates`);
  return res.data;
}

export async function getTemplate(id: number): Promise<Template> {
  const res = await request.get<Template>(`/templates/${id}`);
  return res.data;
}

export async function updateTemplate(id: number, data: UpdateTemplateRequest): Promise<Template> {
  const res = await request.put<Template>(`/templates/${id}`, data);
  return res.data;
}

export async function deleteTemplate(id: number): Promise<void> {
  await request.delete(`/templates/${id}`);
}

export async function downloadTemplate(id: number): Promise<void> {
  const token = localStorage.getItem("token");
  const resp = await fetch(`/api/templates/${id}/download`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!resp.ok) throw new Error("下载失败");
  const blob = await resp.blob();
  const filename =
    resp.headers.get("Content-Disposition")?.match(/filename="(.+?)"/)?.[1] ?? "template";
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
