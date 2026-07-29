/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // The airplane 3D scene is behind React.lazy() for code-splitting, so Vite's dev-server
  // dependency scanner (a static esbuild pass) doesn't reliably discover three/@react-three
  // through the dynamic import - it ends up pre-bundling them late/twice, producing two live
  // copies of the module graph and a genuine "Invalid hook call" from @react-three/fiber's
  // <Canvas> (it uses React's internal dispatcher directly for useFrame et al., so two copies
  // of react-three-fiber really do break hooks, not just waste bytes). Listing them here forces
  // a single, upfront optimization pass.
  optimizeDeps: {
    include: ['three', '@react-three/fiber', '@react-three/drei'],
  },
  // Belt-and-suspenders for the same duplicate-instance problem: drei's barrel export pulls in
  // enough sub-modules that Vite's optimizer still emitted a second, differently-hashed copy of
  // three/@react-three-fiber alongside the primary one even with optimizeDeps.include above.
  // dedupe forces every resolution of these to the single physical package in node_modules.
  resolve: {
    dedupe: ['react', 'react-dom', 'three', '@react-three/fiber', '@react-three/drei'],
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
})
