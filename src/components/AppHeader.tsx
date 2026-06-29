import { Link } from "@tanstack/react-router";

export function AppHeader() {
  return (
    <header className="sticky top-0 z-10 border-b border-border bg-background/95 backdrop-blur">
      <div className="container-app flex h-14 items-center justify-between">
        <Link to="/" className="flex items-center gap-2 group">
          <span className="inline-block h-2.5 w-2.5 rounded-full bg-primary group-hover:bg-accent transition-colors" />
          <h1 className="text-base font-bold tracking-tight">
            <span className="text-primary">Friction</span>
            <span className="text-foreground"> Timer</span>
          </h1>
        </Link>
        <span className="text-[10px] uppercase tracking-widest text-muted-foreground">
          v0.1
        </span>
      </div>
    </header>
  );
}
