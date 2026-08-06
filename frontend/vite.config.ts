/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // three/@react-three are only reachable through LandingPage's route-level React.lazy()
  // (see App.tsx) - listing them here keeps Vite's dev-server dependency scanner from
  // discovering them late through the dynamic import and pre-bundling a second copy.
  optimizeDeps: {
    include: ['three', '@react-three/fiber', '@react-three/drei'],
  },
  resolve: {
    dedupe: ['react', 'react-dom', 'three', '@react-three/fiber', '@react-three/drei'],
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
})
