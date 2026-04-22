import { describe, it, expect } from 'vitest';
import { resolveVariablePriority } from '../variablePriority';

const empty = {
  projectVars: [],
  hostgroupVars: [],
  environmentVars: [],
  roleVars: [],
  roleDefaults: [],
};

describe('resolveVariablePriority', () => {
  describe('no duplicates', () => {
    it('returns empty duplicate set when input is empty', () => {
      const result = resolveVariablePriority(empty);
      expect(result.duplicateKeys.size).toBe(0);
      expect(result.winningScope.size).toBe(0);
    });

    it('assigns winner for unique keys but flags no duplicates', () => {
      const result = resolveVariablePriority({
        ...empty,
        projectVars: [{ key: 'port' }],
        hostgroupVars: [{ key: 'host_name' }],
      });
      expect(result.duplicateKeys.size).toBe(0);
      expect(result.winningScope.get('port')).toBe('PROJECT');
      expect(result.winningScope.get('host_name')).toBe('HOSTGROUP');
    });
  });

  describe('priority order: ENVIRONMENT > HOSTGROUP > PROJECT > ROLE_VARS > ROLE_DEFAULTS', () => {
    it('ENVIRONMENT wins over all other scopes', () => {
      const result = resolveVariablePriority({
        projectVars: [{ key: 'k' }],
        hostgroupVars: [{ key: 'k' }],
        environmentVars: [{ key: 'k' }],
        roleVars: [{ key: 'k' }],
        roleDefaults: [{ key: 'k' }],
      });
      expect(result.duplicateKeys.has('k')).toBe(true);
      expect(result.winningScope.get('k')).toBe('ENVIRONMENT');
    });

    it('HOSTGROUP wins over PROJECT/ROLE_VARS/ROLE_DEFAULTS when ENV absent', () => {
      const result = resolveVariablePriority({
        ...empty,
        projectVars: [{ key: 'k' }],
        hostgroupVars: [{ key: 'k' }],
        roleVars: [{ key: 'k' }],
        roleDefaults: [{ key: 'k' }],
      });
      expect(result.winningScope.get('k')).toBe('HOSTGROUP');
    });

    it('PROJECT wins over ROLE_VARS/ROLE_DEFAULTS', () => {
      const result = resolveVariablePriority({
        ...empty,
        projectVars: [{ key: 'k' }],
        roleVars: [{ key: 'k' }],
        roleDefaults: [{ key: 'k' }],
      });
      expect(result.winningScope.get('k')).toBe('PROJECT');
    });

    it('ROLE_VARS wins over ROLE_DEFAULTS', () => {
      const result = resolveVariablePriority({
        ...empty,
        roleVars: [{ key: 'k' }],
        roleDefaults: [{ key: 'k' }],
      });
      expect(result.duplicateKeys.has('k')).toBe(true);
      expect(result.winningScope.get('k')).toBe('ROLE_VARS');
    });

    it('ROLE_DEFAULTS alone wins without duplicate flag', () => {
      const result = resolveVariablePriority({
        ...empty,
        roleDefaults: [{ key: 'k' }],
      });
      expect(result.duplicateKeys.has('k')).toBe(false);
      expect(result.winningScope.get('k')).toBe('ROLE_DEFAULTS');
    });
  });

  describe('duplicate detection', () => {
    it('flags key appearing in any two scopes as duplicate', () => {
      const result = resolveVariablePriority({
        ...empty,
        projectVars: [{ key: 'port' }],
        environmentVars: [{ key: 'port' }],
      });
      expect(result.duplicateKeys.has('port')).toBe(true);
      expect(result.winningScope.get('port')).toBe('ENVIRONMENT');
    });

    it('flags key appearing twice within the same scope (e.g. two hostgroups sharing a key)', () => {
      const result = resolveVariablePriority({
        ...empty,
        hostgroupVars: [{ key: 'app_port' }, { key: 'app_port' }],
      });
      expect(result.duplicateKeys.has('app_port')).toBe(true);
      expect(result.winningScope.get('app_port')).toBe('HOSTGROUP');
    });

    it('does not cross-flag unrelated keys', () => {
      const result = resolveVariablePriority({
        ...empty,
        projectVars: [{ key: 'a' }, { key: 'b' }],
        environmentVars: [{ key: 'b' }, { key: 'c' }],
      });
      expect(result.duplicateKeys.has('a')).toBe(false);
      expect(result.duplicateKeys.has('b')).toBe(true);
      expect(result.duplicateKeys.has('c')).toBe(false);
      expect(result.winningScope.get('a')).toBe('PROJECT');
      expect(result.winningScope.get('b')).toBe('ENVIRONMENT');
      expect(result.winningScope.get('c')).toBe('ENVIRONMENT');
    });
  });

  describe('mixed scenarios', () => {
    it('resolves several keys independently with different winners', () => {
      const result = resolveVariablePriority({
        projectVars: [{ key: 'shared' }, { key: 'only_project' }],
        hostgroupVars: [{ key: 'shared' }],
        environmentVars: [{ key: 'env_only' }],
        roleVars: [{ key: 'shared' }, { key: 'role_only' }],
        roleDefaults: [{ key: 'role_only' }],
      });
      expect(result.winningScope.get('shared')).toBe('HOSTGROUP');
      expect(result.winningScope.get('only_project')).toBe('PROJECT');
      expect(result.winningScope.get('env_only')).toBe('ENVIRONMENT');
      expect(result.winningScope.get('role_only')).toBe('ROLE_VARS');
      expect(result.duplicateKeys.has('shared')).toBe(true);
      expect(result.duplicateKeys.has('role_only')).toBe(true);
      expect(result.duplicateKeys.has('only_project')).toBe(false);
      expect(result.duplicateKeys.has('env_only')).toBe(false);
    });
  });
});
