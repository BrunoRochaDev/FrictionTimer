import type { HTMLAttributes } from "react";

export function Skeleton({ className = "", ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={`skeleton ${className}`} {...rest} />;
}

export function AppCardSkeleton() {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3">
      <div className="min-w-0 flex-1 space-y-2">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-3 w-44" />
      </div>
      <div className="shrink-0 flex flex-col items-end gap-2">
        <Skeleton className="h-3 w-16" />
        <Skeleton className="h-3 w-20" />
      </div>
    </div>
  );
}

export function InstalledAppRowSkeleton() {
  return (
    <div className="flex items-center justify-between px-3 py-2.5">
      <div className="min-w-0 flex-1 space-y-2">
        <Skeleton className="h-3.5 w-28" />
        <Skeleton className="h-3 w-40" />
      </div>
      <Skeleton className="ml-2 h-2.5 w-2.5 rounded-full" />
    </div>
  );
}

export function ServiceButtonSkeleton() {
  return (
    <div className="flex items-center justify-between gap-2 rounded-lg border border-border bg-card px-4 py-3">
      <Skeleton className="h-4 w-28" />
      <Skeleton className="h-3 w-3 rounded-full" />
    </div>
  );
}
