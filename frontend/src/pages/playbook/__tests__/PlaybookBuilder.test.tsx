import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

vi.mock("../../../api/playbook", () => ({
  getPlaybook: vi.fn(),
  updatePlaybook: vi.fn(),
  addRole: vi.fn(),
  removeRole: vi.fn(),
  reorderRoles: vi.fn(),
  addHostGroup: vi.fn(),
  removeHostGroup: vi.fn(),
  addTag: vi.fn(),
  removeTag: vi.fn(),
  addEnvironment: vi.fn(),
  removeEnvironment: vi.fn(),
  generateYaml: vi.fn(),
}));

vi.mock("../../../api/role", () => ({
  getRoles: vi.fn(),
}));

vi.mock("../../../api/host", () => ({
  getHostGroups: vi.fn(),
}));

vi.mock("../../../api/tag", () => ({
  listTags: vi.fn(),
}));

vi.mock("../../../api/environment", () => ({
  listEnvironments: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useParams: () => ({ id: "1", pbId: "10" }) };
});

import { getPlaybook, generateYaml } from "../../../api/playbook";
import { getRoles } from "../../../api/role";
import { getHostGroups } from "../../../api/host";
import { listTags } from "../../../api/tag";
import { listEnvironments } from "../../../api/environment";
import PlaybookBuilder from "../PlaybookBuilder";

const mockGetPlaybook = vi.mocked(getPlaybook);
const mockGetRoles = vi.mocked(getRoles);
const mockGetHostGroups = vi.mocked(getHostGroups);
const mockListTags = vi.mocked(listTags);
const mockListEnvironments = vi.mocked(listEnvironments);
const mockGenerateYaml = vi.mocked(generateYaml);

function renderBuilder() {
  return render(
    <MemoryRouter>
      <PlaybookBuilder />
    </MemoryRouter>
  );
}

function setupMocks(overrides: Record<string, unknown> = {}) {
  mockGetPlaybook.mockResolvedValue({
    id: 10,
    projectId: 1,
    name: "deploy.yml",
    description: "",
    extraVars: "",
    roleIds: [],
    hostGroupIds: [],
    tagIds: [],
    environmentIds: [],
    createdAt: "",
    updatedAt: "",
    ...overrides,
  });
  mockGetRoles.mockResolvedValue([]);
  mockGetHostGroups.mockResolvedValue([]);
  mockListTags.mockResolvedValue([]);
  mockListEnvironments.mockResolvedValue([]);
  mockGenerateYaml.mockResolvedValue("- hosts: all\n  roles: []\n");
}

describe("PlaybookBuilder", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders main sections after loading", async () => {
    setupMocks();
    renderBuilder();

    await waitFor(() => {
      expect(screen.getByText("Roles（按顺序）")).toBeInTheDocument();
    });
    expect(screen.getByText("主机组")).toBeInTheDocument();
    expect(screen.getByText("标签")).toBeInTheDocument();
    expect(screen.getByText("环境")).toBeInTheDocument();
    expect(screen.getByText("YAML 预览")).toBeInTheDocument();
  });

  it("shows YAML preview content", async () => {
    setupMocks();
    mockGenerateYaml.mockResolvedValue("- hosts: web\n  roles:\n    - nginx\n");
    renderBuilder();

    await waitFor(() => {
      expect(screen.getByText(/hosts: web/)).toBeInTheDocument();
    });
  });

  it("renders Extra Vars textarea with initial value", async () => {
    setupMocks({ extraVars: "app_port: 8080" });
    renderBuilder();

    await waitFor(() => {
      expect(screen.getByText("Extra Vars")).toBeInTheDocument();
    });
    const textarea = screen.getByPlaceholderText(/key: value/);
    expect(textarea).toHaveValue("app_port: 8080");
  });
});
