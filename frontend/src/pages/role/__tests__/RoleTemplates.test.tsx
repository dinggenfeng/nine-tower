import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RoleTemplates from "../RoleTemplates";

vi.mock("../../../api/template", () => ({
  createTemplate: vi.fn(),
  getTemplates: vi.fn(),
  getTemplate: vi.fn(),
  updateTemplate: vi.fn(),
  deleteTemplate: vi.fn(),
  downloadTemplate: vi.fn(),
}));

vi.mock("@uiw/react-codemirror", () => ({
  default: () => <div data-testid="codemirror">CodeMirror</div>,
}));

vi.mock("@codemirror/lang-yaml", () => ({
  yaml: () => ({}),
}));

import { getTemplates } from "../../../api/template";
const mockGet = vi.mocked(getTemplates);

const baseTemplate = {
  id: 0,
  roleId: 4,
  parentDir: null,
  name: "",
  targetPath: null,
  content: null,
  isDirectory: false,
  size: null,
  children: null,
  createdBy: 1,
  createdAt: "",
};

describe("RoleTemplates", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches templates for the given roleId on mount", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleTemplates roleId={4} />);
    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(4));
  });

  it("renders template names in the tree", async () => {
    mockGet.mockResolvedValue([
      {
        ...baseTemplate,
        id: 1,
        name: "conf.d",
        isDirectory: true,
        children: [
          {
            ...baseTemplate,
            id: 2,
            name: "nginx.conf.j2",
            isDirectory: false,
            size: 1024,
          },
        ],
      },
      { ...baseTemplate, id: 3, name: "app.yml.j2" },
    ]);
    render(<RoleTemplates roleId={4} />);
    await waitFor(() => {
      expect(screen.getByText("conf.d")).toBeInTheDocument();
      expect(screen.getByText("nginx.conf.j2")).toBeInTheDocument();
      expect(screen.getByText("app.yml.j2")).toBeInTheDocument();
    });
  });

  it("opens the create modal when 新建模板 is clicked", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleTemplates roleId={4} />);
    await userEvent.click(screen.getByRole("button", { name: /新建模板/ }));
    await waitFor(() => {
      expect(screen.getAllByText("新建模板").length).toBeGreaterThan(1);
    });
  });

  it("opens the create-directory modal when 新建目录 is clicked", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleTemplates roleId={4} />);
    await userEvent.click(screen.getByRole("button", { name: /新建目录/ }));
    await waitFor(() => {
      expect(screen.getAllByText("新建目录").length).toBeGreaterThan(1);
    });
  });
});
