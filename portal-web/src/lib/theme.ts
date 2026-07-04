/*
 * Design tokens as JS constants.
 *
 * Use these ONLY where Tailwind utility classes can't reach — most importantly Recharts, which takes
 * raw color strings for `fill` / `stroke`. For everything rendered as normal DOM, prefer the Tailwind
 * tokens (`bg-primary`, `text-accent`, `border-border`, …) so styling stays declarative.
 *
 * Single source of truth lives in tailwind.config.js; keep these values in sync with it.
 */

/** Brand colors. */
export const BRAND = {
  primary: "#1E3A5F",
  primaryHover: "#172C48",
  secondary: "#334155",
  accent: "#14B8A6",
  accentHover: "#0F766E",
} as const;

/** Status colors (success / warning / error / info). */
export const STATUS = {
  success: "#16A34A",
  warning: "#F59E0B",
  error: "#DC2626",
  info: "#2563EB",
} as const;

/** Neutral surface tokens. */
export const SURFACE = {
  canvas: "#F8FAFC",
  card: "#FFFFFF",
  border: "#E2E8F0",
  text: "#0F172A",
  textSecondary: "#64748B",
  textMuted: "#94A3B8",
} as const;

/**
 * Semantic chart colors. Each analytics metric has ONE canonical color across the whole product so a
 * reader learns the legend once:
 *   revenue        -> accent teal
 *   purchases      -> info blue
 *   cancellations  -> warning orange
 *   failures       -> error red
 *   popupOpened    -> purple
 */
export const CHART = {
  revenue: BRAND.accent,
  purchases: STATUS.info,
  cancellations: STATUS.warning,
  failures: STATUS.error,
  popupOpened: "#8B5CF6",
} as const;

/** Grid / axis hairline for charts on the light canvas. */
export const CHART_GRID = SURFACE.border;

/** Fill for the revenue area chart (accent teal at low opacity over the light canvas). */
export const CHART_REVENUE_FILL = "#14B8A620";

/**
 * Categorical palette for charts that plot arbitrary series (e.g. revenue by billing mode). Ordered
 * for good adjacent contrast; starts with the brand primary.
 */
export const CHART_PALETTE = [
  BRAND.primary,
  CHART.purchases,
  BRAND.accent,
  STATUS.warning,
  CHART.popupOpened,
  STATUS.success,
] as const;
