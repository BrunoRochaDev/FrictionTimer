import { createFileRoute } from "@tanstack/react-router";
import { AppHeader } from "@/components/AppHeader";
import { AppForm } from "@/components/AppForm";

export const Route = createFileRoute("/app/new")({
  head: () => ({
    meta: [
      { title: "Add application — Friction Timer" },
      { name: "description", content: "Add a new application to limit." },
    ],
  }),
  component: NewAppPage,
});

function NewAppPage() {
  return (
    <div className="min-h-screen flex flex-col">
      <AppHeader />
      <main className="container-app flex-1 py-6">
        <h2 className="mb-4 text-lg font-bold">New application</h2>
        <AppForm />
      </main>
    </div>
  );
}
