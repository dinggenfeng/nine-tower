import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoleFiles from '../RoleFiles';

vi.mock('../../../api/roleFile', () => ({
  getFiles: vi.fn(),
  createFile: vi.fn(),
  updateFile: vi.fn(),
  deleteFile: vi.fn(),
  getFileDownloadUrl: (id: number) => `/api/files/${id}/download`,
}));

import { getFiles } from '../../../api/roleFile';
const mockGet = vi.mocked(getFiles);

const baseFile = {
  id: 0,
  roleId: 3,
  parentDir: '',
  name: '',
  isDirectory: false,
  size: 0,
  textContent: '',
  children: undefined as undefined | unknown[],
  createdBy: 1,
  createdAt: '',
  updatedAt: '',
};

describe('RoleFiles', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches files for the given roleId on mount', async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleFiles roleId={3} />);
    await waitFor(() => expect(mockGet).toHaveBeenCalledWith(3));
  });

  it('renders file and directory names from the tree', async () => {
    mockGet.mockResolvedValue([
      { ...baseFile, id: 1, name: 'config', isDirectory: true },
      { ...baseFile, id: 2, name: 'README.md', isDirectory: false, size: 1024 },
    ]);
    render(<RoleFiles roleId={3} />);
    await waitFor(() => {
      expect(screen.getByText('config')).toBeInTheDocument();
      expect(screen.getByText('README.md')).toBeInTheDocument();
    });
  });

  it('opens the create-file modal when 新建文件 is clicked', async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleFiles roleId={3} />);
    await userEvent.click(screen.getByRole('button', { name: /新建文件/ }));
    await waitFor(() => {
      expect(screen.getAllByText('新建文件').length).toBeGreaterThan(1);
    });
  });

  it('opens the create-directory modal when 新建目录 is clicked', async () => {
    mockGet.mockResolvedValue([]);
    render(<RoleFiles roleId={3} />);
    await userEvent.click(screen.getByRole('button', { name: /新建目录/ }));
    await waitFor(() => {
      expect(screen.getAllByText('新建目录').length).toBeGreaterThan(1);
    });
  });
});
