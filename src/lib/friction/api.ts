/**
 * Friction Timer data layer.
 *
 * Everything that touches persistence goes through this module. Components
 * never read `localStorage` (or any future API) directly — they call these
 * async functions. To swap in a real backend later, replace the bodies; the
 * signatures and event semantics stay the same.
 */

import type {
  FrictionApp,
  FrictionAppDraft,
  InstalledApp,
  ServiceStatus,
} from "./types";

const APPS_KEY = "friction-timer:apps";
const STATUS_KEY = "friction-timer:status";
const CHANGE_EVENT = "friction-store-change";

// Simulated network latency so loading states render in dev.
const LATENCY_MS = { read: 350, write: 200, search: 250 } as const;

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function readJSON<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function writeJSON<T>(key: string, value: T): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
    window.dispatchEvent(new CustomEvent(CHANGE_EVENT, { detail: { key } }));
  } catch {
    /* ignore quota / private-mode errors */
  }
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
  await delay(LATENCY_MS.read);
  return readJSON<FrictionApp[]>(APPS_KEY, []);
}

export async function getApp(id: string): Promise<FrictionApp | null> {
  await delay(LATENCY_MS.read);
  return readJSON<FrictionApp[]>(APPS_KEY, []).find((a) => a.id === id) ?? null;
}

export async function createApp(draft: FrictionAppDraft): Promise<FrictionApp> {
  await delay(LATENCY_MS.write);
  const app: FrictionApp = { id: crypto.randomUUID(), ...draft };
  const current = readJSON<FrictionApp[]>(APPS_KEY, []);
  writeJSON(APPS_KEY, [...current, app]);
  return app;
}

export async function updateApp(
  id: string,
  patch: Partial<FrictionAppDraft>,
): Promise<FrictionApp> {
  await delay(LATENCY_MS.write);
  const current = readJSON<FrictionApp[]>(APPS_KEY, []);
  const idx = current.findIndex((a) => a.id === id);
  if (idx < 0) throw new Error(`App not found: ${id}`);
  const next = { ...current[idx], ...patch };
  const out = [...current];
  out[idx] = next;
  writeJSON(APPS_KEY, out);
  return next;
}

export async function deleteApp(id: string): Promise<void> {
  await delay(LATENCY_MS.write);
  const current = readJSON<FrictionApp[]>(APPS_KEY, []);
  writeJSON(
    APPS_KEY,
    current.filter((a) => a.id !== id),
  );
}

// ---------- Service status (overlay / accessibility permissions) ----------

const DEFAULT_STATUS: ServiceStatus = { overlay: false, accessibility: false };

export async function getStatus(): Promise<ServiceStatus> {
  await delay(LATENCY_MS.read);
  return readJSON<ServiceStatus>(STATUS_KEY, DEFAULT_STATUS);
}

export async function setStatus(patch: Partial<ServiceStatus>): Promise<ServiceStatus> {
  await delay(LATENCY_MS.write);
  const current = readJSON<ServiceStatus>(STATUS_KEY, DEFAULT_STATUS);
  const next = { ...current, ...patch };
  writeJSON(STATUS_KEY, next);
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
