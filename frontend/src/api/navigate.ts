import type { NavigateFunction } from "react-router-dom";

let navigate: NavigateFunction | null = null;

export function setNavigate(n: NavigateFunction) {
  navigate = n;
}

export function getNavigate(): NavigateFunction | null {
  return navigate;
}
