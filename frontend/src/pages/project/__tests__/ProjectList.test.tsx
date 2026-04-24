import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import ProjectList from "../ProjectList";

vi.mock("../../../api/project", () => ({
  getMyProjects: vi.fn(),
  createProject: vi.fn(),
  deleteProject: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useNavigate: () => mockNavigate };
});

import { getMyProjects } from "../../../api/project";
const mockGetMyProjects = vi.mocked(getMyProjects);

function renderPage() {
  return render(
    <MemoryRouter>
      <ProjectList />
    </MemoryRouter>
  );
}

const baseProject = {
  id: 0,
  name: "",
  description: "",
  createdBy: 1,
  createdAt: "",
  updatedAt: "",
  myRole: "PROJECT_MEMBER" as const,
};

describe("ProjectList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches projects on mount and renders card names", async () => {
    mockGetMyProjects.mockResolvedValue([
      { ...baseProject, id: 1, name: "alpha", myRole: "PROJECT_ADMIN" },
      { ...baseProject, id: 2, name: "beta" },
    ]);
    renderPage();
    await waitFor(() => {
      expect(mockGetMyProjects).toHaveBeenCalledTimes(1);
      expect(screen.getByText("alpha")).toBeInTheDocument();
      expect(screen.getByText("beta")).toBeInTheDocument();
    });
  });

  it("shows project count", async () => {
    mockGetMyProjects.mockResolvedValue([
      { ...baseProject, id: 1, name: "only", myRole: "PROJECT_ADMIN" },
    ]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/共\s*1\s*个项目/)).toBeInTheDocument();
    });
  });

  it("shows empty state when the list is empty", async () => {
    mockGetMyProjects.mockResolvedValue([]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("暂无项目")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /创建第一个项目/ })).toBeInTheDocument();
    });
  });

  it("opens the create modal when 新建项目 is clicked", async () => {
    mockGetMyProjects.mockResolvedValue([
      { ...baseProject, id: 1, name: "a", myRole: "PROJECT_ADMIN" },
    ]);
    renderPage();
    await userEvent.click(screen.getByRole("button", { name: /新建项目/ }));
    await waitFor(() => {
      expect(screen.getByText("创建项目")).toBeInTheDocument();
    });
  });
});
