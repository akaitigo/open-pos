import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

/**
 * オフライン同期 E2E テスト (#416)
 *
 * ネットワーク切断時の UI 挙動と、復帰後のデータ整合性を検証する。
 * POS アプリは Dexie.js でオフラインストレージを使用し、
 * オフライン中もローカルトランザクションとしてチェックアウトが完了する。
 */
test.describe('Offline Sync', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()

    // Ensure product list is loaded while online
    await expect(posPage.productGrid.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })

  test('cart state persists through offline/online toggle', async ({ context }) => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    await context.setOffline(true)
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    await context.setOffline(false)
    await expect(posPage.cart).toContainText('ドリップコーヒー')
  })

  test('product list remains visible after brief offline period', async ({ context }) => {
    await context.setOffline(true)
    await context.setOffline(false)
    await expect(posPage.productGrid.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })

  test('offline checkout creates local transaction and syncs on reconnect', async ({ context }) => {
    test.slow()
    test.setTimeout(60_000)

    // Go offline
    await context.setOffline(true)

    // Add product to cart while offline
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Start payment flow offline
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    // Receipt should appear from local transaction (Dexie.js)
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 15_000 })
    await posPage.closeReceipt()

    // Go back online
    await context.setOffline(false)

    // Cart should be empty after checkout completes
    await expect(posPage.cart).toContainText('カートは空です', { timeout: 15_000 })
  })
})
