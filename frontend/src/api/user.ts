import request from "./request";
import type { User, PageResponse } from "../types/entity/User";

export interface UpdateUserPayload {
  email?: string;
  password?: string;
  oldPassword?: string;
}

export const userApi = {
  searchUsers: async (keyword?: string, page = 0, size = 20): Promise<PageResponse<User>> => {
    const res = await request.get<PageResponse<User>>("/users", {
      params: { keyword, page, size },
    });
    return res.data;
  },

  getUser: async (id: number): Promise<User> => {
    const res = await request.get<User>(`/users/${id}`);
    return res.data;
  },

  updateUser: async (id: number, payload: UpdateUserPayload): Promise<User> => {
    const res = await request.put<User>(`/users/${id}`, payload);
    return res.data;
  },

  deleteUser: async (id: number): Promise<void> => {
    await request.delete(`/users/${id}`);
  },
};
