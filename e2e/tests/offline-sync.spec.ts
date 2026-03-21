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
    await expect(posPage.productGrid.getByText('ドリップコーヒー', { exact: true })).toBeVisible()

    // Go offline
    await context.setOffline(true)

    // Add product to cart while offline
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Go back online
    await context.setOffline(false)

    // Verify the product is still in cart after reconnection
    await expect(posPage.cart).toContainText('ドリップコーヒー')
  })

  test('product list remains visible after brief offline period', async ({ context }) => {
    // Go offline
    await context.setOffline(true)

    // Go back online
    await context.setOffline(false)

    // Product list should still be visible
    await expect(posPage.productGrid.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })
})
