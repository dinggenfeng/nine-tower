export interface EnvConfig {
  id: number;
  environmentId: number;
  configKey: string;
  configValue: string;
}

export interface Environment {
  id: number;
  projectId: number;
  name: string;
  description: string | null;
  configs: EnvConfig[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateEnvironmentRequest {
  name: string;
  description?: string;
}

export interface UpdateEnvironmentRequest {
  name: string;
  description?: string;
}

export interface EnvConfigRequest {
  configKey: string;
  configValue: string;
}
