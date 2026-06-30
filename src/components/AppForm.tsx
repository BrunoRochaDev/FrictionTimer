import { useState } from "react"
import { useNavigate } from "@tanstack/react-router"
import { useApps, useAppMutations, type FrictionApp, type FrictionAppDraft } from "@/lib/friction"
import { StageApp, StageMessages, StageTime } from "./app-form/stages"
import {
  DEFAULT_DRAFT,
  draftFromApp,
  sanitizeDraft,
  type Stage,
} from "./app-form/utils"

export function AppForm({ initial }: { initial?: FrictionApp }) {
  const navigate = useNavigate()
  const { data: existingApps } = useApps()
  const mutations = useAppMutations()
  const isEdit = !!initial

  const [stage, setStage] = useState<Stage>(0)
  const [draft, setDraft] = useState<FrictionAppDraft>(() =>
    initial ? draftFromApp(initial) : DEFAULT_DRAFT,
  )
  const coveredAppIds = new Set(
    existingApps
      .filter((app) => app.id !== initial?.id)
      .map((app) => app.appId),
  )
  const appAlreadyCovered = draft.appId !== initial?.appId && coveredAppIds.has(draft.appId)

  async function save() {
    if (!draft.appId || appAlreadyCovered || mutations.saving) return
    const clean = sanitizeDraft(draft)

    try {
      if (initial) await mutations.update(initial.id, clean)
      else await mutations.create(clean)
      navigate({ to: "/" })
    } catch {
      /* error surfaced via mutations.error */
    }
  }

  async function handleDelete() {
    if (!initial || mutations.saving) return

    try {
      await mutations.remove(initial.id)
      navigate({ to: "/" })
    } catch {
      /* error surfaced via mutations.error */
    }
  }

  return (
    <div className="space-y-6">
      <Stepper stage={stage} />

      {stage === 0 && (
        <StageApp
          draft={draft}
          setDraft={setDraft}
          isEdit={isEdit}
          coveredAppIds={coveredAppIds}
          currentAppId={initial?.appId}
        />
      )}
      {stage === 1 && <StageTime draft={draft} setDraft={setDraft} />}
      {stage === 2 && <StageMessages draft={draft} setDraft={setDraft} />}

      {appAlreadyCovered && (
        <p className="rounded-md border border-border bg-secondary/40 px-3 py-2 text-xs text-muted-foreground">
          That app is already covered by another saved entry. Choose a different app to continue.
        </p>
      )}

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
              disabled={!draft.appId || appAlreadyCovered}
              onClick={() => setStage((stage + 1) as Stage)}
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next →
            </button>
          ) : (
            <button
              type="button"
              onClick={save}
              disabled={mutations.saving || appAlreadyCovered}
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
            >
              {mutations.saving ? "Saving…" : isEdit ? "Save" : "Create"}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

function Stepper({ stage }: { stage: Stage }) {
  const items = ["App", "Timing", "Messages"]

  return (
    <ol className="grid grid-cols-3 gap-2 text-[11px] uppercase tracking-widest">
      {items.map((label, i) => {
        const active = i === stage
        return (
          <li key={label} className="min-w-0">
            <span
              className={`flex w-full items-center justify-center gap-2 rounded-md border px-3 py-2 text-center ${
                active
                  ? "border-primary bg-primary/10 text-foreground"
                  : "border-border text-muted-foreground"
              }`}
            >
              <span
                className={`inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full border text-[10px] font-bold ${
                  active
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border text-muted-foreground"
                }`}
              >
                {i + 1}
              </span>
              <span className="truncate">{label}</span>
            </span>
          </li>
        )
      })}
    </ol>
  )
}
