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
  getTemplateDownloadUrl: (id: number) => `/api/templates/${id}/download`,
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
  parentDir: "",
  name: "",
  targetPath: "",
  content: "",
  createdBy: 1,
  createdAt: "",
  updatedAt: "",
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
      { ...baseTemplate, id: 1, name: "nginx.conf.j2", parentDir: "nginx" },
      { ...baseTemplate, id: 2, name: "app.yml.j2" },
    ]);
    render(<RoleTemplates roleId={4} />);
    await waitFor(() => {
      expect(screen.getByText("nginx.conf.j2")).toBeInTheDocument();
      expect(screen.getByText("app.yml.j2")).toBeInTheDocument();
      // Directory node
      expect(screen.getByText("nginx")).toBeInTheDocument();
    });
  });

  it("opens the create modal when 添加模板 is clicked", async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleTemplates roleId={4} />);
    await userEvent.click(screen.getByRole("button", { name: /添加模板/ }));
    await waitFor(() => {
      expect(screen.getAllByText("添加模板").length).toBeGreaterThan(1);
    });
  });
});
