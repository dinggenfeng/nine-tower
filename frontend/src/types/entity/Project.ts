export interface Project {
  id: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string;
  myRole: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
}

export interface ProjectMember {
  userId: number;
  username: string;
  email: string;
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
  joinedAt: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
}

export interface AddMemberRequest {
  userId: number;
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
}

export interface UpdateMemberRoleRequest {
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
}
