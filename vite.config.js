import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  // Root-relative by default (Vercel, Cloudflare Pages, local, most hosts).
  // GitHub Pages serves under /UBBInfo/, so its workflow builds with
  // VITE_BASE=/UBBInfo/. See .github/workflows/deploy.yml.
  base: process.env.VITE_BASE || '/',
  plugins: [react()],
  server: {
    port: 5173,
    open: true,
  },
});
