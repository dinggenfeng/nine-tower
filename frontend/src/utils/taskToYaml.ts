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
    lines.push(`  loop: ${yamlScalar(task.loop)}`);
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
