import request from './request';
import type { Task, CreateTaskRequest, UpdateTaskRequest } from '../types/entity/Task';

export async function createTask(
  roleId: number,
  data: CreateTaskRequest
): Promise<Task> {
  const res = await request.post<Task>(`/roles/${roleId}/tasks`, data);
  return res.data;
}

export async function getTasks(roleId: number): Promise<Task[]> {
  const res = await request.get<Task[]>(`/roles/${roleId}/tasks`);
  return res.data;
}

export async function getTask(id: number): Promise<Task> {
  const res = await request.get<Task>(`/tasks/${id}`);
  return res.data;
}

export async function updateTask(
  id: number,
  data: UpdateTaskRequest
): Promise<Task> {
  const res = await request.put<Task>(`/tasks/${id}`, data);
  return res.data;
}

export async function deleteTask(id: number): Promise<void> {
  await request.delete(`/tasks/${id}`);
}

export async function updateTaskTags(
  taskId: number,
  tagIds: number[],
): Promise<void> {
  await request.put(`/tasks/${taskId}/tags`, tagIds);
}

export async function getTaskTags(taskId: number): Promise<number[]> {
  const res = await request.get<number[]>(`/tasks/${taskId}/tags`);
  return res.data;
}
