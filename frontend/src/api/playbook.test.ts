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
  listPlaybooks,
  getPlaybook,
  createPlaybook,
  updatePlaybook,
  deletePlaybook,
  addRole,
  removeRole,
  reorderRoles,
  addHostGroup,
  removeHostGroup,
  addTag,
  removeTag,
  addEnvironment,
  removeEnvironment,
  generateYaml,
} from "./playbook";

beforeEach(() => {
  vi.clearAllMocks();
});

describe("playbook API", () => {
  describe("listPlaybooks", () => {
    it("calls GET /projects/:id/playbooks", async () => {
      const data = [{ id: 1, name: "deploy" }];
      mockGet.mockResolvedValue({ data });
      const result = await listPlaybooks(42);
      expect(mockGet).toHaveBeenCalledWith("/projects/42/playbooks");
      expect(result).toEqual(data);
    });
  });

  describe("getPlaybook", () => {
    it("calls GET /playbooks/:id", async () => {
      const data = { id: 1, name: "deploy" };
      mockGet.mockResolvedValue({ data });
      const result = await getPlaybook(1);
      expect(mockGet).toHaveBeenCalledWith("/playbooks/1");
      expect(result).toEqual(data);
    });
  });

  describe("createPlaybook", () => {
    it("calls POST /projects/:id/playbooks with body", async () => {
      const data = { id: 1, name: "deploy" };
      const body = { name: "deploy", description: "desc" };
      mockPost.mockResolvedValue({ data });
      const result = await createPlaybook(42, body);
      expect(mockPost).toHaveBeenCalledWith("/projects/42/playbooks", body);
      expect(result).toEqual(data);
    });
  });

  describe("updatePlaybook", () => {
    it("calls PUT /playbooks/:id with body", async () => {
      const data = { id: 1, name: "deploy" };
      const body = { name: "deploy", extraVars: "key: value" };
      mockPut.mockResolvedValue({ data });
      const result = await updatePlaybook(1, body);
      expect(mockPut).toHaveBeenCalledWith("/playbooks/1", body);
      expect(result).toEqual(data);
    });
  });

  describe("deletePlaybook", () => {
    it("calls DELETE /playbooks/:id", async () => {
      mockDelete.mockResolvedValue({});
      await deletePlaybook(1);
      expect(mockDelete).toHaveBeenCalledWith("/playbooks/1");
    });
  });

  describe("role management", () => {
    it("addRole calls POST /playbooks/:id/roles", async () => {
      mockPost.mockResolvedValue({});
      await addRole(1, 10);
      expect(mockPost).toHaveBeenCalledWith("/playbooks/1/roles", { roleId: 10 });
    });

    it("removeRole calls DELETE /playbooks/:id/roles/:roleId", async () => {
      mockDelete.mockResolvedValue({});
      await removeRole(1, 10);
      expect(mockDelete).toHaveBeenCalledWith("/playbooks/1/roles/10");
    });

    it("reorderRoles calls PUT /playbooks/:id/roles/order", async () => {
      mockPut.mockResolvedValue({});
      const order = [3, 1, 2];
      await reorderRoles(1, order);
      expect(mockPut).toHaveBeenCalledWith("/playbooks/1/roles/order", order);
    });
  });

  describe("host group management", () => {
    it("addHostGroup calls POST with correct URL", async () => {
      mockPost.mockResolvedValue({});
      await addHostGroup(1, 5);
      expect(mockPost).toHaveBeenCalledWith("/playbooks/1/host-groups/5");
    });

    it("removeHostGroup calls DELETE with correct URL", async () => {
      mockDelete.mockResolvedValue({});
      await removeHostGroup(1, 5);
      expect(mockDelete).toHaveBeenCalledWith("/playbooks/1/host-groups/5");
    });
  });

  describe("tag management", () => {
    it("addTag calls POST with correct URL", async () => {
      mockPost.mockResolvedValue({});
      await addTag(1, 3);
      expect(mockPost).toHaveBeenCalledWith("/playbooks/1/tags/3");
    });

    it("removeTag calls DELETE with correct URL", async () => {
      mockDelete.mockResolvedValue({});
      await removeTag(1, 3);
      expect(mockDelete).toHaveBeenCalledWith("/playbooks/1/tags/3");
    });
  });

  describe("environment management", () => {
    it("addEnvironment calls POST with correct URL", async () => {
      mockPost.mockResolvedValue({});
      await addEnvironment(1, 7);
      expect(mockPost).toHaveBeenCalledWith("/playbooks/1/environments/7");
    });

    it("removeEnvironment calls DELETE with correct URL", async () => {
      mockDelete.mockResolvedValue({});
      await removeEnvironment(1, 7);
      expect(mockDelete).toHaveBeenCalledWith("/playbooks/1/environments/7");
    });
  });

  describe("generateYaml", () => {
    it("calls GET /playbooks/:id/yaml and returns string", async () => {
      const yaml = "- hosts: all\n  become: true";
      mockGet.mockResolvedValue({ data: yaml });
      const result = await generateYaml(1);
      expect(mockGet).toHaveBeenCalledWith("/playbooks/1/yaml");
      expect(result).toBe(yaml);
    });
  });
});
