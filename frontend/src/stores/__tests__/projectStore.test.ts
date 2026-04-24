import { describe, it, expect, beforeEach } from "vitest";
import { useProjectStore } from "../projectStore";

describe("useProjectStore", () => {
  beforeEach(() => {
    useProjectStore.setState({ currentProject: null });
  });

  it("starts with null currentProject", () => {
    expect(useProjectStore.getState().currentProject).toBeNull();
  });

  it("setCurrentProject sets the project", () => {
    const project = { id: 1, name: "Test" } as any;
    useProjectStore.getState().setCurrentProject(project);
    expect(useProjectStore.getState().currentProject?.name).toBe("Test");
  });

  it("setCurrentProject with null clears the project", () => {
    useProjectStore.getState().setCurrentProject({ id: 1 } as any);
    useProjectStore.getState().setCurrentProject(null);
    expect(useProjectStore.getState().currentProject).toBeNull();
  });
});
