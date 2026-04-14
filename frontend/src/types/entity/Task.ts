export interface Task {
  id: number;
  roleId: number;
  name: string;
  module: string;
  args: string;
  whenCondition: string;
  loop: string;
  until: string;
  register: string;
  notify: string[];
  taskOrder: number;
  createdBy: number;
  createdAt: string;
}

export interface CreateTaskRequest {
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder: number;
}

export interface UpdateTaskRequest {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder?: number;
}

export interface Handler {
  id: number;
  roleId: number;
  name: string;
  module: string;
  args: string;
  whenCondition: string;
  register: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateHandlerRequest {
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  register?: string;
}

export interface UpdateHandlerRequest {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  register?: string;
}
