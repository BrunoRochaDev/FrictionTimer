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
  ServiceStatus,
} from "./types";

const STATUS_KEY = "friction-timer:status";
const CHANGE_EVENT = "friction-store-change";

// Simulated latency for mocks so loading states still render in dev.
const LATENCY_MS = { read: 350, write: 200, search: 250 } as const;

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function readLocalJSON<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function writeLocalJSON<T>(key: string, value: T): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* ignore quota / private-mode errors */
  }
}

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

const DEFAULT_STATUS: ServiceStatus = { overlay: false, accessibility: false };

export async function getStatus(): Promise<ServiceStatus> {
  await delay(LATENCY_MS.read);
  return readLocalJSON<ServiceStatus>(STATUS_KEY, DEFAULT_STATUS);
}

export async function setStatus(patch: Partial<ServiceStatus>): Promise<ServiceStatus> {
  await delay(LATENCY_MS.write);
  const current = readLocalJSON<ServiceStatus>(STATUS_KEY, DEFAULT_STATUS);
  const next = { ...current, ...patch };
  writeLocalJSON(STATUS_KEY, next);
  emitChange(STATUS_KEY);
  return next;
}

// ---------- Installed apps (mocked device inventory) ----------

const MOCK_INSTALLED: InstalledApp[] = [
  { appId: "com.instagram.android", name: "Instagram" },
  { appId: "com.zhiliaoapp.musically", name: "TikTok" },
  { appId: "com.twitter.android", name: "X" },
  { appId: "com.reddit.frontpage", name: "Reddit" },
  { appId: "com.google.android.youtube", name: "YouTube" },
  { appId: "com.facebook.katana", name: "Facebook" },
  { appId: "com.snapchat.android", name: "Snapchat" },
  { appId: "com.netflix.mediaclient", name: "Netflix" },
  { appId: "com.discord", name: "Discord" },
  { appId: "com.whatsapp", name: "WhatsApp" },
  { appId: "com.spotify.music", name: "Spotify" },
  { appId: "com.amazon.mShop.android.shopping", name: "Amazon" },
];

export async function listInstalledApps(query = ""): Promise<InstalledApp[]> {
  await delay(LATENCY_MS.search);
  const q = query.trim().toLowerCase();
  if (!q) return MOCK_INSTALLED;
  return MOCK_INSTALLED.filter(
    (a) => a.name.toLowerCase().includes(q) || a.appId.toLowerCase().includes(q),
  );
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
