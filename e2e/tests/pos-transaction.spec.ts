import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

test.describe('POS Smoke', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('shows seeded product list after login', async () => {
    await expect(posPage.page.getByText('ドリップコーヒー')).toBeVisible()
  })

  test('filters products by search term', async () => {
    await posPage.searchProduct('ドリップコーヒー')
    await expect(posPage.page.getByText('ドリップコーヒー')).toBeVisible()
    await expect(posPage.page.getByText('北海道おにぎり鮭')).not.toBeVisible()
  })

  test('adds a seeded product to cart', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')
    await expect(posPage.cart).toContainText('￥188')
  })

  test('completes a cash checkout flow', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await expect(posPage.receiptDialog).toBeVisible()
    await expect(posPage.receiptDialog).toContainText('レシート')
    await posPage.closeReceipt()
    await expect(posPage.page.getByText('ドリップコーヒー')).toBeVisible()
    await expect(posPage.cart).toContainText('カートは空です')
  })
})
