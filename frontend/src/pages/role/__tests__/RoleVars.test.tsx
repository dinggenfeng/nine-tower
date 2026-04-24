import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RoleVars from "../RoleVars";

vi.mock("../../../api/roleVariable", () => ({
  getRoleVariables: vi.fn(),
  createRoleVariable: vi.fn(),
  updateRoleVariable: vi.fn(),
  deleteRoleVariable: vi.fn(),
  getRoleDefaults: vi.fn(),
  createRoleDefault: vi.fn(),
  updateRoleDefault: vi.fn(),
  deleteRoleDefault: vi.fn(),
}));

import { getRoleVariables } from "../../../api/roleVariable";
const mockGet = vi.mocked(getRoleVariables);

const baseVar = {
  id: 0,
  roleId: 5,
  key: "",
  value: "",
  createdBy: 1,
  createdAt: "",
  updatedAt: "",
};

describe("RoleVars", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches variables for the given roleId on mount", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleVars roleId={5} />);
    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(5));
  });

  it("renders variable rows in the table", async () => {
    mockGet.mockResolvedValue([
      { ...baseVar, id: 1, key: "http_port", value: "80" },
      { ...baseVar, id: 2, key: "app_name", value: "myapp" },
    ]);
    render(<RoleVars roleId={5} />);
    await waitFor(() => {
      expect(screen.getByText("http_port")).toBeInTheDocument();
      expect(screen.getByText("80")).toBeInTheDocument();
      expect(screen.getByText("app_name")).toBeInTheDocument();
    });
  });

  it("opens the create modal when 添加变量 is clicked", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleVars roleId={5} />);
    await userEvent.click(screen.getByRole("button", { name: /添加变量/ }));
    await waitFor(() => {
      // Modal title + button both say 添加变量
      expect(screen.getAllByText("添加变量").length).toBeGreaterThan(1);
    });
  });
});
