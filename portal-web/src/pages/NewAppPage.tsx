import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCreateApp } from "../hooks/usePortal";
import { Alert, Button, Card, Field, Input, Select, Textarea } from "../components/ui";

const PACKAGE_RE = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/;

export default function NewAppPage() {
  const navigate = useNavigate();
  const createApp = useCreateApp();
  const [appName, setAppName] = useState("");
  const [packageName, setPackageName] = useState("");
  const [defaultBillingMode, setDefaultBillingMode] = useState("MOCK");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!appName.trim()) return setError("App name is required.");
    if (!PACKAGE_RE.test(packageName)) return setError("Package name must look like com.example.app");
    try {
      const app = await createApp.mutateAsync({ appName, packageName, defaultBillingMode, description } as never);
      navigate(`/apps/${app.id}/dashboard`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create app");
    }
  }

  return (
    <div className="min-h-screen bg-surface">
      <header className="border-b border-slate-200 bg-white px-6 py-3">
        <Link to="/apps" className="text-sm text-slate-500 hover:text-slate-800">
          ← All apps
        </Link>
      </header>
      <main className="mx-auto max-w-xl p-6">
        <h1 className="mb-1 text-xl font-semibold text-slate-900">Create app</h1>
        <p className="mb-6 text-sm text-slate-500">Example: Demo Game / com.example.demogame / MOCK</p>
        <Card>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert tone="error">{error}</Alert>}
            <Field label="App name">
              <Input value={appName} onChange={(e) => setAppName(e.target.value)} placeholder="Demo Game" />
            </Field>
            <Field label="Package name" hint="Must look like com.example.app">
              <Input
                value={packageName}
                onChange={(e) => setPackageName(e.target.value)}
                placeholder="com.example.demogame"
                className="font-mono"
              />
            </Field>
            <Field label="Default billing mode">
              <Select value={defaultBillingMode} onChange={(e) => setDefaultBillingMode(e.target.value)}>
                <option value="MOCK">MOCK (demo / no Google Play setup)</option>
                <option value="GOOGLE_PLAY">GOOGLE_PLAY (future real billing)</option>
              </Select>
            </Field>
            <Field label="Description (optional)">
              <Textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
            </Field>
            <div className="flex justify-end gap-2">
              <Link to="/apps">
                <Button type="button" variant="secondary">
                  Cancel
                </Button>
              </Link>
              <Button type="submit" disabled={createApp.isPending}>
                {createApp.isPending ? "Creating…" : "Create app"}
              </Button>
            </div>
          </form>
        </Card>
      </main>
    </div>
  );
}
