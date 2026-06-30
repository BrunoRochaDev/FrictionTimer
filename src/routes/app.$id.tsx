import { createFileRoute, useNavigate, useParams } from "@tanstack/react-router";
import { useEffect } from "react";
import { AppHeader } from "@/components/AppHeader";
import { AppForm } from "@/components/AppForm";
import { Skeleton } from "@/components/Skeletons";
import { useApp } from "@/lib/friction";

export const Route = createFileRoute("/app/$id")({
  head: () => ({
    meta: [
      { title: "Edit application — Friction Timer" },
      { name: "description", content: "Edit a limited application." },
    ],
  }),
  component: EditAppPage,
});

function EditAppPage() {
  const { id } = useParams({ from: "/app/$id" });
  const navigate = useNavigate();
  const { data: app, loading, error } = useApp(id);

  // Redirect to home only after the fetch has resolved and produced nothing.
  useEffect(() => {
    if (!loading && !error && app === null) {
      navigate({ to: "/" });
    }
  }, [loading, error, app, navigate]);

  return (
    <div className="min-h-screen flex flex-col">
      <AppHeader />
      <main className="container-app flex-1 py-6">
        <h2 className="mb-4 text-lg font-bold">Edit application</h2>

        {loading && <EditFormSkeleton />}

        {error && (
          <p className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
            Couldn't load app: {error.message}
          </p>
        )}

        {!loading && app && <AppForm initial={app} />}
      </main>
    </div>
  );
}

function EditFormSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-4 w-40" />
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}
