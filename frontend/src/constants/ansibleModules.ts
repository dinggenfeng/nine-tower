export interface ModuleParamOption {
  label: string;
  value: string;
}

export interface ModuleParam {
  name: string;
  label: string;
  type: "input" | "select" | "switch";
  required: boolean;
  placeholder?: string;
  tooltip?: string;
  options?: ModuleParamOption[];
  defaultValue?: string | boolean;
}

export interface ModuleDefinition {
  name: string;
  label: string;
  description: string;
  docUrl: string;
  params: ModuleParam[];
  validate?: (values: Record<string, unknown>) => Record<string, string>;
}

export const ANSIBLE_MODULES: ModuleDefinition[] = [
  {
    name: "copy",
    label: "copy",
    description: "复制文件到远程主机",
    docUrl: "https://docs.ansible.com/ansible/latest/collections/ansible/builtin/copy_module.html",
    params: [
      {
        name: "src",
        label: "源文件路径 (src)",
        type: "input",
        required: false,
        placeholder: "/path/to/local/file",
        tooltip: "本地文件路径，与 content 二选一",
      },
      {
        name: "content",
        label: "文件内容 (content)",
        type: "input",
        required: false,
        placeholder: "直接写入远程文件的内容",
        tooltip: "直接指定文件内容，与 src 二选一",
      },
      {
        name: "dest",
        label: "目标路径 (dest)",
        type: "input",
        required: true,
        placeholder: "/path/to/remote/file",
        tooltip: "远程主机上的目标路径",
      },
      {
        name: "owner",
        label: "所有者 (owner)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "group",
        label: "所属组 (group)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "mode",
        label: "权限 (mode)",
        type: "input",
        required: false,
        placeholder: "0644",
      },
      {
        name: "backup",
        label: "备份 (backup)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "覆盖前是否备份原文件",
      },
      {
        name: "remote_src",
        label: "远程源 (remote_src)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "src 是否为远程主机上的路径",
      },
    ],
    validate: (values: Record<string, unknown>) => {
      const errors: Record<string, string> = {};
      if (!values.src && !values.content) {
        errors.src = "src 和 content 至少填写一个";
        errors.content = "src 和 content 至少填写一个";
      }
      if (values.src && values.content) {
        errors.content = "src 和 content 不能同时填写";
      }
      return errors;
    },
  },
  {
    name: "template",
    label: "template",
    description: "渲染 Jinja2 模板到远程主机",
    docUrl:
      "https://docs.ansible.com/ansible/latest/collections/ansible/builtin/template_module.html",
    params: [
      {
        name: "src",
        label: "模板路径 (src)",
        type: "input",
        required: true,
        placeholder: "templates/nginx.conf.j2",
        tooltip: "本地 Jinja2 模板文件路径",
      },
      {
        name: "dest",
        label: "目标路径 (dest)",
        type: "input",
        required: true,
        placeholder: "/etc/nginx/nginx.conf",
        tooltip: "远程主机上的目标路径",
      },
      {
        name: "owner",
        label: "所有者 (owner)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "group",
        label: "所属组 (group)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "mode",
        label: "权限 (mode)",
        type: "input",
        required: false,
        placeholder: "0644",
      },
      {
        name: "backup",
        label: "备份 (backup)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "覆盖前是否备份原文件",
      },
    ],
  },
  {
    name: "file",
    label: "file",
    description: "管理文件和目录属性",
    docUrl: "https://docs.ansible.com/ansible/latest/collections/ansible/builtin/file_module.html",
    params: [
      {
        name: "path",
        label: "路径 (path)",
        type: "input",
        required: true,
        placeholder: "/etc/myapp",
        tooltip: "要管理的文件或目录路径",
      },
      {
        name: "state",
        label: "状态 (state)",
        type: "select",
        required: false,
        tooltip: "目标状态类型",
        options: [
          { label: "file — 普通文件", value: "file" },
          { label: "directory — 目录", value: "directory" },
          { label: "link — 符号链接", value: "link" },
          { label: "hard — 硬链接", value: "hard" },
          { label: "touch — 创建空文件", value: "touch" },
          { label: "absent — 删除", value: "absent" },
        ],
      },
      {
        name: "src",
        label: "链接源 (src)",
        type: "input",
        required: false,
        placeholder: "/path/to/source",
        tooltip: "state 为 link 或 hard 时，链接指向的源路径",
      },
      {
        name: "owner",
        label: "所有者 (owner)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "group",
        label: "所属组 (group)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "mode",
        label: "权限 (mode)",
        type: "input",
        required: false,
        placeholder: "0755",
      },
      {
        name: "recurse",
        label: "递归 (recurse)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "递归设置目录属性",
      },
    ],
  },
  {
    name: "shell",
    label: "shell",
    description: "在远程主机上执行 Shell 命令",
    docUrl: "https://docs.ansible.com/ansible/latest/collections/ansible/builtin/shell_module.html",
    params: [
      {
        name: "cmd",
        label: "命令 (cmd)",
        type: "input",
        required: true,
        placeholder: "ls -la /tmp",
        tooltip: "要执行的 Shell 命令",
      },
      {
        name: "chdir",
        label: "工作目录 (chdir)",
        type: "input",
        required: false,
        placeholder: "/opt/app",
        tooltip: "执行命令前切换到此目录",
      },
      {
        name: "creates",
        label: "创建标记 (creates)",
        type: "input",
        required: false,
        placeholder: "/path/to/file",
        tooltip: "如果该文件已存在则跳过执行",
      },
      {
        name: "removes",
        label: "删除标记 (removes)",
        type: "input",
        required: false,
        placeholder: "/path/to/file",
        tooltip: "如果该文件不存在则跳过执行",
      },
      {
        name: "executable",
        label: "解释器 (executable)",
        type: "input",
        required: false,
        placeholder: "/bin/bash",
        tooltip: "用于执行命令的 Shell 解释器路径",
      },
    ],
  },
  {
    name: "known_hosts",
    label: "known_hosts",
    description: "管理 SSH known_hosts 文件",
    docUrl:
      "https://docs.ansible.com/ansible/latest/collections/ansible/builtin/known_hosts_module.html",
    params: [
      {
        name: "name",
        label: "主机名 (name)",
        type: "input",
        required: true,
        placeholder: "github.com",
        tooltip: "要添加或移除的主机名或 IP 地址",
      },
      {
        name: "key",
        label: "公钥 (key)",
        type: "input",
        required: false,
        placeholder: "github.com ssh-rsa AAAA...",
        tooltip: "SSH 公钥行，state=present 时必填",
      },
      {
        name: "path",
        label: "文件路径 (path)",
        type: "input",
        required: false,
        placeholder: "/etc/ssh/ssh_known_hosts",
        tooltip: "known_hosts 文件路径，默认 ~/.ssh/known_hosts",
      },
      {
        name: "state",
        label: "状态 (state)",
        type: "select",
        required: false,
        tooltip: "添加或移除主机密钥",
        options: [
          { label: "present — 添加", value: "present" },
          { label: "absent — 移除", value: "absent" },
        ],
      },
      {
        name: "hash_host",
        label: "哈希主机名 (hash_host)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "是否对主机名进行哈希处理",
      },
    ],
    validate: (values: Record<string, unknown>) => {
      const errors: Record<string, string> = {};
      if ((!values.state || values.state === "present") && !values.key) {
        errors.key = "state 为 present 时必须填写公钥";
      }
      return errors;
    },
  },
  {
    name: "unarchive",
    label: "unarchive",
    description: "解压归档文件到远程主机",
    docUrl:
      "https://docs.ansible.com/ansible/latest/collections/ansible/builtin/unarchive_module.html",
    params: [
      {
        name: "src",
        label: "源文件 (src)",
        type: "input",
        required: true,
        placeholder: "/path/to/archive.tar.gz",
        tooltip: "归档文件路径（本地或远程）",
      },
      {
        name: "dest",
        label: "目标目录 (dest)",
        type: "input",
        required: true,
        placeholder: "/opt/app",
        tooltip: "解压到的远程目录",
      },
      {
        name: "remote_src",
        label: "远程源 (remote_src)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "src 是否为远程主机上的路径",
      },
      {
        name: "owner",
        label: "所有者 (owner)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "group",
        label: "所属组 (group)",
        type: "input",
        required: false,
        placeholder: "root",
      },
      {
        name: "mode",
        label: "权限 (mode)",
        type: "input",
        required: false,
        placeholder: "0755",
      },
      {
        name: "creates",
        label: "创建标记 (creates)",
        type: "input",
        required: false,
        placeholder: "/opt/app/bin/myapp",
        tooltip: "如果该文件已存在则跳过解压",
      },
      {
        name: "list_files",
        label: "列出文件 (list_files)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "解压后是否在结果中列出文件清单",
      },
    ],
  },
  {
    name: "block",
    label: "block",
    description: "将多个任务组合为块，支持 rescue 和 always",
    docUrl: "https://docs.ansible.com/ansible/latest/playbook_guide/playbooks_blocks.html",
    params: [
      {
        name: "when",
        label: "条件 (when)",
        type: "input",
        required: false,
        placeholder: 'ansible_os_family == "Debian"',
        tooltip: "整个 block 的执行条件",
      },
      {
        name: "ignore_errors",
        label: "忽略错误 (ignore_errors)",
        type: "switch",
        required: false,
        defaultValue: false,
        tooltip: "是否忽略 block 内任务的错误",
      },
    ],
  },
];

export function getModuleDefinition(name: string): ModuleDefinition | undefined {
  return ANSIBLE_MODULES.find((m) => m.name === name);
}
