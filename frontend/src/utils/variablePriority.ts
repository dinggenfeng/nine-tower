export type VariableScopeKind =
  | "PROJECT"
  | "HOSTGROUP"
  | "ENVIRONMENT"
  | "ROLE_VARS"
  | "ROLE_DEFAULTS";

const PRIORITY_RANK: Record<VariableScopeKind, number> = {
  ENVIRONMENT: 5,
  HOSTGROUP: 4,
  PROJECT: 3,
  ROLE_VARS: 2,
  ROLE_DEFAULTS: 1,
};

export interface KeyedVariable {
  key: string;
}

export interface VariablePriorityInput {
  projectVars: KeyedVariable[];
  hostgroupVars: KeyedVariable[];
  environmentVars: KeyedVariable[];
  roleVars: KeyedVariable[];
  roleDefaults: KeyedVariable[];
}

export interface VariablePriorityResult {
  duplicateKeys: Set<string>;
  winningScope: Map<string, VariableScopeKind>;
}

export function resolveVariablePriority(input: VariablePriorityInput): VariablePriorityResult {
  const keyScopes = new Map<string, VariableScopeKind[]>();
  const track = (vars: KeyedVariable[], scope: VariableScopeKind) => {
    vars.forEach((v) => {
      const entries = keyScopes.get(v.key) ?? [];
      entries.push(scope);
      keyScopes.set(v.key, entries);
    });
  };
  track(input.projectVars, "PROJECT");
  track(input.hostgroupVars, "HOSTGROUP");
  track(input.environmentVars, "ENVIRONMENT");
  track(input.roleVars, "ROLE_VARS");
  track(input.roleDefaults, "ROLE_DEFAULTS");

  const duplicateKeys = new Set<string>();
  const winningScope = new Map<string, VariableScopeKind>();
  keyScopes.forEach((scopes, key) => {
    if (scopes.length > 1) duplicateKeys.add(key);
    const winner = scopes.reduce((best, s) => (PRIORITY_RANK[s] > PRIORITY_RANK[best] ? s : best));
    winningScope.set(key, winner);
  });

  return { duplicateKeys, winningScope };
}
