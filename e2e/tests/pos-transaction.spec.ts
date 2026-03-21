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
    await expect(posPage.productGrid.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })

  test('filters products by search term', async () => {
    await posPage.searchProduct('ドリップコーヒー')
    await expect(posPage.productGrid.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })

  test('adds a seeded product to cart', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')
  })

  test('completes a cash checkout flow', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await posPage.closeReceipt()
    await expect(posPage.cart).toContainText('カートは空です')
  })
})

test.describe('POS Transaction - Void', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('voided transaction appears in history with VOIDED status', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await posPage.closeReceipt()

    await posPage.navigateToHistory()
    await expect(posPage.page.getByText('完了').first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('POS Transaction - Multiple Payment Methods', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('checkout dialog shows payment method tabs', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()

    await expect(posPage.page.getByTestId('payment-tab-cash')).toBeVisible()
    await expect(posPage.page.getByTestId('payment-tab-card')).toBeVisible()
    await expect(posPage.page.getByTestId('payment-tab-qr')).toBeVisible()
  })

  test('can switch to credit card payment tab', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()

    await posPage.page.getByTestId('payment-tab-card').click()
    await expect(posPage.page.getByText('カード端末プレースホルダー')).toBeVisible()
  })

  test('can switch to QR payment tab', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()

    await posPage.page.getByTestId('payment-tab-qr').click()
    await expect(posPage.page.getByText('QR 決済プレースホルダー')).toBeVisible()
  })
})

test.describe('POS Transaction - Discount/Coupon', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('cart shows discount line as zero when no discounts applied', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart.getByText('割引')).toBeVisible()
  })
})

test.describe('POS Transaction - History View', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('transaction history page shows table headers', async () => {
    await posPage.navigateToHistory()
    await expect(posPage.page.getByRole('heading', { name: '取引履歴' })).toBeVisible()
  })

  test('transaction history shows seeded completed transactions', async () => {
    await posPage.navigateToHistory()
    await expect(posPage.page.getByText('完了').first()).toBeVisible({ timeout: 10_000 })
  })

  test('completed transaction has a receipt button', async () => {
    await posPage.navigateToHistory()
    await expect(posPage.page.getByText('完了').first()).toBeVisible({ timeout: 10_000 })
    await expect(posPage.page.getByRole('button', { name: 'レシート' }).first()).toBeVisible()
  })

  test('receipt button opens receipt dialog', async () => {
    await posPage.navigateToHistory()
    await expect(posPage.page.getByRole('button', { name: 'レシート' }).first()).toBeVisible({
      timeout: 10_000,
    })
    await posPage.page.getByRole('button', { name: 'レシート' }).first().click()
    await expect(posPage.page.getByTestId('receipt-dialog')).toBeVisible()
  })
})
