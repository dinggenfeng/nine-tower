import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import MemberManagement from '../MemberManagement';

vi.mock('../../../api/project', () => ({
  getMembers: vi.fn(),
  addMember: vi.fn(),
  removeMember: vi.fn(),
  updateMemberRole: vi.fn(),
}));

vi.mock('../../../stores/projectStore', () => ({
  useProjectStore: () => ({
    currentProject: { id: 5, name: 'proj', myRole: 'PROJECT_ADMIN' },
  }),
}));

vi.mock('../../../stores/authStore', () => ({
  useAuthStore: (selector: (s: { user: { id: number; username: string; email: string } | null }) => unknown) =>
    selector({ user: { id: 1, username: 'admin', email: 'admin@test.com' } }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => ({ id: '5' }) };
});

import { getMembers } from '../../../api/project';
const mockGetMembers = vi.mocked(getMembers);

function renderPage() {
  return render(
    <MemoryRouter>
      <MemberManagement />
    </MemoryRouter>,
  );
}

const baseMember = {
  id: 0,
  projectId: 5,
  userId: 0,
  username: '',
  email: '',
  role: 'PROJECT_MEMBER' as const,
  joinedAt: '2026-01-01T00:00:00Z',
};

describe('MemberManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches members for the current project on mount', async () => {
    mockGetMembers.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockGetMembers).toHaveBeenCalledWith(5));
  });

  it('renders member rows with username and email', async () => {
    mockGetMembers.mockResolvedValue([
      { ...baseMember, userId: 1, username: 'alice', email: 'a@x.test', role: 'PROJECT_ADMIN' },
      { ...baseMember, userId: 2, username: 'bob', email: 'b@x.test' },
    ]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('alice')).toBeInTheDocument();
      expect(screen.getByText('bob')).toBeInTheDocument();
      expect(screen.getByText('a@x.test')).toBeInTheDocument();
    });
  });

  it('shows the 添加成员 button for admins and opens the modal', async () => {
    mockGetMembers.mockResolvedValue([]);
    renderPage();
    const addBtn = await screen.findByRole('button', { name: /添加成员/ });
    await userEvent.click(addBtn);
    await waitFor(() => {
      // Modal title
      expect(screen.getAllByText('添加成员').length).toBeGreaterThan(1);
    });
  });

  it('does not show Select or remove button for the current user row', async () => {
    mockGetMembers.mockResolvedValue([
      { ...baseMember, userId: 1, username: 'admin', email: 'a@x.test', role: 'PROJECT_ADMIN' },
      { ...baseMember, userId: 2, username: 'bob', email: 'b@x.test', role: 'PROJECT_MEMBER' },
    ]);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
      expect(screen.getByText('bob')).toBeInTheDocument();
    });

    const adminRow = screen.getByText('admin').closest('tr');
    expect(adminRow).toBeTruthy();
    expect(adminRow!.querySelector('.ant-select')).toBeNull();

    const bobRow = screen.getByText('bob').closest('tr');
    expect(bobRow!.querySelector('.ant-select')).toBeTruthy();

    const removeButtons = screen.queryAllByRole('button', { name: /移除/ });
    expect(removeButtons).toHaveLength(1);
  });
});
