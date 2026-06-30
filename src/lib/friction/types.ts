// Domain types — keep these stable; swapping the backing driver shouldn't change them.

export type FrictionApp = {
  id: string;
  appId: string; // e.g. com.instagram.android
  name: string; // human readable
  waitSeconds: number; // wait before unlock
  durationSeconds: number; // how long it stays unlocked
  messages: string[]; // lines shown during wait
};

export type ServiceStatus = {
  overlay: boolean;
  accessibility: boolean;
};

export type ServicePermissionKey = keyof ServiceStatus;

export type InstalledApp = {
  appId: string;
  name: string;
};

export type FrictionAppDraft = Omit<FrictionApp, "id">;
