/**
 * Convert task data to Ansible YAML format string.
 * Accepts either a saved Task object or form values from the add/edit modal.
 */
export interface TaskYamlInput {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  become?: boolean;
  becomeUser?: string;
  ignoreErrors?: boolean;
  blockSection?: string;
  parentTaskId?: number | null;
  children?: TaskYamlInput[];
}

function yamlScalar(value: unknown): string {
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  if (typeof value === 'number') return String(value);
  const str = String(value);
  // Strings containing special YAML characters or Jinja2 expressions need quoting
  if (
    str.includes(':') ||
    str.includes('#') ||
    str.includes('{') ||
    str.includes('}') ||
    str.includes('[') ||
    str.includes(']') ||
    str.includes('"') ||
    str.includes("'") ||
    str.includes('\n') ||
    str === '' ||
    str === 'true' ||
    str === 'false' ||
    str === 'null' ||
    str === 'yes' ||
    str === 'no'
  ) {
    // Use double quotes, escape internal double quotes
    return `"${str.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`;
  }
  return str;
}

function renderLoop(loopValue: string): string {
  try {
    const parsed = JSON.parse(loopValue);
    if (Array.isArray(parsed) && parsed.length > 0) {
      const items = parsed.map((item: unknown) => `    - ${yamlScalar(item)}`);
      return `  loop:\n${items.join('\n')}`;
    }
  } catch {
    // not JSON — treat as Jinja2 expression
  }
  return `  loop: ${yamlScalar(loopValue)}`;
}

function renderModuleArgs(argsJson: string): string {
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(argsJson);
  } catch {
    // If args is not valid JSON, render as a raw string
    return ` ${yamlScalar(argsJson)}`;
  }

  if (Object.keys(parsed).length === 0) return '';

  const lines = Object.entries(parsed).map(
    ([key, value]) => `    ${key}: ${yamlScalar(value)}`,
  );
  return '\n' + lines.join('\n');
}

export function blockToYaml(task: TaskYamlInput): string {
  const lines: string[] = [];

  lines.push(`- name: ${yamlScalar(task.name || 'Unnamed task')}`);
  lines.push(`  block:`);

  const blockChildren = (task.children || []).filter(
    (c) => !c.blockSection || c.blockSection === 'BLOCK',
  );
  for (const child of blockChildren) {
    const childYaml = taskToYaml(child);
    const indented = childYaml.split('\n').map((l) => '    ' + l).join('\n');
    lines.push(indented);
  }

  const rescueChildren = (task.children || []).filter((c) => c.blockSection === 'RESCUE');
  if (rescueChildren.length > 0) {
    lines.push(`  rescue:`);
    for (const child of rescueChildren) {
      const childYaml = taskToYaml(child);
      const indented = childYaml.split('\n').map((l) => '    ' + l).join('\n');
      lines.push(indented);
    }
  }

  const alwaysChildren = (task.children || []).filter((c) => c.blockSection === 'ALWAYS');
  if (alwaysChildren.length > 0) {
    lines.push(`  always:`);
    for (const child of alwaysChildren) {
      const childYaml = taskToYaml(child);
      const indented = childYaml.split('\n').map((l) => '    ' + l).join('\n');
      lines.push(indented);
    }
  }

  if (task.whenCondition) {
    lines.push(`  when: ${yamlScalar(task.whenCondition)}`);
  }
  if (task.become) {
    lines.push('  become: true');
  }
  if (task.becomeUser) {
    lines.push(`  become_user: ${yamlScalar(task.becomeUser)}`);
  }
  if (task.ignoreErrors) {
    lines.push('  ignore_errors: true');
  }

  return lines.join('\n');
}

export function taskToYaml(task: TaskYamlInput): string {
  const lines: string[] = [];

  lines.push(`- name: ${yamlScalar(task.name || 'Unnamed task')}`);

  if (task.module) {
    const moduleHeader = `  ${task.module}:`;
    if (task.args) {
      lines.push(moduleHeader + renderModuleArgs(task.args));
    } else {
      lines.push(moduleHeader);
    }
  }

  if (task.whenCondition) {
    lines.push(`  when: ${yamlScalar(task.whenCondition)}`);
  }

  if (task.loop) {
    lines.push(renderLoop(task.loop));
  }

  if (task.until) {
    lines.push(`  until: ${yamlScalar(task.until)}`);
  }

  if (task.become) {
    lines.push('  become: true');
  }

  if (task.becomeUser) {
    lines.push(`  become_user: ${task.becomeUser}`);
  }

  if (task.ignoreErrors) {
    lines.push('  ignore_errors: true');
  }

  if (task.register) {
    lines.push(`  register: ${task.register}`);
  }

  if (task.notify && task.notify.length > 0) {
    lines.push('  notify:');
    for (const handler of task.notify) {
      lines.push(`    - ${yamlScalar(handler)}`);
    }
  }

  return lines.join('\n');
}
