import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import EnvironmentManager from "../EnvironmentManager";

vi.mock("../../../api/environment", () => ({
  listEnvironments: vi.fn(),
  createEnvironment: vi.fn(),
  updateEnvironment: vi.fn(),
  deleteEnvironment: vi.fn(),
  addConfig: vi.fn(),
  updateConfig: vi.fn(),
  removeConfig: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useParams: () => ({ id: "9" }) };
});

import { listEnvironments } from "../../../api/environment";
const mockList = vi.mocked(listEnvironments);

function renderPage() {
  return render(
    <MemoryRouter>
      <EnvironmentManager />
    </MemoryRouter>
  );
}

const baseEnv = {
  id: 0,
  projectId: 9,
  name: "",
  description: "",
  configs: [] as { id: number; environmentId: number; configKey: string; configValue: string }[],
  createdBy: 1,
  createdAt: "",
  updatedAt: "",
};

describe("EnvironmentManager", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches environments for the current project on mount", async () => {
    mockList.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockList).toHaveBeenCalledWith(9));
  });

  it("shows empty state when no environments exist", async () => {
    mockList.mockResolvedValue([]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("暂无环境")).toBeInTheDocument();
    });
  });

  it("renders each environment as a card with configs", async () => {
    mockList.mockResolvedValue([
      {
        ...baseEnv,
        id: 1,
        name: "production",
        description: "prod env",
        configs: [{ id: 10, environmentId: 1, configKey: "DB_HOST", configValue: "db.internal" }],
      },
    ]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("production")).toBeInTheDocument();
      expect(screen.getByText("prod env")).toBeInTheDocument();
      expect(screen.getByText("DB_HOST")).toBeInTheDocument();
      expect(screen.getByText("db.internal")).toBeInTheDocument();
    });
  });

  it("opens the create env modal when 新建环境 is clicked", async () => {
    mockList.mockResolvedValue([]);
    renderPage();
    await userEvent.click(screen.getByRole("button", { name: /新建环境/ }));
    await waitFor(() => {
      expect(screen.getAllByText("新建环境").length).toBeGreaterThan(1);
    });
  });
});
