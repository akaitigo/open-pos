import path from 'node:path'
import { defineConfig } from '@playwright/test'

const rootDir = path.resolve(process.cwd(), '..')
const webServerTimeout = process.env.CI ? 300_000 : 120_000

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  workers: process.env.CI ? 1 : undefined,
  timeout: 30_000,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'github' : 'html',
  webServer: [
    {
      command: 'pnpm --filter pos-terminal exec vite --host 127.0.0.1 --port 5173 --strictPort',
      url: 'http://127.0.0.1:5173',
      cwd: rootDir,
      reuseExistingServer: !process.env.CI,
      timeout: webServerTimeout,
    },
    {
      command: 'pnpm --filter admin-dashboard exec vite --host 127.0.0.1 --port 5174 --strictPort',
      url: 'http://127.0.0.1:5174',
      cwd: rootDir,
      reuseExistingServer: !process.env.CI,
      timeout: webServerTimeout,
    },
  ],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
})
