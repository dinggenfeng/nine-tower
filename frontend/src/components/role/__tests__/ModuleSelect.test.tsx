import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ModuleSelect from "../ModuleSelect";

function renderModuleSelect(
  props: { value?: string; onChange?: (v: string) => void; filterModule?: string } = {}
) {
  return render(<ModuleSelect {...props} />);
}

describe("ModuleSelect", () => {
  it("renders the select component", () => {
    renderModuleSelect();
    expect(screen.getByRole("combobox")).toBeInTheDocument();
  });

  it("shows module options when opened", async () => {
    renderModuleSelect();
    await userEvent.click(screen.getByRole("combobox"));
    // Options render in a dropdown — wait for them to appear
    await waitFor(() => {
      expect(screen.getByText("复制文件到远程主机")).toBeInTheDocument();
    });
    // Several modules should be visible
    expect(screen.getByText("管理文件和目录属性")).toBeInTheDocument();
    expect(screen.getByText("在远程主机上执行 Shell 命令")).toBeInTheDocument();
  });

  it("filters out filterModule", async () => {
    renderModuleSelect({ filterModule: "block" });
    await userEvent.click(screen.getByRole("combobox"));
    await waitFor(() => {
      // Some module should be visible
      expect(screen.getByText("复制文件到远程主机")).toBeInTheDocument();
    });
    // The block module description should NOT be present
    expect(screen.queryByText("将多个任务组合为块")).not.toBeInTheDocument();
  });

  it("calls onChange when an option is selected", async () => {
    const onChange = vi.fn();
    renderModuleSelect({ onChange });
    await userEvent.click(screen.getByRole("combobox"));
    // Wait for dropdown to open and click the first visible option
    await waitFor(() => {
      expect(screen.getByText("复制文件到远程主机")).toBeInTheDocument();
    });
    // Ant Design Select options have value attribute — find the "copy" option and click
    const copyOption = document.querySelector(".ant-select-item-option");
    expect(copyOption).toBeTruthy();
    await userEvent.click(copyOption!);
    expect(onChange).toHaveBeenCalledWith("copy", expect.anything());
  });
});
