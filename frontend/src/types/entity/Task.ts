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
  become: boolean;
  becomeUser: string;
  ignoreErrors: boolean;
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
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
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
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
}

export interface Handler {
  id: number;
  roleId: number;
  name: string;
  module: string;
  args: string;
  whenCondition: string;
  register: string;
  become: boolean;
  becomeUser: string;
  ignoreErrors: boolean;
  createdBy: number;
  createdAt: string;
}

export interface CreateHandlerRequest {
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  register?: string;
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
}

export interface UpdateHandlerRequest {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  register?: string;
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
}
