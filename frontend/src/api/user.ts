import request from "./request";
import type { User, PageResponse } from "../types/entity/User";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface UpdateUserPayload {
  email?: string;
  password?: string;
  oldPassword?: string;
}

export const userApi = {
  searchUsers: (keyword?: string, page = 0, size = 20): Promise<ApiResult<PageResponse<User>>> =>
    request.get("/users", { params: { keyword, page, size } }),

  getUser: (id: number): Promise<ApiResult<User>> => request.get(`/users/${id}`),

  updateUser: (id: number, payload: UpdateUserPayload): Promise<ApiResult<User>> =>
    request.put(`/users/${id}`, payload),

  deleteUser: (id: number): Promise<ApiResult<void>> => request.delete(`/users/${id}`),
};
