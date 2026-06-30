/**
 * React hooks over the friction data API.
 *
 * Each hook exposes a small async state machine:
 *   { data, loading, error, refresh, ...mutations }
 *
 * Components stay declarative and don't talk to the API directly outside of
 * these hooks. Swap the underlying API in `./api.ts` and these hooks keep
 * working unchanged.
 */

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import * as api from "./api";
import type {
  FrictionApp,
  FrictionAppDraft,
  InstalledApp,
  ServicePermissionKey,
  ServiceStatus,
} from "./types";

type AsyncState<T> = {
  data: T;
  loading: boolean;
  error: Error | null;
};

function useAsync<T>(
  fn: () => Promise<T>,
  initial: T,
  deps: ReadonlyArray<unknown>,
): AsyncState<T> & { refresh: () => void } {
  const [state, setState] = useState<AsyncState<T>>({
    data: initial,
    loading: true,
    error: null,
  });
  const reqId = useRef(0);

  const run = useCallback(() => {
    const id = ++reqId.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    fn()
      .then((data) => {
        if (reqId.current === id) setState({ data, loading: false, error: null });
      })
      .catch((err: unknown) => {
        if (reqId.current === id) {
          setState((s) => ({
            ...s,
            loading: false,
            error: err instanceof Error ? err : new Error(String(err)),
          }));
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    run();
  }, [run]);

  return { ...state, refresh: run };
}

// ---------- Apps list ----------

export function useApps() {
  const state = useAsync<FrictionApp[]>(() => api.listApps(), [], []);

  useEffect(() => api.subscribe(state.refresh), [state.refresh]);

  return state;
}

// ---------- Single app (for the edit screen) ----------

export function useApp(id: string | undefined) {
  const state = useAsync<FrictionApp | null>(
    () => (id ? api.getApp(id) : Promise.resolve(null)),
    null,
    [id],
  );

  useEffect(() => api.subscribe(state.refresh), [state.refresh]);

  return state;
}

// ---------- Mutations (no auto-refresh; events from `api` drive subscribers) ----------

export function useAppMutations() {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const wrap = useCallback(async <T,>(op: () => Promise<T>): Promise<T> => {
    setSaving(true);
    setError(null);
    try {
      return await op();
    } catch (err) {
      const e = err instanceof Error ? err : new Error(String(err));
      setError(e);
      throw e;
    } finally {
      setSaving(false);
    }
  }, []);

  const create = useCallback((draft: FrictionAppDraft) => wrap(() => api.createApp(draft)), [wrap]);
  const update = useCallback(
    (id: string, patch: Partial<FrictionAppDraft>) => wrap(() => api.updateApp(id, patch)),
    [wrap],
  );
  const remove = useCallback((id: string) => wrap(() => api.deleteApp(id)), [wrap]);

  return { saving, error, create, update, remove };
}

// ---------- Service status ----------

export function useServiceStatus() {
  const [actionError, setActionError] = useState<Error | null>(null);
  const state = useAsync<ServiceStatus>(
    () => api.getServiceStatus(),
    { overlay: false, accessibility: false },
    [],
  );

  useEffect(() => {
    if (typeof window === "undefined") return;

    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") {
        setActionError(null);
        state.refresh();
      }
    };

    const refreshOnFocus = () => {
      setActionError(null);
      state.refresh();
    };

    window.addEventListener("focus", refreshOnFocus);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      window.removeEventListener("focus", refreshOnFocus);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [state.refresh]);

  const openSettings = useCallback(
    async (key: ServicePermissionKey) => {
      setActionError(null);
      try {
        await api.openServiceSettings(key);
      } catch (err) {
        const error = err instanceof Error ? err : new Error(String(err));
        setActionError(error);
        throw error;
      }
    },
    [],
  );

  return { ...state, error: actionError ?? state.error, openSettings };
}

// ---------- Installed-apps search (mocked device inventory) ----------

export function useInstalledApps(query: string) {
  // Debounce search input slightly so the loading state isn't jittery.
  const [debounced, setDebounced] = useState(query);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 150);
    return () => clearTimeout(t);
  }, [query]);

  const state = useAsync<InstalledApp[]>(
    () => api.listInstalledApps(debounced),
    [],
    [debounced],
  );

  return useMemo(
    () => ({ ...state, query: debounced, pending: debounced !== query }),
    [state, debounced, query],
  );
}
