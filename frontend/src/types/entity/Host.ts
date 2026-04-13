export interface HostGroup {
  id: number;
  projectId: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string;
}

export interface Host {
  id: number;
  hostGroupId: number;
  name: string;
  ip: string;
  port: number;
  ansibleUser: string;
  ansibleSshPass: string;
  ansibleSshPrivateKeyFile: string;
  ansibleBecome: boolean;
  createdBy: number;
  createdAt: string;
}

export interface CreateHostGroupRequest {
  name: string;
  description?: string;
}

export interface UpdateHostGroupRequest {
  name?: string;
  description?: string;
}

export interface CreateHostRequest {
  name: string;
  ip: string;
  port?: number;
  ansibleUser?: string;
  ansibleSshPass?: string;
  ansibleSshPrivateKeyFile?: string;
  ansibleBecome?: boolean;
}

export interface UpdateHostRequest {
  name?: string;
  ip?: string;
  port?: number;
  ansibleUser?: string;
  ansibleSshPass?: string;
  ansibleSshPrivateKeyFile?: string;
  ansibleBecome?: boolean;
}
