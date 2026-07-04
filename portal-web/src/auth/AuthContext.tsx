import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, TOKEN_KEY } from "../lib/api";
import type { AuthResponse, User } from "../types";

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  /** Persist a fresh token + user (e.g. after editing your own profile). */
  applyAuth: (res: AuthResponse) => void;
  logout: () => void;
}

const AuthCtx = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      setLoading(false);
      return;
    }
    api
      .get<User>("/api/v1/portal/auth/me")
      .then(setUser)
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false));
  }, []);

  async function persist(res: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, res.token);
    setUser(res.user);
  }

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      login: async (email, password) => {
        persist(await api.post<AuthResponse>("/api/v1/portal/auth/login", { email, password }));
      },
      register: async (email, password, displayName) => {
        persist(
          await api.post<AuthResponse>("/api/v1/portal/auth/register", { email, password, displayName }),
        );
      },
      applyAuth: (res) => persist(res),
      logout: () => {
        localStorage.removeItem(TOKEN_KEY);
        setUser(null);
        window.location.assign("/login");
      },
    }),
    [user, loading],
  );

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
