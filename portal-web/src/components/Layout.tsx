import { NavLink, Outlet, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useApp, useApps } from "../hooks/usePortal";
import { cn } from "./ui";

const NAV = [
  { to: "dashboard", label: "Dashboard" },
  { to: "api-keys", label: "API Keys" },
  { to: "items", label: "Items" },
  { to: "analytics", label: "Analytics" },
  { to: "analytics/revenue", label: "Revenue" },
  { to: "purchases", label: "Purchases" },
  { to: "entitlements", label: "Entitlements" },
  { to: "sdk-setup", label: "SDK Setup" },
];

export default function Layout() {
  const { appId: routeAppId = "" } = useParams();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { data: apps } = useApps();
  // Global pages (Settings, Users) have no :appId in the URL — fall back to the first app so the
  // app-scoped nav links and the app switcher still work.
  const appId = routeAppId || apps?.[0]?.id || "";
  const { data: app } = useApp(appId);

  return (
    <div className="flex h-full">
      {/* Sidebar */}
      <aside className="hidden w-60 shrink-0 flex-col border-r border-border bg-card md:flex">
        <div className="border-b border-border px-4 py-4">
          <div className="text-sm font-semibold text-slate-900">Purchase SDK</div>
          <div className="text-xs text-slate-500">Developer Portal</div>
        </div>
        <nav className="flex-1 space-y-1 p-3">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={`/apps/${appId}/${item.to}`}
              end={item.to === "analytics"}
              className={({ isActive }) =>
                cn(
                  "block rounded-lg px-3 py-2 text-sm font-medium",
                  isActive ? "bg-primary text-white" : "text-slate-600 hover:bg-slate-100",
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
          <NavLink
            to="/users"
            className={({ isActive }) =>
              cn(
                "block rounded-lg px-3 py-2 text-sm font-medium",
                isActive ? "bg-primary text-white" : "text-slate-600 hover:bg-slate-100",
              )
            }
          >
            Users
          </NavLink>
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              cn(
                "block rounded-lg px-3 py-2 text-sm font-medium",
                isActive ? "bg-primary text-white" : "text-slate-600 hover:bg-slate-100",
              )
            }
          >
            Settings
          </NavLink>
        </nav>
      </aside>

      {/* Main */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Top bar */}
        <header className="flex items-center justify-between gap-4 border-b border-border bg-card px-6 py-3">
          <div className="flex items-center gap-3">
            <select
              value={appId}
              onChange={(e) => navigate(`/apps/${e.target.value}/dashboard`)}
              className="rounded-lg border border-border bg-card px-3 py-1.5 text-sm"
            >
              {(apps ?? []).map((a) => (
                <option key={a.id} value={a.id}>
                  {a.appName}
                </option>
              ))}
            </select>
            <button onClick={() => navigate("/apps")} className="text-sm text-slate-500 hover:text-slate-800">
              All apps
            </button>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-slate-500">{user?.email}</span>
            <button onClick={logout} className="rounded-lg border border-border px-3 py-1.5 hover:bg-slate-50">
              Logout
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">
          <div className="mx-auto max-w-6xl">
            {app && !app.isActive && (
              <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-800">
                This app is deactivated.
              </div>
            )}
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
