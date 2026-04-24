import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Form } from "antd";
import { ModuleParamsGrid, ExtraParamsInput } from "../ModuleParamsForm";

function renderInForm(ui: React.ReactNode) {
  return render(<Form>{ui}</Form>);
}

describe("ModuleParamsGrid", () => {
  it("renders nothing when moduleName is undefined", () => {
    const { container } = renderInForm(<ModuleParamsGrid moduleName={undefined} />);
    // Should not render any form items
    const formItems = container.querySelectorAll(".ant-form-item");
    expect(formItems.length).toBe(0);
  });

  it('renders form items for "copy" module', () => {
    renderInForm(<ModuleParamsGrid moduleName="copy" />);
    // copy module has dest (required), src, content, owner, group, mode, backup (switch), remote_src (switch)
    expect(screen.getByText(/目标路径 \(dest\)/)).toBeInTheDocument();
    expect(screen.getByText(/备份 \(backup\)/)).toBeInTheDocument();
  });

  it('renders select for "file" module state param', () => {
    renderInForm(<ModuleParamsGrid moduleName="file" />);
    expect(screen.getByText(/状态 \(state\)/)).toBeInTheDocument();
  });
});

describe("ExtraParamsInput", () => {
  it('renders "添加参数" button', () => {
    renderInForm(<ExtraParamsInput />);
    expect(screen.getByText("添加参数")).toBeInTheDocument();
  });

  it("adds a key-value row on click", async () => {
    renderInForm(<ExtraParamsInput />);
    await userEvent.click(screen.getByText("添加参数"));
    // After clicking, two input fields (参数名 + 参数值) should appear
    const inputs = screen.getAllByPlaceholderText("参数名");
    expect(inputs.length).toBe(1);
    const valueInputs = screen.getAllByPlaceholderText("参数值");
    expect(valueInputs.length).toBe(1);
  });
});
