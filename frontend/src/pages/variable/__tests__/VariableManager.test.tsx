import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import VariableManager from "../VariableManager";

vi.mock("../../../api/variable", () => ({
  listVariables: vi.fn(),
  createVariable: vi.fn(),
  updateVariable: vi.fn(),
  deleteVariable: vi.fn(),
  detectVariables: vi.fn(),
  batchSaveVariables: vi.fn(),
}));

vi.mock("../../../api/environment", () => ({
  listEnvironments: vi.fn(),
}));

vi.mock("../../../api/host", () => ({
  getHostGroups: vi.fn(),
}));

vi.mock("../../../api/role", () => ({
  getRoles: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useParams: () => ({ id: "12" }) };
});

import {
  listVariables,
  detectVariables,
  batchSaveVariables,
} from "../../../api/variable";
import { listEnvironments } from "../../../api/environment";
import { getHostGroups } from "../../../api/host";
import { getRoles } from "../../../api/role";

const mockListVariables = vi.mocked(listVariables);
const mockDetectVariables = vi.mocked(detectVariables);
const mockBatchSaveVariables = vi.mocked(batchSaveVariables);
const mockListEnvironments = vi.mocked(listEnvironments);
const mockGetHostGroups = vi.mocked(getHostGroups);
const mockGetRoles = vi.mocked(getRoles);

function renderPage() {
  return render(
    <MemoryRouter>
      <VariableManager />
    </MemoryRouter>
  );
}

const baseVariable = {
  id: 1,
  scope: "PROJECT" as const,
  scopeId: 12,
  key: "APP_PORT",
  value: "8080",
  createdAt: "",
  updatedAt: "",
};

const baseRole = {
  id: 7,
  projectId: 12,
  name: "web",
  description: "web role",
  createdBy: 1,
  createdAt: "",
};

describe("VariableManager", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListVariables.mockResolvedValue([]);
    mockListEnvironments.mockResolvedValue([]);
    mockGetHostGroups.mockResolvedValue([]);
    mockGetRoles.mockResolvedValue([]);
    mockDetectVariables.mockResolvedValue([]);
    mockBatchSaveVariables.mockResolvedValue([]);
  });

  it("shows the current scope as read-only when editing an existing variable", async () => {
    mockListVariables.mockResolvedValue([baseVariable]);

    renderPage();

    await screen.findByText("APP_PORT");
    await userEvent.click(screen.getByRole("button", { name: /编辑/ }));

    await waitFor(() => {
      expect(screen.getByDisplayValue("项目级")).toBeDisabled();
      expect(screen.getByDisplayValue("APP_PORT")).toBeInTheDocument();
    });
  });

  it("allows 200-character keys in the create modal", async () => {
    renderPage();

    await userEvent.click(screen.getByRole("button", { name: /新建变量/ }));

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/APP_PORT/)).toHaveAttribute("maxlength", "200");
    });
  });

  it("removes successful detected variables and refreshes tree view after batch save", async () => {
    mockGetRoles.mockResolvedValue([baseRole]);
    mockDetectVariables.mockResolvedValue([
      {
        key: "app_port",
        occurrences: [
          {
            roleId: 7,
            roleName: "web",
            type: "TASK",
            entityId: 11,
            entityName: "start app",
            field: "name",
          },
        ],
        suggestedScope: "ROLE_VARS",
      },
      {
        key: "db_host",
        occurrences: [
          {
            roleId: 7,
            roleName: "web",
            type: "TASK",
            entityId: 12,
            entityName: "configure db",
            field: "args",
          },
        ],
        suggestedScope: "ROLE_VARS",
      },
    ]);
    mockBatchSaveVariables.mockResolvedValue([
      { index: 0, success: true, key: "app_port" },
      { index: 1, success: false, error: "duplicate" },
    ]);

    renderPage();

    await userEvent.click(screen.getByText("树形视图"));
    await waitFor(() => {
      expect(mockListVariables).toHaveBeenCalledWith(12, "HOSTGROUP");
      expect(mockListVariables).toHaveBeenCalledWith(12, "ENVIRONMENT");
    });

    mockListVariables.mockClear();

    await userEvent.click(screen.getByRole("button", { name: /变量探测/ }));
    await screen.findByText("app_port");
    await screen.findByText("db_host");

    await userEvent.click(screen.getByRole("button", { name: /批量保存/ }));

    await waitFor(() => {
      expect(mockBatchSaveVariables).toHaveBeenCalled();
      expect(screen.queryByText("app_port")).not.toBeInTheDocument();
      expect(screen.getByText("db_host")).toBeInTheDocument();
    });

    expect(mockListVariables.mock.calls.some(([, scope]) => scope === "HOSTGROUP")).toBe(true);
    expect(mockListVariables.mock.calls.some(([, scope]) => scope === "ENVIRONMENT")).toBe(true);
  });
});
