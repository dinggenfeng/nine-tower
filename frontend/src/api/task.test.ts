import { describe, it, expect, vi, beforeEach } from 'vitest';

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('./request', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

import {
  createTask,
  getTasks,
  getTask,
  updateTask,
  deleteTask,
  updateTaskTags,
  getTaskTags,
  reorderTasks,
} from './task';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('task API', () => {
  describe('createTask', () => {
    it('calls POST /roles/:roleId/tasks', async () => {
      const body = { name: 'Install nginx', module: 'apt', taskOrder: 1 };
      const data = { id: 1, ...body };
      mockPost.mockResolvedValue({ data });
      const result = await createTask(5, body);
      expect(mockPost).toHaveBeenCalledWith('/roles/5/tasks', body);
      expect(result).toEqual(data);
    });
  });

  describe('getTasks', () => {
    it('calls GET /roles/:roleId/tasks', async () => {
      const data = [{ id: 1, name: 'Install nginx' }];
      mockGet.mockResolvedValue({ data });
      const result = await getTasks(5);
      expect(mockGet).toHaveBeenCalledWith('/roles/5/tasks');
      expect(result).toEqual(data);
    });
  });

  describe('getTask', () => {
    it('calls GET /tasks/:id', async () => {
      const data = { id: 1, name: 'Install nginx' };
      mockGet.mockResolvedValue({ data });
      const result = await getTask(1);
      expect(mockGet).toHaveBeenCalledWith('/tasks/1');
      expect(result).toEqual(data);
    });
  });

  describe('updateTask', () => {
    it('calls PUT /tasks/:id with body', async () => {
      const body = { name: 'Updated task' };
      const data = { id: 1, name: 'Updated task' };
      mockPut.mockResolvedValue({ data });
      const result = await updateTask(1, body);
      expect(mockPut).toHaveBeenCalledWith('/tasks/1', body);
      expect(result).toEqual(data);
    });
  });

  describe('deleteTask', () => {
    it('calls DELETE /tasks/:id', async () => {
      mockDelete.mockResolvedValue({});
      await deleteTask(1);
      expect(mockDelete).toHaveBeenCalledWith('/tasks/1');
    });
  });

  describe('updateTaskTags', () => {
    it('calls PUT /tasks/:id/tags with tagIds', async () => {
      mockPut.mockResolvedValue({});
      await updateTaskTags(1, [10, 20]);
      expect(mockPut).toHaveBeenCalledWith('/tasks/1/tags', [10, 20]);
    });
  });

  describe('getTaskTags', () => {
    it('calls GET /tasks/:id/tags and returns number array', async () => {
      mockGet.mockResolvedValue({ data: [10, 20] });
      const result = await getTaskTags(1);
      expect(mockGet).toHaveBeenCalledWith('/tasks/1/tags');
      expect(result).toEqual([10, 20]);
    });
  });

  describe('reorderTasks', () => {
    it('calls PUT /roles/:roleId/tasks/order', async () => {
      mockPut.mockResolvedValue({});
      await reorderTasks(5, [3, 1, 2]);
      expect(mockPut).toHaveBeenCalledWith('/roles/5/tasks/order', [3, 1, 2]);
    });
  });
});
