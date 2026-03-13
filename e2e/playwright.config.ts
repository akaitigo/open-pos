import path from 'node:path'
import { defineConfig } from '@playwright/test'

const rootDir = path.resolve(process.cwd(), '..')

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'github' : 'html',
  webServer: [
    {
      command: 'pnpm --filter pos-terminal dev -- --host 127.0.0.1 --port 5173',
      url: 'http://127.0.0.1:5173',
      cwd: rootDir,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: 'pnpm --filter admin-dashboard dev -- --host 127.0.0.1 --port 5174',
      url: 'http://127.0.0.1:5174',
      cwd: rootDir,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
})
