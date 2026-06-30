/**
 * Friction Timer data layer.
 *
 * Everything that touches persistence goes through this module.
 */

import { invoke } from "@tauri-apps/api/core";
import type {
  FrictionApp,
  FrictionAppDraft,
  InstalledApp,
  ServicePermissionKey,
  ServiceStatus,
} from "./types";

const CHANGE_EVENT = "friction-store-change";

function emitChange(key: string): void {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent(CHANGE_EVENT, { detail: { key } }));
}

/** Subscribe to any change in the friction data layer. Returns an unsubscribe. */
export function subscribe(listener: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  const onCustom = () => listener();
  const onStorage = () => listener();
  window.addEventListener(CHANGE_EVENT, onCustom);
  window.addEventListener("storage", onStorage);
  return () => {
    window.removeEventListener(CHANGE_EVENT, onCustom);
    window.removeEventListener("storage", onStorage);
  };
}

// ---------- Friction apps ----------

export async function listApps(): Promise<FrictionApp[]> {
  return invoke<FrictionApp[]>("list_apps");
}

export async function getApp(id: string): Promise<FrictionApp | null> {
  return invoke<FrictionApp | null>("get_app", { id });
}

export async function createApp(draft: FrictionAppDraft): Promise<FrictionApp> {
  const app = await invoke<FrictionApp>("create_app", { input: draft });
  emitChange("apps");
  return app;
}

export async function updateApp(
  id: string,
  patch: Partial<FrictionAppDraft>,
): Promise<FrictionApp> {
  const app = await invoke<FrictionApp>("update_app", { id, patch });
  emitChange("apps");
  return app;
}

export async function deleteApp(id: string): Promise<void> {
  await invoke("delete_app", { id });
  emitChange("apps");
}

// ---------- Service status (overlay / accessibility permissions) ----------

export async function getServiceStatus(): Promise<ServiceStatus> {
  return invoke<ServiceStatus>("get_service_status");
}

export async function openServiceSettings(kind: ServicePermissionKey): Promise<void> {
  await invoke("open_service_settings", { kind });
}

// ---------- Installed apps ----------

export async function listInstalledApps(query = ""): Promise<InstalledApp[]> {
  return invoke<InstalledApp[]>("list_installed_apps", { query });
}

// ---------- Formatting helpers ----------

export function formatDuration(totalSeconds: number): string {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  const parts: string[] = [];
  if (h) parts.push(`${h}h`);
  if (m) parts.push(`${m}m`);
  if (s || parts.length === 0) parts.push(`${s}s`);
  return parts.join(" ");
}
