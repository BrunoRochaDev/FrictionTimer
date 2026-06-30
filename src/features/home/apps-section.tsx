import { Link } from "@tanstack/react-router";
import { AppCardSkeleton } from "@/components/Skeletons";
import { formatDuration, type FrictionApp } from "@/lib/friction";

type AppsSectionProps = {
  loading: boolean;
  error: Error | null;
  data: FrictionApp[];
};

export function AppsSection({ loading, error, data }: AppsSectionProps) {
  return (
    <section className="space-y-3">
      <div className="flex items-baseline justify-between">
        <h2 className="text-xs uppercase tracking-widest text-muted-foreground">
          Limited apps
        </h2>
        <span className="text-xs text-muted-foreground tabular-nums">
          {loading ? "…" : data.length}
        </span>
      </div>

      {error && (
        <p className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          Couldn't load apps: {error.message}
        </p>
      )}

      <ul className="space-y-3">
        {loading ? (
          <>
            <li><AppCardSkeleton /></li>
            <li><AppCardSkeleton /></li>
          </>
        ) : (
          data.map((app) => (
            <li key={app.id}>
              <AppCard app={app} />
            </li>
          ))
        )}

        <li>
          <Link
            to="/app/new"
            className="flex w-full items-center justify-center rounded-lg border-2 border-dashed border-border bg-transparent px-4 py-5 text-sm font-medium text-muted-foreground transition-colors hover:border-primary hover:text-primary"
          >
            + Add application
          </Link>
        </li>
      </ul>
    </section>
  );
}

function AppCard({ app }: { app: FrictionApp }) {
  return (
    <Link
      to="/app/$id"
      params={{ id: app.id }}
      className="group flex items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3 transition-colors hover:border-primary"
    >
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-semibold text-foreground">
          {app.name}
        </div>
        <div className="truncate text-[11px] text-muted-foreground">
          {app.appId}
        </div>
      </div>
      <div className="shrink-0 flex flex-col items-end gap-1 text-[11px]">
        <div className="flex items-center gap-1.5">
          <span className="text-muted-foreground">wait</span>
          <span className="font-mono text-foreground">
            {formatDuration(app.waitSeconds)}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="text-muted-foreground">open</span>
          <span className="font-mono text-foreground">
            {formatDuration(app.durationSeconds)}
          </span>
        </div>
      </div>
    </Link>
  );
}
