import request from './request';
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from '../types/entity/Template';

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

export async function updateTemplate(
  id: number,
  data: UpdateTemplateRequest
): Promise<Template> {
  const res = await request.put<Template>(`/templates/${id}`, data);
  return res.data;
}

export async function deleteTemplate(id: number): Promise<void> {
  await request.delete(`/templates/${id}`);
}
