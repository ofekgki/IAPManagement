import { useState, type ReactElement } from "react";
import { useParams } from "react-router-dom";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useRevenue, useRevenueByProduct } from "../hooks/usePortal";
import { FilterBar, type FilterState } from "../components/Filters";
import { Card, ErrorMessage, Kpi, PageHeader, Spinner } from "../components/ui";
import { money, num, percent } from "../lib/format";
import { paymentMethodLabel } from "../types";
import { CHART, CHART_GRID, CHART_PALETTE, CHART_REVENUE_FILL } from "../lib/theme";

export default function RevenuePage() {
  const { appId = "" } = useParams();
  // Default the revenue view to year-to-date (from 1 Jan of the current calendar year).
  const [filters, setFilters] = useState<FilterState>({
    groupBy: "day",
    from: `${new Date().getFullYear()}-01-01`,
  });
  const revenue = useRevenue(appId, filters);
  const byProduct = useRevenueByProduct(appId, filters);

  const currency = revenue.data?.currency ?? "USD";
  const overTime = (revenue.data?.overTime ?? []).map((p) => ({
    bucket: p.bucket,
    revenue: p.revenueMinor / 100,
    purchases: p.purchases,
  }));
  const products = (byProduct.data ?? []).map((p) => ({
    name: p.name,
    revenue: p.totalRevenueMinor / 100,
    purchases: p.successfulPurchases,
  }));
  const byMethod = (revenue.data?.byPaymentMethod ?? []).map((m) => ({
    name: paymentMethodLabel(m.paymentMethod),
    value: m.revenueMinor / 100,
  }));

  return (
    <div>
      <PageHeader title="Revenue" subtitle="Counted from SUCCESS purchases only, using priceAmountMinor." />
      <FilterBar appId={appId} value={filters} onChange={setFilters} showGroupBy />

      {revenue.isLoading && <Spinner />}
      {revenue.error && <ErrorMessage error={revenue.error} />}

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="Total revenue" value={money(revenue.data?.totalRevenueMinor, currency)} sub="net of restores" />
        <Kpi
          label="Restored (refunded)"
          value={money(revenue.data?.restoredValueMinor, currency)}
          sub={`${num(revenue.data?.restoredCount)} restored · subtracted from revenue`}
        />
        <Kpi label="Products sold" value={num(products.filter((p) => p.purchases > 0).length)} />
        <Kpi label="Currency" value={currency} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Revenue over time</h2>
          <ChartFrame>
            <AreaChart data={overTime}>
              <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID} />
              <XAxis dataKey="bucket" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip formatter={(v: number) => money(Math.round(v * 100), currency)} />
              <Area type="monotone" dataKey="revenue" stroke={CHART.revenue} fill={CHART_REVENUE_FILL} />
            </AreaChart>
          </ChartFrame>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Purchases over time</h2>
          <ChartFrame>
            <BarChart data={overTime}>
              <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID} />
              <XAxis dataKey="bucket" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="purchases" fill={CHART.purchases} />
            </BarChart>
          </ChartFrame>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Revenue by product</h2>
          <ChartFrame>
            <BarChart data={products}>
              <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID} />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip formatter={(v: number) => money(Math.round(v * 100), currency)} />
              <Bar dataKey="revenue" fill={CHART.revenue} />
            </BarChart>
          </ChartFrame>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Revenue by payment method</h2>
          <ChartFrame height={300}>
            <PieChart>
              <Pie data={byMethod} dataKey="value" nameKey="name" cy="42%" outerRadius={80} label>
                {byMethod.map((_, i) => (
                  <Cell key={i} fill={CHART_PALETTE[i % CHART_PALETTE.length]} />
                ))}
              </Pie>
              <Legend verticalAlign="bottom" height={36} wrapperStyle={{ paddingTop: 12 }} />
              <Tooltip formatter={(v: number) => money(Math.round(v * 100), currency)} />
            </PieChart>
          </ChartFrame>
        </Card>
      </div>

      <Card className="mt-6 overflow-x-auto p-0">
        <h2 className="px-4 pt-4 text-sm font-semibold text-slate-900">Revenue by product</h2>
        <table className="mt-3 w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Product</th>
              <th className="px-4 py-3">Item ID</th>
              <th className="px-4 py-3">Successful</th>
              <th className="px-4 py-3">Revenue</th>
              <th className="px-4 py-3">Avg</th>
              <th className="px-4 py-3">Popup views</th>
              <th className="px-4 py-3">Conversion</th>
            </tr>
          </thead>
          <tbody>
            {(byProduct.data ?? []).map((p) => (
              <tr key={p.itemId} className="border-b border-slate-100 last:border-0">
                <td className="px-4 py-3 font-medium">{p.name}</td>
                <td className="px-4 py-3 font-mono text-xs">{p.itemId}</td>
                <td className="px-4 py-3">{num(p.successfulPurchases)}</td>
                <td className="px-4 py-3">{money(p.totalRevenueMinor, p.currency ?? currency)}</td>
                <td className="px-4 py-3">{money(p.averageRevenueMinor, p.currency ?? currency)}</td>
                <td className="px-4 py-3">{num(p.popupViews)}</td>
                <td className="px-4 py-3">{percent(p.conversionRate)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}

function ChartFrame({ children, height = 240 }: { children: ReactElement; height?: number }) {
  return (
    <div style={{ width: "100%", height }}>
      <ResponsiveContainer>{children}</ResponsiveContainer>
    </div>
  );
}
