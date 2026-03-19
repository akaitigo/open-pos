import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '@playwright/test'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '../..')
const screenshotDir = path.join(repoRoot, 'docs/assets/demo')
const rawCaptureDir = path.join(repoRoot, '.local/demo-assets')
const posBaseUrl = process.env.POS_BASE_URL ?? 'http://localhost:5173'
const adminBaseUrl = process.env.ADMIN_BASE_URL ?? 'http://localhost:5174'
const posStaffName = process.env.POS_STAFF_NAME ?? '渋谷店 オーナー'
const posPin = process.env.POS_PIN ?? '1234'
const featuredProduct = process.env.POS_PRODUCT_ONE ?? 'ドリップコーヒー'
const secondaryProduct = process.env.POS_PRODUCT_TWO ?? '北海道おにぎり鮭'
const posConfigPath = path.join(repoRoot, 'apps/pos-terminal/public/demo-config.json')
const adminViewport = { width: 1440, height: 960 }
const posViewport = { width: 1440, height: 960 }
const videoViewport = { width: 1280, height: 900 }

async function readStoreId() {
  const raw = await fs.readFile(posConfigPath, 'utf8')
  const parsed = JSON.parse(raw)
  if (!parsed.storeId) {
    throw new Error(`storeId is missing in ${posConfigPath}`)
  }
  return parsed.storeId
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function dismissToastIfVisible(page) {
  const closeButton = page.getByRole('button', { name: 'Close notification' }).last()
  if (await closeButton.isVisible().catch(() => false)) {
    await closeButton.click()
  }
}

async function loginToPos(page) {
  await page.goto(posBaseUrl, { waitUntil: 'networkidle' })
  await page.getByText('OpenPOS Terminal').waitFor()
  await page.getByRole('button', { name: new RegExp(posStaffName) }).click()
  for (const digit of posPin) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }
  await page.getByRole('button', { name: 'ログイン' }).click()
  await page.getByRole('link', { name: 'OpenPOS' }).waitFor()
}

async function captureAdminDashboard(browser, storeId) {
  const context = await browser.newContext({ viewport: adminViewport })
  const page = await context.newPage()

  await page.goto(adminBaseUrl, { waitUntil: 'networkidle' })
  await page.getByRole('heading', { name: 'ダッシュボード' }).waitFor()
  await sleep(800)
  await page.screenshot({
    path: path.join(screenshotDir, 'admin-dashboard.png'),
    animations: 'disabled',
  })

  await page.getByRole('link', { name: '在庫管理' }).click()
  await page.getByRole('heading', { name: '在庫管理' }).waitFor()
  await page.getByLabel('店舗ID').fill(storeId)
  await page.locator('tbody tr').first().waitFor()
  await sleep(500)
  await page.screenshot({
    path: path.join(screenshotDir, 'admin-inventory.png'),
    animations: 'disabled',
  })

  await context.close()
}

async function capturePosScreenshots(browser) {
  const context = await browser.newContext({ viewport: posViewport })
  const page = await context.newPage()

  await loginToPos(page)
  await sleep(600)
  await page.screenshot({
    path: path.join(screenshotDir, 'pos-products.png'),
    animations: 'disabled',
  })

  await context.close()
}

async function capturePosCheckoutVideo(browser) {
  const context = await browser.newContext({
    viewport: videoViewport,
    recordVideo: {
      dir: rawCaptureDir,
      size: videoViewport,
    },
  })
  const page = await context.newPage()

  await loginToPos(page)
  await sleep(500)

  await page.locator('.cursor-pointer').filter({ hasText: featuredProduct }).first().click()
  await sleep(400)
  await dismissToastIfVisible(page)
  await page.locator('.cursor-pointer').filter({ hasText: secondaryProduct }).first().click()
  await sleep(500)
  await dismissToastIfVisible(page)
  await page.getByRole('button', { name: /お会計/ }).click()
  await sleep(500)
  await page.getByRole('button', { name: 'ぴったり' }).click()
  await sleep(300)
  await page.getByRole('button', { name: 'お会計を確定' }).click()
  await page.getByRole('dialog').filter({ hasText: 'レシート' }).waitFor()
  await sleep(1200)

  const video = page.video()
  await context.close()

  if (!video) {
    throw new Error('Playwright did not produce a checkout video')
  }

  const recordedPath = await video.path()
  await fs.copyFile(recordedPath, path.join(rawCaptureDir, 'pos-checkout.webm'))
}

async function main() {
  await fs.mkdir(screenshotDir, { recursive: true })
  await fs.mkdir(rawCaptureDir, { recursive: true })

  const storeId = await readStoreId()
  const browser = await chromium.launch({ headless: true })

  try {
    await captureAdminDashboard(browser, storeId)
    await capturePosScreenshots(browser)
    await capturePosCheckoutVideo(browser)
  } finally {
    await browser.close()
  }

  console.log(`Wrote README demo screenshots to ${screenshotDir}`)
  console.log(`Wrote raw checkout capture to ${path.join(rawCaptureDir, 'pos-checkout.webm')}`)
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error)
  process.exitCode = 1
})
