import request from "./request";
import type { TokenResponse } from "../types/entity/User";

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface RegisterPayload {
  username: string;
  password: string;
  email: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export const authApi = {
  register: (payload: RegisterPayload): Promise<ApiResult<TokenResponse>> =>
    request.post("/auth/register", payload),

  login: (payload: LoginPayload): Promise<ApiResult<TokenResponse>> =>
    request.post("/auth/login", payload),

  me: (): Promise<ApiResult<TokenResponse["user"]>> => request.get("/auth/me"),
};
