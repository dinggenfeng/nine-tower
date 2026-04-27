export interface RoleFile {
  id: number;
  roleId: number;
  parentDir: string;
  name: string;
  targetPath: string | null;
  isDirectory: boolean;
  size: number | null;
  textContent: string | null;
  children: RoleFile[] | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFileRequest {
  parentDir?: string;
  name: string;
  targetPath?: string;
  isDirectory: boolean;
  textContent?: string;
}

export interface UpdateFileRequest {
  name?: string;
  parentDir?: string;
  targetPath?: string;
  textContent?: string;
}
