import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import TagManager from '../TagManager';

vi.mock('../../../api/tag', () => ({
  listTags: vi.fn(),
  createTag: vi.fn(),
  updateTag: vi.fn(),
  deleteTag: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => ({ id: '7' }) };
});

import { listTags } from '../../../api/tag';
const mockListTags = vi.mocked(listTags);

function renderPage() {
  return render(
    <MemoryRouter>
      <TagManager />
    </MemoryRouter>,
  );
}

describe('TagManager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches tags for the current project on mount', async () => {
    mockListTags.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockListTags).toHaveBeenCalledWith(7));
  });

  it('renders the loaded tag rows in the table', async () => {
    mockListTags.mockResolvedValue([
      { id: 1, name: 'deploy', projectId: 7, createdAt: '', updatedAt: '' },
      { id: 2, name: 'staging', projectId: 7, createdAt: '', updatedAt: '' },
    ]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('deploy')).toBeInTheDocument();
      expect(screen.getByText('staging')).toBeInTheDocument();
    });
  });

  it('opens the create modal when 新建标签 is clicked', async () => {
    mockListTags.mockResolvedValue([]);
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: /新建标签/ }));
    await waitFor(() => {
      // Modal title appears
      expect(screen.getAllByText('新建标签').length).toBeGreaterThan(1);
    });
  });
});
