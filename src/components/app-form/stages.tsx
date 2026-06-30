import { useState, type Dispatch, type SetStateAction } from "react"
import { InstalledAppRowSkeleton } from "@/components/Skeletons"
import { useInstalledApps, type FrictionAppDraft } from "@/lib/friction"
import { hmsToSeconds, secondsToHMS } from "./utils"

type DraftSetter = Dispatch<SetStateAction<FrictionAppDraft>>

export function StageApp({
  draft,
  setDraft,
  isEdit,
  coveredAppIds,
  currentAppId,
}: {
  draft: FrictionAppDraft
  setDraft: DraftSetter
  isEdit: boolean
  coveredAppIds: Set<string>
  currentAppId?: string
}) {
  const [query, setQuery] = useState("")
  const { data: apps, loading, pending, error } = useInstalledApps(query)

  return (
    <div className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold">Choose an application</h3>
        <p className="text-xs text-muted-foreground">
          {isEdit ? "Change which installed app to limit." : "Pick from apps installed on the device."}
        </p>
      </div>
      <input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search apps..."
        className="w-full rounded-md border border-border bg-card px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:border-primary"
      />
      <ul className="h-80 divide-y divide-border overflow-auto rounded-md border border-border">
        {error && (
          <li className="px-3 py-4 text-center text-xs text-destructive">{error.message}</li>
        )}
        {loading || pending ? (
          <>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
          </>
        ) : apps.length === 0 ? (
          <li className="px-3 py-4 text-center text-xs text-muted-foreground">No matches.</li>
        ) : (
          apps.map((app) => {
            const selected = draft.appId === app.appId
            const covered = app.appId !== currentAppId && coveredAppIds.has(app.appId)

            return (
              <li key={app.appId}>
                <button
                  type="button"
                  disabled={covered}
                  onClick={() =>
                    setDraft((current) => ({ ...current, appId: app.appId, name: app.name }))
                  }
                  className={`flex w-full items-center justify-between px-3 py-2.5 text-left transition-colors disabled:cursor-not-allowed ${
                    covered
                      ? "bg-secondary/40 text-muted-foreground opacity-60"
                      : selected
                        ? "bg-primary/10"
                        : "bg-card hover:bg-secondary"
                  }`}
                >
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium">{app.name}</div>
                    <div className="truncate text-[11px] text-muted-foreground">
                      {app.appId}
                    </div>
                  </div>
                  <div className="ml-2 flex shrink-0 items-center gap-2">
                    {covered && (
                      <span className="rounded-full border border-border px-2 py-0.5 text-[10px] uppercase tracking-wide">
                        Added
                      </span>
                    )}
                    <span
                      className={`h-2.5 w-2.5 rounded-full ${
                        covered ? "bg-muted-foreground/50" : selected ? "bg-primary" : "bg-border"
                      }`}
                    />
                  </div>
                </button>
              </li>
            )
          })
        )}
      </ul>
    </div>
  )
}

export function StageTime({
  draft,
  setDraft,
}: {
  draft: FrictionAppDraft
  setDraft: DraftSetter
}) {
  const wait = secondsToHMS(draft.waitSeconds)
  const duration = secondsToHMS(draft.durationSeconds)

  return (
    <div className="space-y-5">
      <div>
        <h3 className="text-sm font-semibold">Set timing</h3>
        <p className="text-xs text-muted-foreground">Minimum one second on each.</p>
      </div>

      <TimeBlock
        label="Wait time"
        hint="How long to wait before the app unlocks."
        h={wait.h}
        m={wait.m}
        s={wait.s}
        onChange={(h, m, s) =>
          setDraft((current) => ({
            ...current,
            waitSeconds: Math.max(1, hmsToSeconds(h, m, s)),
          }))
        }
      />
      <TimeBlock
        label="Duration"
        hint="How long the app stays unlocked once allowed in."
        h={duration.h}
        m={duration.m}
        s={duration.s}
        onChange={(h, m, s) =>
          setDraft((current) => ({
            ...current,
            durationSeconds: Math.max(1, hmsToSeconds(h, m, s)),
          }))
        }
      />
    </div>
  )
}

function TimeBlock({
  label,
  hint,
  h,
  m,
  s,
  onChange,
}: {
  label: string
  hint: string
  h: number
  m: number
  s: number
  onChange: (h: number, m: number, s: number) => void
}) {
  return (
    <div className="space-y-3 rounded-lg border border-border bg-card p-4">
      <div>
        <div className="text-xs font-bold uppercase tracking-widest text-foreground">
          {label}
        </div>
        <div className="text-[11px] text-muted-foreground">{hint}</div>
      </div>
      <div className="grid grid-cols-3 gap-2">
        <NumberField label="hours" value={h} max={23} onChange={(value) => onChange(value, m, s)} />
        <NumberField label="minutes" value={m} max={59} onChange={(value) => onChange(h, value, s)} />
        <NumberField label="seconds" value={s} max={59} onChange={(value) => onChange(h, m, value)} />
      </div>
    </div>
  )
}

function NumberField({
  label,
  value,
  max,
  onChange,
}: {
  label: string
  value: number
  max: number
  onChange: (value: number) => void
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-[10px] uppercase tracking-widest text-muted-foreground">
        {label}
      </span>
      <input
        type="number"
        inputMode="numeric"
        min={0}
        max={max}
        value={value}
        onChange={(e) => {
          const nextValue = Number(e.target.value)
          if (Number.isNaN(nextValue)) return
          onChange(Math.max(0, Math.min(max, Math.floor(nextValue))))
        }}
        className="w-full rounded-md border border-border bg-background px-2 py-2 text-center font-mono text-lg tabular-nums focus:outline-none focus:border-primary"
      />
    </label>
  )
}

export function StageMessages({
  draft,
  setDraft,
}: {
  draft: FrictionAppDraft
  setDraft: DraftSetter
}) {
  return (
    <div className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold">Friction messages</h3>
        <p className="text-xs text-muted-foreground">
          Shown while the wait timer counts down. Add a few to keep things fresh.
        </p>
      </div>

      <ul className="space-y-2">
        {draft.messages.map((message, index) => (
          <li key={index} className="flex items-stretch gap-2">
            <span className="grid w-8 shrink-0 place-items-center rounded-md border border-border bg-card font-mono text-xs text-muted-foreground">
              {String(index + 1).padStart(2, "0")}
            </span>
            <input
              value={message}
              onChange={(e) => {
                const nextMessage = e.target.value
                setDraft((current) => {
                  const messages = [...current.messages]
                  messages[index] = nextMessage
                  return { ...current, messages }
                })
              }}
              placeholder="A gentle nudge..."
              className="min-w-0 flex-1 rounded-md border border-border bg-card px-3 py-2 text-sm focus:outline-none focus:border-primary"
            />
            <button
              type="button"
              onClick={() =>
                setDraft((current) => ({
                  ...current,
                  messages: current.messages.filter((_, messageIndex) => messageIndex !== index),
                }))
              }
              className="shrink-0 rounded-md border border-border bg-card px-3 text-sm text-muted-foreground hover:border-destructive hover:text-destructive"
              aria-label="Remove line"
            >
              ✕
            </button>
          </li>
        ))}
      </ul>

      <button
        type="button"
        onClick={() =>
          setDraft((current) => ({ ...current, messages: [...current.messages, ""] }))
        }
        className="w-full rounded-md border-2 border-dashed border-border px-3 py-2.5 text-sm text-muted-foreground hover:border-primary hover:text-primary"
      >
        + Add line
      </button>
    </div>
  )
}
