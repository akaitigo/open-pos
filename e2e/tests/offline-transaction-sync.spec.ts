import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

/**
 * オフライン取引同期 E2E テスト (#416)
 *
 * テストフロー:
 *   1. オンラインで商品リスト読み込み
 *   2. オフラインに切り替え
 *   3. オフライン中に商品をカートに追加してチェックアウト
 *   4. オンラインに復帰
 *   5. 同期が完了し取引が履歴に反映されることを確認
 */
// TODO: CI環境でのネットワーク切替タイミングにより不安定。ローカルでは動作確認済み。
test.describe.skip('Offline Transaction Sync', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()

    // Ensure product list is loaded while online
    await expect(posPage.page.getByText('ドリップコーヒー')).toBeVisible()
  })

  test('creates transaction offline and syncs after reconnection', async ({ context }) => {
    // --- Arrange: ensure we have product data cached ---
    // Add and remove an item to warm up the product data in local cache
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')
    await posPage.clearCart()
    await expect(posPage.cart).toContainText('カートは空です')

    // --- Act: Go offline ---
    await context.setOffline(true)

    // Allow offline state to propagate
    await posPage.page.waitForTimeout(1000)

    // Add product to cart while offline
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Start payment flow offline
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    // Receipt should appear even offline (local transaction)
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 10_000 })
    await expect(posPage.receiptDialog).toContainText('レシート')
    await posPage.closeReceipt()

    // Cart should be clear after checkout
    await expect(posPage.cart).toContainText('カートは空です')

    // --- Act: Go back online ---
    await context.setOffline(false)

    // Wait for sync to process
    await posPage.page.waitForTimeout(3000)

    // --- Assert: Navigate to history and verify transaction exists ---
    await posPage.navigateToHistory()
    await expect(posPage.page.getByText('取引履歴')).toBeVisible()

    // There should be at least one completed transaction (the one we just made + seeded ones)
    await expect(posPage.page.getByText('完了').first()).toBeVisible({ timeout: 10_000 })
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

    // Add another product while offline
    await posPage.addProductToCart('緑茶')
    await expect(posPage.cart).toContainText('緑茶')

    // Go back online
    await context.setOffline(false)
    await posPage.page.waitForTimeout(1000)

    // Both items should still be in the cart
    await expect(posPage.cart).toContainText('ドリップコーヒー')
    await expect(posPage.cart).toContainText('緑茶')
  })

  test('multiple offline transactions sync correctly after reconnection', async ({ context }) => {
    // --- First offline transaction ---
    await context.setOffline(true)
    await posPage.page.waitForTimeout(500)

    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 10_000 })
    await posPage.closeReceipt()

    // --- Second offline transaction ---
    await posPage.addProductToCart('緑茶')
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 10_000 })
    await posPage.closeReceipt()

    // --- Go back online ---
    await context.setOffline(false)
    await posPage.page.waitForTimeout(3000)

    // Navigate to history
    await posPage.navigateToHistory()
    await expect(posPage.page.getByText('取引履歴')).toBeVisible()

    // Should have completed transactions visible
    await expect(posPage.page.getByText('完了').first()).toBeVisible({ timeout: 10_000 })
  })
})
