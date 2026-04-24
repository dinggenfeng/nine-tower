import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RoleHandlers from "../RoleHandlers";

vi.mock("../../../api/handler", () => ({
  createHandler: vi.fn(),
  getHandlers: vi.fn(),
  updateHandler: vi.fn(),
  deleteHandler: vi.fn(),
  getNotifyingTasks: vi.fn(),
}));

vi.mock("../../../components/role/ModuleSelect", () => ({
  default: () => <div data-testid="module-select">ModuleSelect</div>,
}));

vi.mock("../../../components/role/ModuleParamsForm", () => ({
  ModuleParamsGrid: () => null,
  ExtraParamsInput: () => null,
}));

vi.mock("../../../utils/taskToYaml", () => ({
  taskToYaml: vi.fn(() => "- name: stub"),
}));

import { getHandlers } from "../../../api/handler";
const mockGet = vi.mocked(getHandlers);

const baseHandler = {
  id: 0,
  roleId: 7,
  name: "",
  module: "",
  args: "",
  whenCondition: "",
  register: "",
  become: false,
  becomeUser: "",
  ignoreErrors: false,
  createdBy: 1,
  createdAt: "",
  updatedAt: "",
};

describe("RoleHandlers", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches handlers for the given roleId on mount", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleHandlers roleId={7} />);
    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(7));
  });

  it("renders handler rows with name and module", async () => {
    mockGet.mockResolvedValue([
      { ...baseHandler, id: 1, name: "Restart nginx", module: "service" },
    ]);
    render(<RoleHandlers roleId={7} />);
    await waitFor(() => {
      expect(screen.getByText("Restart nginx")).toBeInTheDocument();
      expect(screen.getByText("service")).toBeInTheDocument();
    });
  });

  it("opens the create modal when 添加 Handler is clicked", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleHandlers roleId={7} />);
    await userEvent.click(screen.getByRole("button", { name: /添加 Handler/ }));
    await waitFor(() => {
      expect(screen.getByText("创建 Handler")).toBeInTheDocument();
    });
  });
});
