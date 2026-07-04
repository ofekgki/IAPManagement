// Display helpers. Money is stored in minor units (cents) on the backend.

export function money(minor: number | null | undefined, currency = "USD"): string {
  const amount = (minor ?? 0) / 100;
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}

export function percent(ratio: number | null | undefined, digits = 1): string {
  return `${((ratio ?? 0) * 100).toFixed(digits)}%`;
}

export function dateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleString();
}

export function date(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export function num(n: number | null | undefined): string {
  return (n ?? 0).toLocaleString();
}
