import { createFileRoute, Link } from "@tanstack/react-router";
import { AppHeader } from "@/components/AppHeader";
import {
  AppCardSkeleton,
  ServiceButtonSkeleton,
} from "@/components/Skeletons";
import {
  formatDuration,
  useApps,
  useServiceStatus,
  type FrictionApp,
  type ServiceStatus,
} from "@/lib/friction";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Friction Timer" },
      { name: "description", content: "Add intentional friction before opening distracting apps." },
      { property: "og:title", content: "Friction Timer" },
      { property: "og:description", content: "Add intentional friction before opening distracting apps." },
    ],
  }),
  component: Home,
});

function Home() {
  const apps = useApps();
  const services = useServiceStatus();

  return (
    <div className="min-h-screen flex flex-col">
      <AppHeader />
      <main className="container-app flex-1 py-6 space-y-6">
        <AppsSection
          loading={apps.loading}
          error={apps.error}
          data={apps.data}
        />

        <hr className="border-border" />

        <PermissionsSection
          loading={services.loading}
          error={services.error}
          data={services.data}
          onToggle={services.toggle}
        />
      </main>
      <footer className="container-app py-6 text-center text-[10px] uppercase tracking-widest text-muted-foreground">
      <a href="https://brunorochamoura.com">Made by brunorochamoura.com</a>
      </footer>
    </div>
  );
}

function AppsSection({
  loading,
  error,
  data,
}: {
  loading: boolean;
  error: Error | null;
  data: FrictionApp[];
}) {
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

        {/* The "add" card is always last, even while loading. */}
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

function PermissionsSection({
  loading,
  error,
  data,
  onToggle,
}: {
  loading: boolean;
  error: Error | null;
  data: ServiceStatus;
  onToggle: (key: keyof ServiceStatus) => void;
}) {
  // Keep the warning card's full footprint in the layout so the page
  // disposition stays identical whether the warning is shown or not.
  const anyDisabled = !loading && (!data.overlay || !data.accessibility);

  return (
    <section className="space-y-3">
      <h2 className="text-xs uppercase tracking-widest text-muted-foreground">
        Permissions
      </h2>

      {error && (
        <p className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          Couldn't load permissions: {error.message}
        </p>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {loading ? (
          <>
            <ServiceButtonSkeleton />
            <ServiceButtonSkeleton />
          </>
        ) : (
          <>
            <ServiceButton
              label="Overlay"
              enabled={data.overlay}
              onClick={() => onToggle("overlay")}
            />
            <ServiceButton
              label="Accessibility service"
              enabled={data.accessibility}
              onClick={() => onToggle("accessibility")}
            />
          </>
        )}
      </div>

      <div aria-live="polite">
        <p
          aria-hidden={!anyDisabled}
          className={`rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive ${
            anyDisabled ? "" : "invisible"
          }`}
        >
          Friction Timer needs both permissions to intercept apps. Tap a button above to grant.
        </p>
      </div>
    </section>
  );
}

function ServiceButton({
  label,
  enabled,
  onClick,
}: {
  label: string;
  enabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center justify-between gap-2 rounded-lg border border-border bg-card px-4 py-3 text-left transition-colors hover:border-primary"
    >
      <span className="truncate text-sm font-medium">{label}</span>
      <span
        aria-label={enabled ? "enabled" : "disabled"}
        className={`shrink-0 h-3 w-3 rounded-full ring-2 ring-offset-2 ring-offset-card ${
          enabled ? "bg-success ring-success/40" : "bg-destructive ring-destructive/40"
        }`}
      />
    </button>
  );
}
