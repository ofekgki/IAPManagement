/** @type {import('tailwindcss').Config} */

/*
 * Centralized design tokens for the developer portal.
 *
 * This is the single source of truth for brand + status colors used by Tailwind utility classes
 * (e.g. `bg-primary`, `text-accent`, `border-border`). Keep it in sync with:
 *   - src/lib/theme.ts  -> the same values as JS constants for Recharts (charts can't use classes)
 *   - src/index.css     -> the same values mirrored as CSS variables for raw CSS / documentation
 *
 * Neutrals (page background, borders, primary/secondary/muted text) intentionally reuse Tailwind's
 * built-in `slate` scale, which already matches the requested palette exactly:
 *   slate-50 #F8FAFC (canvas) · slate-200 #E2E8F0 (border) · slate-900 #0F172A (text) ·
 *   slate-500 #64748B (secondary text) · slate-400 #94A3B8 (muted text).
 */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Brand
        primary: { DEFAULT: "#1E3A5F", hover: "#172C48" },
        secondary: "#334155",
        accent: { DEFAULT: "#14B8A6", hover: "#0F766E" },

        // Surfaces
        canvas: "#F8FAFC", // main app background
        card: "#FFFFFF", // surface / card background
        border: "#E2E8F0", // subtle hairline borders

        // Status
        success: "#16A34A",
        warning: "#F59E0B",
        error: "#DC2626",
        info: "#2563EB",

        // Legacy alias: existing components use `ink` for the strong brand color / primary action.
        // Repointed from the old near-black (#0d0f1a) to the brand primary navy so every existing
        // `bg-ink` / `text-ink` usage is themed consistently.
        ink: "#1E3A5F",

        // `surface` was the old body background token; keep it pointing at the canvas so
        // `bg-surface` in index.css keeps working.
        surface: "#F8FAFC",
      },
      borderRadius: {
        // Consistent radii: `rounded-lg` for controls, `rounded-xl` for cards (both already used).
        card: "0.75rem",
      },
      boxShadow: {
        // Subtle, unified card elevation for the SaaS-dashboard look.
        card: "0 1px 2px 0 rgb(15 23 42 / 0.04), 0 1px 3px 0 rgb(15 23 42 / 0.06)",
      },
    },
  },
  plugins: [],
};
