import request from './request';
import type {
  Tag,
  CreateTagRequest,
  UpdateTagRequest,
} from '../types/entity/Tag';

export async function createTag(
  projectId: number,
  data: CreateTagRequest
): Promise<Tag> {
  const res = await request.post<Tag>(`/projects/${projectId}/tags`, data);
  return res.data;
}

export async function listTags(projectId: number): Promise<Tag[]> {
  const res = await request.get<Tag[]>(`/projects/${projectId}/tags`);
  return res.data;
}

export async function updateTag(
  tagId: number,
  data: UpdateTagRequest
): Promise<Tag> {
  const res = await request.put<Tag>(`/tags/${tagId}`, data);
  return res.data;
}

export async function deleteTag(tagId: number): Promise<void> {
  await request.delete(`/tags/${tagId}`);
}
