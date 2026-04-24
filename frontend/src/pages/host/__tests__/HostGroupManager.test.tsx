import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import HostGroupManager from "../HostGroupManager";

vi.mock("../../../api/host", () => ({
  getHostGroups: vi.fn(),
  createHostGroup: vi.fn(),
  updateHostGroup: vi.fn(),
  deleteHostGroup: vi.fn(),
  getHosts: vi.fn(),
  createHost: vi.fn(),
  updateHost: vi.fn(),
  deleteHost: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useParams: () => ({ id: "11" }) };
});

import { getHostGroups, getHosts } from "../../../api/host";
const mockGetHostGroups = vi.mocked(getHostGroups);
const mockGetHosts = vi.mocked(getHosts);

function renderPage() {
  return render(
    <MemoryRouter>
      <HostGroupManager />
    </MemoryRouter>
  );
}

const baseHg = {
  id: 0,
  projectId: 11,
  name: "",
  description: "",
  createdBy: 1,
  createdAt: "",
};

const baseHost = {
  id: 0,
  hostGroupId: 0,
  name: "",
  ip: "",
  port: 22,
  ansibleUser: "root",
  ansibleSshPass: "",
  ansibleSshPrivateKeyFile: "",
  ansibleBecome: false,
  createdBy: 1,
  createdAt: "",
};

describe("HostGroupManager", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches host groups for the current project on mount", async () => {
    mockGetHostGroups.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockGetHostGroups).toHaveBeenCalledWith(11));
  });

  it("renders host group names in the left panel", async () => {
    mockGetHostGroups.mockResolvedValue([
      { ...baseHg, id: 1, name: "web_servers" },
      { ...baseHg, id: 2, name: "db_servers" },
    ]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("web_servers")).toBeInTheDocument();
      expect(screen.getByText("db_servers")).toBeInTheDocument();
    });
  });

  it("loads hosts when a host group is selected", async () => {
    mockGetHostGroups.mockResolvedValue([{ ...baseHg, id: 42, name: "web_servers" }]);
    mockGetHosts.mockResolvedValue([
      { ...baseHost, id: 100, hostGroupId: 42, name: "web1", ip: "10.0.0.1" },
    ]);
    renderPage();
    await userEvent.click(await screen.findByText("web_servers"));
    await waitFor(() => {
      expect(mockGetHosts).toHaveBeenCalledWith(42);
      expect(screen.getByText("web1")).toBeInTheDocument();
      expect(screen.getByText("10.0.0.1")).toBeInTheDocument();
    });
  });

  it("shows empty placeholder when no host group is selected", async () => {
    mockGetHostGroups.mockResolvedValue([]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("请从左侧选择一个主机组")).toBeInTheDocument();
    });
  });
});
