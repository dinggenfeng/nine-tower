import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import RoleTasks from "../RoleTasks";

vi.mock("../../../api/task", () => ({
  createTask: vi.fn(),
  getTasks: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
  updateTaskTags: vi.fn(),
  getTaskTags: vi.fn(),
  reorderTasks: vi.fn(),
}));

vi.mock("../../../api/handler", () => ({
  getHandlers: vi.fn(),
}));

vi.mock("../../../utils/taskToYaml", () => ({
  taskToYaml: vi.fn(() => "- name: test\n  apt:\n    name: nginx"),
  blockToYaml: vi.fn(() => "block:\n  - name: test\n    apt:\n      name: nginx"),
}));

vi.mock("../../../components/role/ModuleSelect", () => ({
  default: () => <div data-testid="module-select">ModuleSelect</div>,
}));

vi.mock("../../../components/role/ModuleParamsForm", () => ({
  ModuleParamsGrid: () => null,
  ExtraParamsInput: () => null,
}));

vi.mock("../../../components/role/BlockTasksEditor", () => ({
  default: () => <div data-testid="block-editor">BlockTasksEditor</div>,
}));

vi.mock("../../../components/role/TagSelect", () => ({
  default: () => <div data-testid="tag-select">TagSelect</div>,
}));

import { getTasks } from "../../../api/task";
import { getHandlers } from "../../../api/handler";

const mockGetTasks = vi.mocked(getTasks);
const mockGetHandlers = vi.mocked(getHandlers);

function renderRoleTasks() {
  return render(
    <MemoryRouter>
      <RoleTasks roleId={10} />
    </MemoryRouter>
  );
}

import type { Task } from "../../../types/entity/Task";

const sampleTask: Task = {
  id: 1,
  roleId: 10,
  name: "Install nginx",
  module: "apt",
  args: '{"name":"nginx"}',
  whenCondition: "",
  loop: "",
  until: "",
  register: "",
  notify: [],
  taskOrder: 1,
  become: false,
  becomeUser: "",
  ignoreErrors: false,
  parentTaskId: null,
  blockSection: null,
  children: [],
  createdBy: 1,
  createdAt: "",
};

describe("RoleTasks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders task list with task data", async () => {
    mockGetTasks.mockResolvedValue([sampleTask]);
    mockGetHandlers.mockResolvedValue([]);
    renderRoleTasks();

    await waitFor(() => {
      expect(screen.getByText("Install nginx")).toBeInTheDocument();
    });
  });

  it('shows "添加 Task" button when no tasks', async () => {
    mockGetTasks.mockResolvedValue([]);
    mockGetHandlers.mockResolvedValue([]);
    renderRoleTasks();

    await waitFor(() => {
      expect(screen.getByText("添加 Task")).toBeInTheDocument();
    });
  });

  it("renders block task in table", async () => {
    mockGetTasks.mockResolvedValue([
      {
        ...sampleTask,
        id: 2,
        name: "Handle errors",
        module: "block",
        parentTaskId: null,
        blockSection: null,
      },
    ]);
    mockGetHandlers.mockResolvedValue([]);
    renderRoleTasks();

    await waitFor(() => {
      expect(screen.getByText("Handle errors")).toBeInTheDocument();
    });
  });
});
