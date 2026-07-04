import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The portal talks to the backend via VITE_API_BASE_URL (default http://localhost:8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
  },
  preview: {
    port: 5173,
    host: true,
  },
});
