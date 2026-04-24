import { describe, it, expect, vi, beforeEach, type Mock } from "vitest";

vi.mock("axios", () => {
  const interceptors = {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  };
  const instance = { interceptors };
  return {
    default: {
      create: vi.fn(() => instance),
    },
  };
});

type RequestConfig = { headers: Record<string, string> };
type MockedAxios = { create: Mock };
type MockedInstance = {
  interceptors: {
    request: { use: Mock };
    response: { use: Mock };
  };
};

async function getMockedAxios(): Promise<MockedAxios> {
  const axios = (await import("axios")).default;
  return axios as unknown as MockedAxios;
}

describe("request interceptor", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("creates axios instance with /api base URL", async () => {
    const axios = await getMockedAxios();
    await import("./request");
    expect(axios.create).toHaveBeenCalledWith({
      baseURL: "/api",
      timeout: 10000,
    });
  });

  it("request interceptor adds Bearer token when present", async () => {
    localStorage.setItem("token", "test-jwt-token");
    const axios = await getMockedAxios();
    const instance = axios.create() as unknown as MockedInstance;
    await import("./request");

    const callback = instance.interceptors.request.use.mock.calls[0][0] as (
      c: RequestConfig
    ) => RequestConfig;
    const result = callback({ headers: {} });
    expect(result.headers.Authorization).toBe("Bearer test-jwt-token");
  });

  it("request interceptor skips header when no token", async () => {
    const axios = await getMockedAxios();
    const instance = axios.create() as unknown as MockedInstance;
    await import("./request");

    const callback = instance.interceptors.request.use.mock.calls[0][0] as (
      c: RequestConfig
    ) => RequestConfig;
    const result = callback({ headers: {} });
    expect(result.headers.Authorization).toBeUndefined();
  });

  it("response interceptor returns data on success", async () => {
    const axios = await getMockedAxios();
    const instance = axios.create() as unknown as MockedInstance;
    await import("./request");

    const successCb = instance.interceptors.response.use.mock.calls[0][0] as (r: {
      data: unknown;
    }) => unknown;
    expect(successCb({ data: { id: 1 } })).toEqual({ id: 1 });
  });
});
