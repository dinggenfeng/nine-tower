export interface Template {
  id: number;
  roleId: number;
  parentDir: string | null;
  name: string;
  targetPath: string | null;
  content: string | null;
  isDirectory: boolean;
  size: number | null;
  children: Template[] | null;
  createdBy: number;
  createdAt: string;
}

export interface CreateTemplateRequest {
  name: string;
  parentDir?: string;
  targetPath?: string;
  content?: string;
  isDirectory?: boolean;
}

export interface UpdateTemplateRequest {
  name?: string;
  parentDir?: string;
  targetPath?: string;
  content?: string;
  isDirectory?: boolean;
}
