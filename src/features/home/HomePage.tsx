import { AppHeader } from "@/components/AppHeader";
import { openExternalUrl, useApps, useServiceStatus } from "@/lib/friction";
import { AppsSection } from "./apps-section";
import { PermissionsSection } from "./permissions-section";

const AUTHOR_URL = "https://brunorochamoura.com";

export function HomePage() {
  const apps = useApps();
  const services = useServiceStatus();

  const handleAuthorLinkClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    void openExternalUrl(AUTHOR_URL);
  };

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
          onOpenSettings={services.openSettings}
        />
      </main>
      <footer className="container-app py-6 text-center text-[10px] uppercase tracking-widest text-muted-foreground">
        <a
          href={AUTHOR_URL}
          target="_blank"
          rel="noreferrer"
          onClick={handleAuthorLinkClick}
        >
          Made by brunorochamoura.com
        </a>
      </footer>
    </div>
  );
}
