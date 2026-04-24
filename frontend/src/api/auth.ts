import request from "./request";
import type { TokenResponse } from "../types/entity/User";

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
  register: async (payload: RegisterPayload): Promise<TokenResponse> => {
    const res = await request.post<TokenResponse>("/auth/register", payload);
    return res.data;
  },

  login: async (payload: LoginPayload): Promise<TokenResponse> => {
    const res = await request.post<TokenResponse>("/auth/login", payload);
    return res.data;
  },

  me: async (): Promise<TokenResponse["user"]> => {
    const res = await request.get<TokenResponse["user"]>("/auth/me");
    return res.data;
  },
};
