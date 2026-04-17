export type BlockSection = 'BLOCK' | 'RESCUE' | 'ALWAYS';

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
  parentTaskId: number | null;
  blockSection: BlockSection | null;
  children: Task[];
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
  blockChildren?: BlockChildRequest[];
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
  blockChildren?: BlockChildRequest[];
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

export interface BlockChildRequest {
  section: BlockSection;
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
