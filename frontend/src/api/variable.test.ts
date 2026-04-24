import { describe, it, expect, vi, beforeEach } from "vitest";

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock("./request", () => ({
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
  createVariable,
  listVariables,
  getVariable,
  updateVariable,
  deleteVariable,
} from "./variable";

beforeEach(() => {
  vi.clearAllMocks();
});

describe("variable API", () => {
  describe("listVariables", () => {
    it("calls GET with scope param only for PROJECT", async () => {
      const data = [{ id: 1, key: "app_port", value: "8080" }];
      mockGet.mockResolvedValue({ data });
      const result = await listVariables(1, "PROJECT");
      expect(mockGet).toHaveBeenCalledWith("/projects/1/variables", {
        params: { scope: "PROJECT" },
      });
      expect(result).toEqual(data);
    });

    it("calls GET with scope and scopeId for HOSTGROUP", async () => {
      mockGet.mockResolvedValue({ data: [] });
      await listVariables(1, "HOSTGROUP", 5);
      expect(mockGet).toHaveBeenCalledWith("/projects/1/variables", {
        params: { scope: "HOSTGROUP", scopeId: 5 },
      });
    });

    it("calls GET with scope and scopeId for ENVIRONMENT", async () => {
      mockGet.mockResolvedValue({ data: [] });
      await listVariables(1, "ENVIRONMENT", 7);
      expect(mockGet).toHaveBeenCalledWith("/projects/1/variables", {
        params: { scope: "ENVIRONMENT", scopeId: 7 },
      });
    });
  });

  describe("createVariable", () => {
    it("calls POST with body", async () => {
      const body = {
        scope: "PROJECT" as const,
        scopeId: 1,
        key: "port",
        value: "8080",
      };
      const data = { id: 1, ...body };
      mockPost.mockResolvedValue({ data });
      const result = await createVariable(1, body);
      expect(mockPost).toHaveBeenCalledWith("/projects/1/variables", body);
      expect(result).toEqual(data);
    });
  });

  describe("updateVariable", () => {
    it("calls PUT with body", async () => {
      const body = { key: "port", value: "9090" };
      const data = { id: 1, key: "port", value: "9090" };
      mockPut.mockResolvedValue({ data });
      const result = await updateVariable(1, body);
      expect(mockPut).toHaveBeenCalledWith("/variables/1", body);
      expect(result).toEqual(data);
    });
  });

  describe("deleteVariable", () => {
    it("calls DELETE with correct URL", async () => {
      mockDelete.mockResolvedValue({});
      await deleteVariable(1);
      expect(mockDelete).toHaveBeenCalledWith("/variables/1");
    });
  });

  describe("getVariable", () => {
    it("calls GET with correct URL", async () => {
      const data = { id: 1, key: "port", value: "8080" };
      mockGet.mockResolvedValue({ data });
      const result = await getVariable(1);
      expect(mockGet).toHaveBeenCalledWith("/variables/1");
      expect(result).toEqual(data);
    });
  });
});
