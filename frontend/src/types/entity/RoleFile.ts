export interface RoleFile {
  id: number;
  roleId: number;
  parentDir: string;
  name: string;
  isDirectory: boolean;
  size: number | null;
  textContent: string | null;
  children: RoleFile[] | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFileRequest {
  parentDir: string;
  name: string;
  isDirectory: boolean;
  textContent?: string;
}

export interface UpdateFileRequest {
  name: string;
  textContent?: string;
}
