import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useApps } from "../hooks/usePortal";
import { Badge, Button, Card, ErrorMessage, Spinner } from "../components/ui";

function TopNav() {
  const { user, logout } = useAuth();
  return (
    <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
      <div className="font-semibold text-slate-900">Purchase SDK Portal</div>
      <div className="flex items-center gap-3 text-sm">
        <Link to="/users" className="text-slate-500 hover:text-slate-800">
          Users
        </Link>
        <Link to="/settings" className="text-slate-500 hover:text-slate-800">
          Settings
        </Link>
        <span className="text-slate-500">{user?.email}</span>
        <button onClick={logout} className="rounded-lg border border-border px-3 py-1.5 hover:bg-slate-50">
          Logout
        </button>
      </div>
    </header>
  );
}

export default function AppsPage() {
  const { data: apps, isLoading, error } = useApps();

  return (
    <div className="min-h-screen bg-surface">
      <TopNav />
      <main className="mx-auto max-w-5xl p-6">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Your apps</h1>
            <p className="mt-1 text-sm text-slate-500">Each app gets its own API keys, items, and analytics.</p>
          </div>
          <Link to="/apps/new">
            <Button>+ New app</Button>
          </Link>
        </div>

        {isLoading && <Spinner />}
        {error && <ErrorMessage error={error} />}

        {apps && apps.length === 0 && (
          <Card>
            <p className="text-sm text-slate-600">
              You don't have any apps yet. Create your first app to get an API key and start adding items.
            </p>
          </Card>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          {apps?.map((app) => (
            <Link key={app.id} to={`/apps/${app.id}/dashboard`}>
              <Card className="transition hover:border-ink">
                <div className="flex items-center justify-between">
                  <h2 className="font-semibold text-slate-900">{app.appName}</h2>
                  {app.isActive ? <Badge tone="green">Active</Badge> : <Badge tone="red">Inactive</Badge>}
                </div>
                <p className="mt-1 font-mono text-xs text-slate-500">{app.packageName}</p>
                <p className="mt-3 text-xs text-slate-500">
                  Default billing: <span className="font-medium">{app.defaultBillingMode}</span>
                </p>
              </Card>
            </Link>
          ))}
        </div>
      </main>
    </div>
  );
}
