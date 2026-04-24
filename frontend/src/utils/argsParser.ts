import { getModuleDefinition } from "../constants/ansibleModules";

export interface ExtraParam {
  key: string;
  value: string;
}

export interface ParsedArgs {
  moduleParams: Record<string, unknown>;
  extraParams: ExtraParam[];
}

/** Merge moduleParams + extraParams into a JSON string for the args field */
export function buildArgsJson(
  moduleParams: Record<string, unknown> | undefined,
  extraParams: ExtraParam[] | undefined
): string {
  const result: Record<string, unknown> = {};
  if (moduleParams) {
    for (const [k, v] of Object.entries(moduleParams)) {
      if (v !== undefined && v !== "" && v !== null) {
        result[k] = v;
      }
    }
  }
  if (extraParams) {
    for (const item of extraParams) {
      if (item.key) {
        result[item.key] = item.value;
      }
    }
  }
  return Object.keys(result).length > 0 ? JSON.stringify(result) : "";
}

/** Parse args JSON string into moduleParams + extraParams for form population */
export function parseArgsToForm(
  argsJson: string | undefined,
  moduleName: string | undefined
): ParsedArgs {
  const moduleParams: Record<string, unknown> = {};
  const extraParams: ExtraParam[] = [];
  if (!argsJson) return { moduleParams, extraParams };

  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(argsJson);
  } catch {
    extraParams.push({ key: "", value: argsJson });
    return { moduleParams, extraParams };
  }

  const moduleDef = moduleName ? getModuleDefinition(moduleName) : undefined;
  const knownParams = new Set(moduleDef?.params.map((p) => p.name) ?? []);

  for (const [k, v] of Object.entries(parsed)) {
    if (knownParams.has(k)) {
      moduleParams[k] = v;
    } else {
      extraParams.push({ key: k, value: String(v) });
    }
  }
  return { moduleParams, extraParams };
}
