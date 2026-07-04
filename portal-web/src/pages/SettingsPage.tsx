import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { useResetDemoData, useUpdateProfile } from "../hooks/usePortal";
import { Alert, Button, Card, Field, Input, PageHeader } from "../components/ui";

export default function SettingsPage() {
  const { user, applyAuth } = useAuth();
  const reset = useResetDemoData();
  const updateProfile = useUpdateProfile();

  // Profile edit form (email, display name, new password). Password left blank = unchanged.
  const [email, setEmail] = useState(user?.email ?? "");
  const [displayName, setDisplayName] = useState(user?.displayName ?? "");
  const [password, setPassword] = useState("");
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSaved, setProfileSaved] = useState(false);

  async function saveProfile(e: FormEvent) {
    e.preventDefault();
    setProfileError(null);
    setProfileSaved(false);
    if (password && password.length < 8) {
      return setProfileError("New password must be at least 8 characters (or leave it blank).");
    }
    try {
      const res = await updateProfile.mutateAsync({
        email: email.trim() || undefined,
        displayName: displayName.trim(),
        password: password || undefined,
      });
      applyAuth(res); // refresh token + user (email change re-issues the JWT)
      setPassword("");
      setProfileSaved(true);
    } catch (err) {
      setProfileError(err instanceof Error ? err.message : "Could not save profile.");
    }
  }

  // Both actions wipe everything; "regenerate" then re-seeds the realistic demo dataset.
  const run = (reseed: boolean) => {
    const message = reseed
      ? "Delete ALL current data and regenerate a fresh demo dataset?\n\nThis cannot be undone."
      : "Delete ALL data and leave the database empty?\n\nThis cannot be undone.";
    if (window.confirm(message)) {
      reset.mutate(reseed);
    }
  };

  return (
    <div>
      <PageHeader title="Settings" subtitle="Manage your profile and demo data." />

      <div className="max-w-xl space-y-6">
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Profile</h2>
          <form onSubmit={saveProfile} className="space-y-4">
            {profileError && <Alert tone="error">{profileError}</Alert>}
            {profileSaved && <Alert tone="success">Profile updated.</Alert>}
            <Field label="Email">
              <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </Field>
            <Field label="Display name">
              <Input value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            </Field>
            <Field label="New password" hint="Leave blank to keep your current password. Minimum 8 characters.">
              <Input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </Field>
            <div className="flex justify-end">
              <Button type="submit" disabled={updateProfile.isPending}>
                {updateProfile.isPending ? "Saving…" : "Save profile"}
              </Button>
            </div>
          </form>
        </Card>

        {/* Danger zone: demo-only data reset. The backend endpoint only exists when seeding is
            enabled (dev/docker), so on a real deployment these buttons simply return an error. */}
        <Card className="border-red-200">
          <h2 className="mb-1 text-sm font-semibold text-red-700">Danger zone — demo data</h2>
          <p className="mb-4 text-xs text-slate-500">
            Reset the demo database so you can generate the realistic Jan&nbsp;2026→today dataset from
            scratch (items, purchases, entitlements, and analytics events). Your portal login is kept.
          </p>

          {reset.isSuccess && reset.data && (
            <div className="mb-4">
              <Alert tone="success">
                {reset.data.reseeded
                  ? `Regenerated: ${reset.data.items} items, ${reset.data.purchases} purchases, ${reset.data.entitlements} entitlements, ${reset.data.events} analytics events.`
                  : "All demo data deleted. The database is now empty."}
              </Alert>
            </div>
          )}
          {reset.isError && (
            <div className="mb-4">
              <Alert tone="error">{(reset.error as Error)?.message ?? "Reset failed."}</Alert>
            </div>
          )}

          <div className="flex flex-wrap gap-3">
            <Button variant="danger" disabled={reset.isPending} onClick={() => run(true)}>
              {reset.isPending ? "Working…" : "Reset & regenerate demo data"}
            </Button>
            <Button variant="secondary" disabled={reset.isPending} onClick={() => run(false)}>
              Delete all data (empty)
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
