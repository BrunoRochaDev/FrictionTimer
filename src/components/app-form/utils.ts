import type { FrictionApp, FrictionAppDraft } from "@/lib/friction"

export type Stage = 0 | 1 | 2

export const DEFAULT_DRAFT: FrictionAppDraft = {
  appId: "",
  name: "",
  waitSeconds: 30,
  durationSeconds: 300,
  messages: ["Do you really need to open this?", "Take a breath."],
}

export function secondsToHMS(total: number) {
  return {
    h: Math.floor(total / 3600),
    m: Math.floor((total % 3600) / 60),
    s: total % 60,
  }
}

export function hmsToSeconds(h: number, m: number, s: number) {
  return Math.max(0, h * 3600 + m * 60 + s)
}

export function draftFromApp(app: FrictionApp): FrictionAppDraft {
  return {
    appId: app.appId,
    name: app.name,
    waitSeconds: app.waitSeconds,
    durationSeconds: app.durationSeconds,
    messages: [...app.messages],
  }
}

export function sanitizeDraft(draft: FrictionAppDraft): FrictionAppDraft {
  return {
    ...draft,
    waitSeconds: Math.max(1, draft.waitSeconds),
    durationSeconds: Math.max(1, draft.durationSeconds),
    messages: draft.messages.map((message) => message.trim()).filter(Boolean),
  }
}
