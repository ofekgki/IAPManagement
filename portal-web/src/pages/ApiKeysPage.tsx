import { useState } from "react";
import { useParams } from "react-router-dom";
import { useApiKeys, useCreateApiKey, useRevokeApiKey, useRotateApiKey } from "../hooks/usePortal";
import { Alert, Badge, Button, Card, CopyButton, ErrorMessage, Field, Input, PageHeader, Spinner } from "../components/ui";
import { dateTime } from "../lib/format";
import type { CreatedApiKey } from "../types";

export default function ApiKeysPage() {
  const { appId = "" } = useParams();
  const keys = useApiKeys(appId);
  const createKey = useCreateApiKey(appId);
  const revokeKey = useRevokeApiKey(appId);
  const rotateKey = useRotateApiKey(appId);
  const [name, setName] = useState("");
  const [revealed, setRevealed] = useState<CreatedApiKey | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function create() {
    setError(null);
    if (!name.trim()) return setError("Key name is required.");
    try {
      setRevealed(await createKey.mutateAsync(name));
      setName("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create key");
    }
  }

  async function rotate(keyId: string) {
    try {
      setRevealed(await rotateKey.mutateAsync(keyId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to rotate key");
    }
  }

  return (
    <div>
      <PageHeader title="API Keys" subtitle="Keys identify your app when the SDK calls the backend." />

      <Alert tone="warning">
        This API key identifies your app when the SDK calls the backend. Since mobile apps can be reverse
        engineered, this key should <strong>not</strong> be treated as a strong secret.
      </Alert>

      {revealed && (
        <Card className="mt-4 border-green-300 bg-green-50">
          <h2 className="text-sm font-semibold text-green-900">Copy this API key now</h2>
          <p className="mb-2 text-xs text-green-800">You will not be able to see it again.</p>
          <div className="flex items-center gap-2">
            <code className="flex-1 overflow-x-auto rounded bg-white px-3 py-2 font-mono text-sm">{revealed.apiKey}</code>
            <CopyButton value={revealed.apiKey} label="Copy key" />
          </div>
          <button className="mt-3 text-xs text-green-800 underline" onClick={() => setRevealed(null)}>
            I've copied it — dismiss
          </button>
        </Card>
      )}

      <Card className="mt-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-900">Create a new key</h2>
        {error && <div className="mb-3"><Alert tone="error">{error}</Alert></div>}
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <Field label="Key name">
              <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Production key" />
            </Field>
          </div>
          <Button onClick={create} disabled={createKey.isPending}>
            {createKey.isPending ? "Creating…" : "Create key"}
          </Button>
        </div>
      </Card>

      <div className="mt-6">
        {keys.isLoading && <Spinner />}
        {keys.error && <ErrorMessage error={keys.error} />}
        <Card className="overflow-x-auto p-0">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Prefix</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Created</th>
                <th className="px-4 py-3">Last used</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {(keys.data ?? []).map((k) => (
                <tr key={k.id} className="border-b border-slate-100 last:border-0">
                  <td className="px-4 py-3 font-medium">{k.name}</td>
                  <td className="px-4 py-3 font-mono text-xs">{k.keyPrefix}…</td>
                  <td className="px-4 py-3">
                    <Badge tone={k.status === "ACTIVE" ? "green" : "red"}>{k.status}</Badge>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{dateTime(k.createdAt)}</td>
                  <td className="px-4 py-3 text-slate-500">{dateTime(k.lastUsedAt)}</td>
                  <td className="px-4 py-3 text-right">
                    {k.status === "ACTIVE" && (
                      <div className="flex justify-end gap-2">
                        <Button variant="secondary" onClick={() => rotate(k.id)}>
                          Rotate
                        </Button>
                        <Button variant="danger" onClick={() => revokeKey.mutate(k.id)}>
                          Revoke
                        </Button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
              {(keys.data ?? []).length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-6 text-center text-slate-500">
                    No API keys yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Card>
      </div>
    </div>
  );
}
