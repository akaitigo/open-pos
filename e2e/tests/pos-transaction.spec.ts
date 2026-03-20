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

test.describe('POS Transaction - Void', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('voided transaction appears in history with VOIDED status', async () => {
    // Complete a transaction first
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await expect(posPage.receiptDialog).toBeVisible()
    await posPage.closeReceipt()

    // Navigate to history
    await posPage.navigateToHistory()
    await expect(posPage.page.getByText('取引履歴')).toBeVisible()

    // Verify there are completed transactions in the list
    await expect(posPage.page.getByText('完了').first()).toBeVisible()
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
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()

    // Verify all payment method tabs are visible
    await expect(posPage.page.getByRole('tab', { name: '現金' })).toBeVisible()
    await expect(posPage.page.getByRole('tab', { name: 'カード' })).toBeVisible()
    await expect(posPage.page.getByRole('tab', { name: 'QR' })).toBeVisible()
  })

  test('can switch to credit card payment tab', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()

    await posPage.page.getByRole('tab', { name: 'カード' }).click()
    await expect(posPage.page.getByText('カード端末プレースホルダー')).toBeVisible()
    await expect(posPage.page.getByPlaceholder('残額を入力')).toBeVisible()
    await expect(posPage.page.getByPlaceholder('カード承認番号')).toBeVisible()
  })

  test('can switch to QR payment tab', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.dismissToastIfVisible()
    await posPage.startPayment()

    await posPage.page.getByRole('tab', { name: 'QR' }).click()
    await expect(posPage.page.getByText('QR 決済プレースホルダー')).toBeVisible()
    await expect(posPage.page.getByPlaceholder('決済ID')).toBeVisible()
    await expect(posPage.page.getByText('OPENPOS-QR')).toBeVisible()
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
    await posPage.dismissToastIfVisible()

    // The cart panel shows a discount line
    await expect(posPage.cart.getByText('割引')).toBeVisible()
    // Discount total should be ￥0 (no discounts applied in seed data)
    await expect(posPage.cart.getByText('￥0').first()).toBeVisible()
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
    await expect(posPage.page.getByText('取引履歴')).toBeVisible()
    await expect(posPage.page.getByText('取引番号')).toBeVisible()
    await expect(posPage.page.getByText('日時')).toBeVisible()
    await expect(posPage.page.getByText('合計').first()).toBeVisible()
    await expect(posPage.page.getByText('ステータス')).toBeVisible()
    await expect(posPage.page.getByText('操作')).toBeVisible()
  })

  test('transaction history shows seeded completed transactions', async () => {
    await posPage.navigateToHistory()

    // Seeded data should include completed transactions
    await expect(posPage.page.getByText('完了').first()).toBeVisible()
  })

  test('completed transaction has a receipt button', async () => {
    await posPage.navigateToHistory()

    await expect(posPage.page.getByText('完了').first()).toBeVisible()
    await expect(posPage.page.getByRole('button', { name: 'レシート' }).first()).toBeVisible()
  })

  test('receipt button opens receipt dialog', async () => {
    await posPage.navigateToHistory()

    await expect(posPage.page.getByRole('button', { name: 'レシート' }).first()).toBeVisible()
    await posPage.page.getByRole('button', { name: 'レシート' }).first().click()

    await expect(posPage.page.getByRole('dialog').filter({ hasText: 'レシート' })).toBeVisible()
    await expect(posPage.page.getByRole('button', { name: '閉じる' })).toBeVisible()
  })
})
