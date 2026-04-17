import { describe, it, expect } from 'vitest';
import { blockToYaml } from '../taskToYaml';

describe('blockToYaml', () => {
  it('renders block with children', () => {
    const blockTask = {
      name: 'SSH block',
      module: 'block',
      whenCondition: "{{ ssh_enabled }}",
      children: [
        {
          name: 'Get SSH key',
          module: 'command',
          args: '{"_raw_params": "ssh-keyscan -H {{ item }}"}',
          register: 'ssh_keyscan_result',
          loop: "{{ groups['all'] }}",
          blockSection: 'BLOCK',
        },
      ],
    };
    const yaml = blockToYaml(blockTask);
    expect(yaml).toContain('- name: SSH block');
    expect(yaml).toContain('  block:');
    expect(yaml).toContain('    - name: Get SSH key');
    expect(yaml).toContain('      command:');
    expect(yaml).toContain('        _raw_params: "ssh-keyscan -H {{ item }}"');
    expect(yaml).toContain('      register: ssh_keyscan_result');
    expect(yaml).toContain("      loop: \"{{ groups['all'] }}\"");
    expect(yaml).toContain('  when: "{{ ssh_enabled }}"');
  });

  it('renders rescue and always sections', () => {
    const blockTask = {
      name: 'Block with rescue',
      module: 'block',
      children: [
        { name: 'Main', module: 'shell', args: '{}', blockSection: 'BLOCK', taskOrder: 1 },
        {
          name: 'Recovery',
          module: 'debug',
          args: '{"msg": "failed"}',
          blockSection: 'RESCUE',
          taskOrder: 1,
        },
        {
          name: 'Cleanup',
          module: 'file',
          args: '{"path": "/tmp/x", "state": "absent"}',
          blockSection: 'ALWAYS',
          taskOrder: 1,
        },
      ],
    };
    const yaml = blockToYaml(blockTask);
    expect(yaml).toContain('  block:');
    expect(yaml).toContain('    - name: Main');
    expect(yaml).toContain('  rescue:');
    expect(yaml).toContain('    - name: Recovery');
    expect(yaml).toContain('  always:');
    expect(yaml).toContain('    - name: Cleanup');
  });

  it('does not render empty rescue/always sections', () => {
    const blockTask = {
      name: 'Block only',
      module: 'block',
      children: [
        { name: 'Main', module: 'shell', args: '{}', blockSection: 'BLOCK', taskOrder: 1 },
      ],
    };
    const yaml = blockToYaml(blockTask);
    expect(yaml).not.toContain('rescue');
    expect(yaml).not.toContain('always');
  });
});
