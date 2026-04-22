import request from './request';
import type { Handler, CreateHandlerRequest, UpdateHandlerRequest, Task } from '../types/entity/Task';

export async function createHandler(
  roleId: number,
  data: CreateHandlerRequest
): Promise<Handler> {
  const res = await request.post<Handler>(`/roles/${roleId}/handlers`, data);
  return res.data;
}

export async function getHandlers(roleId: number): Promise<Handler[]> {
  const res = await request.get<Handler[]>(`/roles/${roleId}/handlers`);
  return res.data;
}

export async function getHandler(id: number): Promise<Handler> {
  const res = await request.get<Handler>(`/handlers/${id}`);
  return res.data;
}

export async function updateHandler(
  id: number,
  data: UpdateHandlerRequest
): Promise<Handler> {
  const res = await request.put<Handler>(`/handlers/${id}`, data);
  return res.data;
}

export async function deleteHandler(id: number): Promise<void> {
  await request.delete(`/handlers/${id}`);
}

export async function getNotifyingTasks(handlerId: number): Promise<Task[]> {
  const res = await request.get<Task[]>(`/handlers/${handlerId}/notified-by`);
  return res.data;
}
