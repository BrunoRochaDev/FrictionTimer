import { ServiceButtonSkeleton } from "@/components/Skeletons";
import type { ServicePermissionKey, ServiceStatus } from "@/lib/friction";

type PermissionsSectionProps = {
  loading: boolean;
  error: Error | null;
  data: ServiceStatus;
  onOpenSettings: (key: ServicePermissionKey) => Promise<void>;
};

export function PermissionsSection({
  loading,
  error,
  data,
  onOpenSettings,
}: PermissionsSectionProps) {
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

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
              onClick={() => {
                void onOpenSettings("overlay");
              }}
            />
            <ServiceButton
              label="Accessibility Service"
              enabled={data.accessibility}
              onClick={() => {
                void onOpenSettings("accessibility");
              }}
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
          Friction Timer needs both permissions to intercept apps. Tap a button
          above to grant.
        </p>
      </div>
    </section>
  );
}

type ServiceButtonProps = {
  label: string;
  enabled: boolean;
  onClick: () => void;
};

function ServiceButton({ label, enabled, onClick }: ServiceButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center justify-between gap-2 rounded-lg border border-border bg-card px-4 py-3 text-left transition-colors hover:border-primary"
    >
      <span className="truncate text-sm font-medium">{label}</span>
      <span
        aria-label={enabled ? "enabled" : "disabled"}
        className={`h-3 w-3 shrink-0 rounded-full ring-2 ring-offset-2 ring-offset-card ${
          enabled
            ? "bg-success ring-success/40"
            : "bg-destructive ring-destructive/40"
        }`}
      />
    </button>
  );
}
