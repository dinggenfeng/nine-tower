import { describe, it, expect, beforeEach } from "vitest";
import type { Project } from "../../types/entity/Project";
import { useProjectStore } from "../projectStore";

function makeProject(overrides: Partial<Project> = {}): Project {
  return { id: 1, name: "Test", ...overrides } as Project;
}

describe("useProjectStore", () => {
  beforeEach(() => {
    useProjectStore.setState({ currentProject: null });
  });

  it("starts with null currentProject", () => {
    expect(useProjectStore.getState().currentProject).toBeNull();
  });

  it("setCurrentProject sets the project", () => {
    useProjectStore.getState().setCurrentProject(makeProject({ id: 1, name: "Test" }));
    expect(useProjectStore.getState().currentProject?.name).toBe("Test");
  });

  it("setCurrentProject with null clears the project", () => {
    useProjectStore.getState().setCurrentProject(makeProject({ id: 1 }));
    useProjectStore.getState().setCurrentProject(null);
    expect(useProjectStore.getState().currentProject).toBeNull();
  });
});
