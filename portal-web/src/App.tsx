import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import AppsPage from "./pages/AppsPage";
import NewAppPage from "./pages/NewAppPage";
import SettingsPage from "./pages/SettingsPage";
import UsersPage from "./pages/UsersPage";
import DashboardPage from "./pages/DashboardPage";
import ApiKeysPage from "./pages/ApiKeysPage";
import ItemsPage from "./pages/ItemsPage";
import NewItemPage from "./pages/NewItemPage";
import ItemDetailPage from "./pages/ItemDetailPage";
import AnalyticsPage from "./pages/AnalyticsPage";
import RevenuePage from "./pages/RevenuePage";
import PurchasesPage from "./pages/PurchasesPage";
import PurchaseDetailPage from "./pages/PurchaseDetailPage";
import EntitlementsPage from "./pages/EntitlementsPage";
import SdkSetupPage from "./pages/SdkSetupPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route path="/" element={<Navigate to="/apps" replace />} />
      <Route path="/apps" element={<ProtectedRoute><AppsPage /></ProtectedRoute>} />
      <Route path="/apps/new" element={<ProtectedRoute><NewAppPage /></ProtectedRoute>} />

      {/* Global pages (Settings, Users) share the same sidebar/topbar chrome as app pages. */}
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/users" element={<UsersPage />} />
      </Route>

      <Route
        path="/apps/:appId"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="api-keys" element={<ApiKeysPage />} />
        <Route path="items" element={<ItemsPage />} />
        <Route path="items/new" element={<NewItemPage />} />
        <Route path="items/:itemId" element={<ItemDetailPage />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="analytics/revenue" element={<RevenuePage />} />
        <Route path="purchases" element={<PurchasesPage />} />
        <Route path="purchases/:purchaseId" element={<PurchaseDetailPage />} />
        <Route path="entitlements" element={<EntitlementsPage />} />
        <Route path="sdk-setup" element={<SdkSetupPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/apps" replace />} />
    </Routes>
  );
}
