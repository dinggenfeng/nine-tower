import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BlockTasksEditor from '../BlockTasksEditor';
import type { BlockChildRequest } from '../../../types/entity/Task';

// Mock ModuleSelect to a simple controlled <select>
vi.mock('../ModuleSelect', () => ({
  default: ({ value, onChange }: { value?: string; onChange?: (v: string) => void }) => (
    <select
      data-testid="module-select"
      value={value ?? ''}
      onChange={(e) => onChange?.(e.target.value)}
    >
      <option value="">--</option>
      <option value="apt">apt</option>
      <option value="copy">copy</option>
    </select>
  ),
}));

const sampleBlockChildren: BlockChildRequest[] = [
  {
    section: 'BLOCK',
    name: 'Install nginx',
    module: 'apt',
    args: '{"name":"nginx"}',
    taskOrder: 0,
  },
  {
    section: 'RESCUE',
    name: 'Handle failure',
    module: 'shell',
    args: '{"cmd":"echo failed"}',
    taskOrder: 0,
  },
];

describe('BlockTasksEditor', () => {
  it('renders three tabs', () => {
    render(<BlockTasksEditor blockChildren={[]} onChange={vi.fn()} />);
    expect(screen.getByText(/block（必填）/)).toBeInTheDocument();
    expect(screen.getByText(/rescue（可选）/)).toBeInTheDocument();
    expect(screen.getByText(/always（可选）/)).toBeInTheDocument();
  });

  it('shows empty state when no children', () => {
    render(<BlockTasksEditor blockChildren={[]} onChange={vi.fn()} />);
    expect(screen.getByText('暂无 block 子任务，请添加')).toBeInTheDocument();
  });

  it('renders existing block children as cards', () => {
    render(<BlockTasksEditor blockChildren={sampleBlockChildren} onChange={vi.fn()} />);
    // Card title includes index prefix: "1. Install nginx"
    expect(screen.getByText(/Install nginx/)).toBeInTheDocument();
  });

  it('renders rescue children when rescue tab is clicked', async () => {
    render(<BlockTasksEditor blockChildren={sampleBlockChildren} onChange={vi.fn()} />);
    await userEvent.click(screen.getByText(/rescue（可选）/));
    await waitFor(() => {
      expect(screen.getByText(/Handle failure/)).toBeInTheDocument();
    });
  });

  it('adds a new block child when button is clicked', async () => {
    const onChange = vi.fn();
    render(<BlockTasksEditor blockChildren={[]} onChange={onChange} />);
    await userEvent.click(screen.getByText('添加 block 子任务'));
    // The card should render — search for the "未命名子任务" text inside a span
    await waitFor(() => {
      expect(screen.getByText(/未命名子任务/)).toBeInTheDocument();
    });
  });

  it('calls onChange when child is deleted', async () => {
    const onChange = vi.fn();
    render(
      <BlockTasksEditor blockChildren={[sampleBlockChildren[0]]} onChange={onChange} />,
    );
    // Find the delete icon button (it has anticon-delete class inside)
    const deleteBtn = document.querySelector('button .anticon-delete');
    expect(deleteBtn).toBeTruthy();
    await userEvent.click(deleteBtn!.closest('button')!);
    // Ant Design 5 Popconfirm: click the OK button in the popup
    const okBtn = await screen.findByText('OK');
    await userEvent.click(okBtn);
    await waitFor(() => {
      expect(onChange).toHaveBeenCalled();
    });
  });
});
