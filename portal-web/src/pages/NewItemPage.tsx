import { useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useCreateItem } from "../hooks/usePortal";
import { Alert, Button, Card, Field, Input, Select, Textarea } from "../components/ui";
import { CURRENCIES, DEFAULT_CURRENCY } from "../lib/constants";
import type { ItemType } from "../types";

/** Major-unit price string (e.g. "0.59") → integer minor units (59). Avoids float rounding bugs. */
const toMinorUnits = (major: string) => Math.round((Number(major) || 0) * 100);

const snake = (s: string) =>
  s.trim().toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "");

export default function NewItemPage() {
  const { appId = "" } = useParams();
  const navigate = useNavigate();
  const createItem = useCreateItem(appId);

  const [name, setName] = useState("");
  const [itemId, setItemId] = useState("");
  const [itemIdTouched, setItemIdTouched] = useState(false);
  const [description, setDescription] = useState("");
  const [type, setType] = useState<ItemType>("NON_CONSUMABLE");
  const [entitlementId, setEntitlementId] = useState("");
  const [price, setPrice] = useState("1.99"); // major units (e.g. 1.99); converted to minor on submit
  const [currency, setCurrency] = useState<string>(DEFAULT_CURRENCY);
  const [googlePlayProductId, setGooglePlayProductId] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const effectiveItemId = itemIdTouched ? itemId : snake(name);
  const suggestedEntitlement = useMemo(
    () => (effectiveItemId ? `ent_${effectiveItemId}` : ""),
    [effectiveItemId],
  );
  const needsEntitlement = type !== "CONSUMABLE";

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!name.trim()) return setError("Name is required.");
    if (!effectiveItemId) return setError("Item ID is required.");
    const priceNum = Number(price);
    if (!Number.isFinite(priceNum) || priceNum < 0) return setError("Price must be a number 0 or more.");
    if (!currency.trim()) return setError("Currency is required.");

    const body = {
      name,
      itemId: effectiveItemId,
      description,
      type,
      entitlementId: entitlementId || (needsEntitlement ? suggestedEntitlement : null),
      priceAmountMinor: toMinorUnits(price),
      currency,
      googlePlayProductId: googlePlayProductId || null,
      isActive,
    };
    try {
      const item = await createItem.mutateAsync(body);
      navigate(`/apps/${appId}/items/${item.itemId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create item");
    }
  }

  return (
    <div>
      <Link to={`/apps/${appId}/items`} className="text-sm text-slate-500 hover:text-slate-800">
        ← Items
      </Link>
      <h1 className="mb-6 mt-2 text-xl font-semibold text-slate-900">New item</h1>

      <Card className="max-w-2xl">
        <form onSubmit={onSubmit} className="space-y-4">
          {error && <Alert tone="error">{error}</Alert>}
          <Field label="Item name">
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Remove Ads" />
          </Field>
          <Field label="Item ID" hint="lowercase snake_case, unique per app. Auto-generated from the name.">
            <Input
              value={effectiveItemId}
              onChange={(e) => {
                setItemIdTouched(true);
                setItemId(snake(e.target.value));
              }}
              className="font-mono"
              placeholder="remove_ads"
            />
          </Field>
          <Field label="Description">
            <Textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Type">
              <Select value={type} onChange={(e) => setType(e.target.value as ItemType)}>
                <option value="NON_CONSUMABLE">NON_CONSUMABLE</option>
                <option value="CONSUMABLE">CONSUMABLE</option>
                <option value="SUBSCRIPTION">SUBSCRIPTION</option>
              </Select>
            </Field>
            <Field
              label="Entitlement ID"
              hint={needsEntitlement ? `Required. Auto: ${suggestedEntitlement || "ent_…"}` : "Optional for consumables"}
            >
              <Input
                value={entitlementId}
                onChange={(e) => setEntitlementId(e.target.value)}
                className="font-mono"
                placeholder={suggestedEntitlement}
              />
            </Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label={`Price (${currency})`} hint={`e.g. 0.59 — stored as ${toMinorUnits(price)} minor units`}>
              <Input
                type="number"
                min={0}
                step="0.01"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="1.99"
              />
            </Field>
            <Field label="Currency">
              <Select value={currency} onChange={(e) => setCurrency(e.target.value)}>
                {CURRENCIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Active">
              <Select value={String(isActive)} onChange={(e) => setIsActive(e.target.value === "true")}>
                <option value="true">Active</option>
                <option value="false">Disabled</option>
              </Select>
            </Field>
          </div>
          <Field label="Google Play Product ID (optional)" hint="Required later for GOOGLE_PLAY production">
            <Input
              value={googlePlayProductId}
              onChange={(e) => setGooglePlayProductId(e.target.value)}
              className="font-mono"
            />
          </Field>
          <div className="flex justify-end gap-2">
            <Link to={`/apps/${appId}/items`}>
              <Button type="button" variant="secondary">
                Cancel
              </Button>
            </Link>
            <Button type="submit" disabled={createItem.isPending}>
              {createItem.isPending ? "Creating…" : "Create item"}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
