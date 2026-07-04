import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { useCreateUser, useDeleteUser, useUsers } from "../hooks/usePortal";
import { Alert, Button, Card, Field, Input, PageHeader, Spinner } from "../components/ui";

export default function UsersPage() {
  const { user } = useAuth();
  const users = useUsers();
  const createUser = useCreateUser();
  const deleteUser = useDeleteUser();

  const [form, setForm] = useState({ email: "", password: "", displayName: "" });
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.email.trim() || form.password.length < 8) {
      return setError("Email and a password of at least 8 characters are required.");
    }
    try {
      await createUser.mutateAsync({
        email: form.email.trim(),
        password: form.password,
        displayName: form.displayName || undefined,
      });
      setForm({ email: "", password: "", displayName: "" });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add user");
    }
  }

  function onDelete(id: string, email: string) {
    if (window.confirm(`Delete user ${email}? This cannot be undone.`)) {
      deleteUser.mutate(id);
    }
  }

  return (
    <div>
      <PageHeader title="Users" subtitle="Everyone here has full access to every app." />

      <div className="max-w-3xl space-y-6">
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Add a user</h2>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert tone="error">{error}</Alert>}
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Email">
                <Input
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="teammate@example.com"
                />
              </Field>
              <Field label="Display name (optional)">
                <Input value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
              </Field>
              <Field label="Temporary password" hint="At least 8 characters. The user can change it later.">
                <Input
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
              </Field>
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={createUser.isPending}>
                {createUser.isPending ? "Adding…" : "Add user"}
              </Button>
            </div>
          </form>
        </Card>

        <Card className="overflow-x-auto p-0">
          <h2 className="px-4 pt-4 text-sm font-semibold text-slate-900">All users</h2>
          {users.isLoading && (
            <div className="p-4">
              <Spinner />
            </div>
          )}
          <table className="mt-3 w-full text-sm">
            <thead className="border-b border-border bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Display name</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {(users.data ?? []).map((u) => (
                <tr key={u.id} className="border-b border-slate-100 last:border-0">
                  <td className="px-4 py-3 font-medium">
                    {u.email}
                    {u.id === user?.id && <span className="ml-2 text-xs text-slate-400">(you)</span>}
                  </td>
                  <td className="px-4 py-3">{u.displayName ?? "—"}</td>
                  <td className="px-4 py-3 text-right">
                    {u.id !== user?.id && (
                      <Button variant="danger" disabled={deleteUser.isPending} onClick={() => onDelete(u.id, u.email)}>
                        Delete
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
              {(users.data ?? []).length === 0 && !users.isLoading && (
                <tr>
                  <td colSpan={3} className="px-4 py-6 text-center text-slate-500">
                    No users yet.
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
