import { describe, it, expect, beforeEach, vi } from "vitest";
import type { User } from "../../types/entity/User";
import { useAuthStore } from "../authStore";
import * as authApi from "../../api/auth";

vi.mock("../../api/auth", () => ({
  authApi: {
    me: vi.fn(),
  },
}));

function makeUser(overrides: Partial<User> = {}): User {
  return { id: 1, username: "test", email: "t@t", ...overrides } as User;
}

describe("useAuthStore", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: false,
    });
  });

  it("login sets token, user, and isAuthenticated", () => {
    useAuthStore.getState().login("tok123", makeUser({ id: 1, username: "alice" }));
    const state = useAuthStore.getState();
    expect(state.token).toBe("tok123");
    expect(state.user?.username).toBe("alice");
    expect(state.isAuthenticated).toBe(true);
    expect(localStorage.getItem("token")).toBe("tok123");
  });

  it("logout clears all state and localStorage", () => {
    useAuthStore.getState().login("tok123", makeUser({ id: 1, username: "alice" }));
    useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(localStorage.getItem("token")).toBeNull();
  });

  it("fetchUser fetches and sets user when token exists but user is null", async () => {
    localStorage.setItem("token", "tok123");
    useAuthStore.setState({ token: "tok123", isAuthenticated: true });
    vi.mocked(authApi.authApi.me).mockResolvedValue(makeUser({ id: 1, username: "bob" }));
    await useAuthStore.getState().fetchUser();
    expect(useAuthStore.getState().user?.username).toBe("bob");
    expect(useAuthStore.getState().loading).toBe(false);
  });

  it("fetchUser skips API call if user already exists", async () => {
    localStorage.setItem("token", "tok");
    useAuthStore.setState({ token: "tok", user: makeUser({ id: 1 }) });
    await useAuthStore.getState().fetchUser();
    expect(authApi.authApi.me).not.toHaveBeenCalled();
  });

  it("fetchUser clears auth on API failure", async () => {
    localStorage.setItem("token", "bad");
    useAuthStore.setState({ token: "bad", isAuthenticated: true });
    vi.mocked(authApi.authApi.me).mockRejectedValue(new Error("fail"));
    await useAuthStore.getState().fetchUser();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().token).toBeNull();
    expect(localStorage.getItem("token")).toBeNull();
  });

  it("setUser updates user in state", () => {
    useAuthStore.getState().setUser(makeUser({ id: 2, username: "carol" }));
    expect(useAuthStore.getState().user?.username).toBe("carol");
  });
});
