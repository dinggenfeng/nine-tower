import { create } from "zustand";
import type { User } from "../types/entity/User";
import { authApi } from "../api/auth";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  fetchUser: () => Promise<void>;
  setUser: (user: User) => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: localStorage.getItem("token"),
  isAuthenticated: !!localStorage.getItem("token"),
  loading: false,

  login: (token: string, user: User) => {
    localStorage.setItem("token", token);
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem("token");
    set({ token: null, user: null, isAuthenticated: false });
  },

  fetchUser: async () => {
    const { token, user } = get();
    if (!token || user) return;
    set({ loading: true });
    try {
      const user = await authApi.me();
      if (!get().user) {
        set({ user, loading: false });
      }
    } catch {
      if (!get().user) {
        localStorage.removeItem("token");
        set({ token: null, user: null, isAuthenticated: false, loading: false });
      }
    }
  },

  setUser: (user: User) => {
    set({ user });
  },
}));
