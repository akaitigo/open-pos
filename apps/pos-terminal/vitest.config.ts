import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      thresholds: {
        lines: 65, // TODO: 段階的に 80% へ引き上げ（50→65→80）
      },
      exclude: [
        'node_modules/**',
        'src/test-setup.ts',
        '**/*.d.ts',
        '**/*.config.*',
        'dist/**',
        'src/components/ui/**',
        'src/main.tsx',
      ],
    },
  },
})
