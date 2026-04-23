import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoleDefaults from '../RoleDefaults';

vi.mock('../../../api/roleVariable', () => ({
  getRoleVariables: vi.fn(),
  createRoleVariable: vi.fn(),
  updateRoleVariable: vi.fn(),
  deleteRoleVariable: vi.fn(),
  getRoleDefaults: vi.fn(),
  createRoleDefault: vi.fn(),
  updateRoleDefault: vi.fn(),
  deleteRoleDefault: vi.fn(),
}));

import { getRoleDefaults } from '../../../api/roleVariable';
const mockGet = vi.mocked(getRoleDefaults);

const baseVar = {
  id: 0,
  roleId: 5,
  key: '',
  value: '',
  createdBy: 1,
  createdAt: '',
  updatedAt: '',
};

describe('RoleDefaults', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches defaults for the given roleId on mount', async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleDefaults roleId={5} />);
    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(5));
  });

  it('renders default variable rows in the table', async () => {
    mockGet.mockResolvedValue([
      { ...baseVar, id: 1, key: 'default_port', value: '8080' },
    ]);
    render(<RoleDefaults roleId={5} />);
    await waitFor(() => {
      expect(screen.getByText('default_port')).toBeInTheDocument();
      expect(screen.getByText('8080')).toBeInTheDocument();
    });
  });

  it('opens the create modal when 添加默认变量 is clicked', async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleDefaults roleId={5} />);
    await userEvent.click(screen.getByRole('button', { name: /添加默认变量/ }));
    await waitFor(() => {
      // Modal title + button both say 添加默认变量
      expect(screen.getAllByText('添加默认变量').length).toBeGreaterThan(1);
    });
  });
});
