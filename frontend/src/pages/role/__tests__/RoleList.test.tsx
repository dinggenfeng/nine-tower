import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import RoleList from '../RoleList';

vi.mock('../../../api/role', () => ({
  getRoles: vi.fn(),
  createRole: vi.fn(),
  updateRole: vi.fn(),
  deleteRole: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => ({ id: '3' }), useNavigate: () => vi.fn() };
});

import { getRoles } from '../../../api/role';
const mockGetRoles = vi.mocked(getRoles);

function renderPage() {
  return render(
    <MemoryRouter>
      <RoleList />
    </MemoryRouter>,
  );
}

const baseRole = {
  id: 0,
  projectId: 3,
  name: '',
  description: '',
  createdBy: 1,
  createdAt: '',
  updatedAt: '',
};

describe('RoleList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches roles for the current project on mount', async () => {
    mockGetRoles.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockGetRoles).toHaveBeenCalledWith(3));
  });

  it('renders role rows with name and description', async () => {
    mockGetRoles.mockResolvedValue([
      { ...baseRole, id: 10, name: 'nginx', description: 'web server' },
    ]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('nginx')).toBeInTheDocument();
      expect(screen.getByText('web server')).toBeInTheDocument();
    });
  });

  it('opens the create modal when 新建 Role is clicked', async () => {
    mockGetRoles.mockResolvedValue([]);
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: /新建 Role/ }));
    await waitFor(() => {
      // Modal title
      expect(screen.getAllByText('新建 Role').length).toBeGreaterThan(1);
    });
  });
});
