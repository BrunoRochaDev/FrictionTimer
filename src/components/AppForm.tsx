import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { InstalledAppRowSkeleton } from "@/components/Skeletons";
import {
  useAppMutations,
  useInstalledApps,
  type FrictionApp,
  type FrictionAppDraft,
} from "@/lib/friction";

type Stage = 0 | 1 | 2;

const DEFAULT_DRAFT: FrictionAppDraft = {
  appId: "",
  name: "",
  waitSeconds: 30,
  durationSeconds: 300,
  messages: ["Do you really need to open this?", "Take a breath."],
};

function secondsToHMS(total: number) {
  return {
    h: Math.floor(total / 3600),
    m: Math.floor((total % 3600) / 60),
    s: total % 60,
  };
}
function hmsToSeconds(h: number, m: number, s: number) {
  return Math.max(0, h * 3600 + m * 60 + s);
}

function draftFromApp(app: FrictionApp): FrictionAppDraft {
  return {
    appId: app.appId,
    name: app.name,
    waitSeconds: app.waitSeconds,
    durationSeconds: app.durationSeconds,
    messages: [...app.messages],
  };
}

export function AppForm({ initial }: { initial?: FrictionApp }) {
  const navigate = useNavigate();
  const mutations = useAppMutations();
  const isEdit = !!initial;

  const [stage, setStage] = useState<Stage>(0);
  const [draft, setDraft] = useState<FrictionAppDraft>(() =>
    initial ? draftFromApp(initial) : DEFAULT_DRAFT,
  );

  async function save() {
    if (!draft.appId || mutations.saving) return;
    const clean: FrictionAppDraft = {
      ...draft,
      waitSeconds: Math.max(1, draft.waitSeconds),
      durationSeconds: Math.max(1, draft.durationSeconds),
      messages: draft.messages.map((m) => m.trim()).filter(Boolean),
    };
    try {
      if (initial) await mutations.update(initial.id, clean);
      else await mutations.create(clean);
      navigate({ to: "/" });
    } catch {
      /* error surfaced via mutations.error */
    }
  }

  async function handleDelete() {
    if (!initial || mutations.saving) return;
    try {
      await mutations.remove(initial.id);
      navigate({ to: "/" });
    } catch {
      /* error surfaced via mutations.error */
    }
  }

  return (
    <div className="space-y-6">
      <Stepper stage={stage} />

      {stage === 0 && (
        <StageApp draft={draft} setDraft={setDraft} isEdit={isEdit} />
      )}
      {stage === 1 && <StageTime draft={draft} setDraft={setDraft} />}
      {stage === 2 && <StageMessages draft={draft} setDraft={setDraft} />}

      {mutations.error && (
        <p className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          {mutations.error.message}
        </p>
      )}

      <div className="flex items-center justify-between gap-2 pt-2">
        <button
          type="button"
          onClick={() => (stage === 0 ? navigate({ to: "/" }) : setStage((stage - 1) as Stage))}
          disabled={mutations.saving}
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:border-primary disabled:opacity-50"
        >
          {stage === 0 ? "Cancel" : "Back"}
        </button>

        <div className="flex items-center gap-2">
          {isEdit && stage === 0 && (
            <button
              type="button"
              onClick={handleDelete}
              disabled={mutations.saving}
              className="rounded-md border border-destructive/40 bg-destructive/10 px-4 py-2 text-sm text-destructive hover:bg-destructive/20 disabled:opacity-50"
            >
              Delete
            </button>
          )}
          {stage < 2 ? (
            <button
              type="button"
              disabled={stage === 0 && !draft.appId}
              onClick={() => setStage((stage + 1) as Stage)}
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next →
            </button>
          ) : (
            <button
              type="button"
              onClick={save}
              disabled={mutations.saving}
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
            >
              {mutations.saving ? "Saving…" : isEdit ? "Save" : "Create"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function Stepper({ stage }: { stage: Stage }) {
  const items = ["App", "Timing", "Messages"];
  return (
    <ol className="flex items-center gap-2 text-[11px] uppercase tracking-widest">
      {items.map((label, i) => {
        const active = i === stage;
        const done = i < stage;
        return (
          <li key={label} className="flex items-center gap-2">
            <span
              className={`inline-flex h-5 w-5 items-center justify-center rounded-full border text-[10px] font-bold ${
                active
                  ? "border-primary bg-primary text-primary-foreground"
                  : done
                    ? "border-success bg-success text-primary-foreground"
                    : "border-border text-muted-foreground"
              }`}
            >
              {i + 1}
            </span>
            <span className={active ? "text-foreground" : "text-muted-foreground"}>
              {label}
            </span>
            {i < items.length - 1 && <span className="text-border">/</span>}
          </li>
        );
      })}
    </ol>
  );
}

function StageApp({
  draft,
  setDraft,
  isEdit,
}: {
  draft: FrictionAppDraft;
  setDraft: (d: FrictionAppDraft) => void;
  isEdit: boolean;
}) {
  const [query, setQuery] = useState("");
  const { data: apps, loading, pending, error } = useInstalledApps(query);

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
      <ul className="h-80 overflow-auto rounded-md border border-border divide-y divide-border">
        {error && (
          <li className="px-3 py-4 text-center text-xs text-destructive">
            {error.message}
          </li>
        )}
        {(loading || pending) ? (
          <>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
            <li><InstalledAppRowSkeleton /></li>
          </>
        ) : apps.length === 0 ? (
          <li className="px-3 py-4 text-center text-xs text-muted-foreground">
            No matches.
          </li>
        ) : (
          apps.map((app) => {
            const selected = draft.appId === app.appId;
            return (
              <li key={app.appId}>
                <button
                  type="button"
                  onClick={() => setDraft({ ...draft, appId: app.appId, name: app.name })}
                  className={`flex w-full items-center justify-between px-3 py-2.5 text-left transition-colors ${
                    selected ? "bg-primary/10" : "bg-card hover:bg-secondary"
                  }`}
                >
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium">{app.name}</div>
                    <div className="truncate text-[11px] text-muted-foreground">
                      {app.appId}
                    </div>
                  </div>
                  <span
                    className={`ml-2 h-2.5 w-2.5 shrink-0 rounded-full ${
                      selected ? "bg-primary" : "bg-border"
                    }`}
                  />
                </button>
              </li>
            );
          })
        )}
      </ul>
    </div>
  );
}

function StageTime({
  draft,
  setDraft,
}: {
  draft: FrictionAppDraft;
  setDraft: (d: FrictionAppDraft) => void;
}) {
  const wait = secondsToHMS(draft.waitSeconds);
  const dur = secondsToHMS(draft.durationSeconds);
  return (
    <div className="space-y-5">
      <div>
        <h3 className="text-sm font-semibold">Set timing</h3>
        <p className="text-xs text-muted-foreground">Minimum one second on each.</p>
      </div>

      <TimeBlock
        label="Wait time"
        hint="How long to wait before the app unlocks."
        accent="warning"
        h={wait.h}
        m={wait.m}
        s={wait.s}
        onChange={(h, m, s) =>
          setDraft({ ...draft, waitSeconds: Math.max(1, hmsToSeconds(h, m, s)) })
        }
      />
      <TimeBlock
        label="Duration"
        hint="How long the app stays unlocked once allowed in."
        accent="success"
        h={dur.h}
        m={dur.m}
        s={dur.s}
        onChange={(h, m, s) =>
          setDraft({ ...draft, durationSeconds: Math.max(1, hmsToSeconds(h, m, s)) })
        }
      />
    </div>
  );
}

function TimeBlock({
  label,
  hint,
  accent,
  h,
  m,
  s,
  onChange,
}: {
  label: string;
  hint: string;
  accent: "warning" | "success";
  h: number;
  m: number;
  s: number;
  onChange: (h: number, m: number, s: number) => void;
}) {
  const color = accent === "warning" ? "text-warning" : "text-success";
  return (
    <div className="rounded-lg border border-border bg-card p-4 space-y-3">
      <div>
        <div className={`text-xs font-bold uppercase tracking-widest ${color}`}>
          {label}
        </div>
        <div className="text-[11px] text-muted-foreground">{hint}</div>
      </div>
      <div className="grid grid-cols-3 gap-2">
        <NumberField label="hours" value={h} max={23} onChange={(v) => onChange(v, m, s)} />
        <NumberField label="minutes" value={m} max={59} onChange={(v) => onChange(h, v, s)} />
        <NumberField label="seconds" value={s} max={59} onChange={(v) => onChange(h, m, v)} />
      </div>
    </div>
  );
}

function NumberField({
  label,
  value,
  max,
  onChange,
}: {
  label: string;
  value: number;
  max: number;
  onChange: (v: number) => void;
}) {
  return (
    <label className="block">
      <span className="block text-[10px] uppercase tracking-widest text-muted-foreground mb-1">
        {label}
      </span>
      <input
        type="number"
        inputMode="numeric"
        min={0}
        max={max}
        value={value}
        onChange={(e) => {
          const n = Number(e.target.value);
          if (Number.isNaN(n)) return;
          onChange(Math.max(0, Math.min(max, Math.floor(n))));
        }}
        className="w-full rounded-md border border-border bg-background px-2 py-2 text-center font-mono text-lg tabular-nums focus:outline-none focus:border-primary"
      />
    </label>
  );
}

function StageMessages({
  draft,
  setDraft,
}: {
  draft: FrictionAppDraft;
  setDraft: (d: FrictionAppDraft) => void;
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
        {draft.messages.map((msg, i) => (
          <li key={i} className="flex items-stretch gap-2">
            <span className="grid w-8 shrink-0 place-items-center rounded-md border border-border bg-card text-xs text-muted-foreground font-mono">
              {String(i + 1).padStart(2, "0")}
            </span>
            <input
              value={msg}
              onChange={(e) => {
                const next = [...draft.messages];
                next[i] = e.target.value;
                setDraft({ ...draft, messages: next });
              }}
              placeholder="A gentle nudge..."
              className="flex-1 min-w-0 rounded-md border border-border bg-card px-3 py-2 text-sm focus:outline-none focus:border-primary"
            />
            <button
              type="button"
              onClick={() =>
                setDraft({ ...draft, messages: draft.messages.filter((_, j) => j !== i) })
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
        onClick={() => setDraft({ ...draft, messages: [...draft.messages, ""] })}
        className="w-full rounded-md border-2 border-dashed border-border px-3 py-2.5 text-sm text-muted-foreground hover:border-primary hover:text-primary"
      >
        + Add line
      </button>
    </div>
  );
}
