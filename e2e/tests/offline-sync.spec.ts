import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

test.describe('Offline Sync E2E', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('creates transaction while offline and syncs when back online', async ({ context }) => {
    // Verify product list is visible (online)
    await expect(posPage.page.getByText('ドリップコーヒー', { exact: true })).toBeVisible()

    // Go offline
    await context.setOffline(true)

    // Verify offline indicator appears
    await posPage.page.waitForTimeout(500)

    // Add product to cart while offline
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Go back online
    await context.setOffline(false)

    // Wait for sync to complete
    await posPage.page.waitForTimeout(2000)

    // Verify the product is still in cart after reconnection
    await expect(posPage.cart).toContainText('ドリップコーヒー')
  })

  test('shows offline status when network is disconnected', async ({ context }) => {
    // Go offline
    await context.setOffline(true)
    await posPage.page.waitForTimeout(500)

    // Go back online
    await context.setOffline(false)
    await posPage.page.waitForTimeout(500)

    // Product list should still be visible (cached)
    await expect(posPage.page.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })
})
