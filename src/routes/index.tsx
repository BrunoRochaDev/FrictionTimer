import { createFileRoute } from "@tanstack/react-router";
import { HomePage } from "@/features/home/HomePage";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Friction Timer" },
      {
        name: "description",
        content: "Add intentional friction before opening distracting apps.",
      },
      { property: "og:title", content: "Friction Timer" },
      {
        property: "og:description",
        content: "Add intentional friction before opening distracting apps.",
      },
    ],
  }),
  component: HomePage,
});
