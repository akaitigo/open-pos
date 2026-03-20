import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

/**
 * オフライン同期 E2E テスト (#416)
 *
 * ネットワーク切断時の UI 挙動と、復帰後のデータ整合性を検証する。
 */
test.describe('Offline Sync', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()

    // Ensure product list is loaded while online
    await expect(posPage.page.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })

  test('cart state persists through offline/online toggle', async ({ context }) => {
    // Add product to cart while online
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Go offline
    await context.setOffline(true)
    await posPage.page.waitForTimeout(500)

    // Cart should still have the item
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Go back online
    await context.setOffline(false)
    await posPage.page.waitForTimeout(1000)

    // Item should still be in the cart after reconnection
    await expect(posPage.cart).toContainText('ドリップコーヒー')
  })

  test('product list remains visible after brief offline period', async ({ context }) => {
    // Go offline
    await context.setOffline(true)
    await posPage.page.waitForTimeout(1000)

    // Go back online
    await context.setOffline(false)
    await posPage.page.waitForTimeout(1000)

    // Product list should still be visible (cached/restored)
    await expect(posPage.page.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })
})
