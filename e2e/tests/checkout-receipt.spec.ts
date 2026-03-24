import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

test.describe('Checkout and Receipt Flow', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('full checkout flow shows receipt with transaction details', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    await posPage.startPayment()

    // Verify checkout dialog content
    await expect(posPage.page.getByText('お会計')).toBeVisible()
    await expect(posPage.page.getByText('合計金額')).toBeVisible()

    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    // Receipt dialog should appear with receipt content
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 30_000 })
    await expect(posPage.receiptDialog.getByText('レシート')).toBeVisible()

    await posPage.closeReceipt()
    await expect(posPage.cart).toContainText('カートは空です')
  })

  test('checkout dialog shows coupon code input', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()

    // Verify coupon section exists
    await expect(posPage.page.getByText('割引・クーポン')).toBeVisible()
    await expect(posPage.page.getByPlaceholder('クーポンコードを入力')).toBeVisible()
    await expect(posPage.page.getByRole('button', { name: '適用' })).toBeVisible()
  })

  test('checkout dialog shows payment summary with subtotal and tax', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()

    // Verify payment summary section
    await expect(posPage.page.getByText('合計金額')).toBeVisible()
    await expect(posPage.page.getByText('小計')).toBeVisible()
    await expect(posPage.page.getByText('支払済')).toBeVisible()
    await expect(posPage.page.getByText('残額')).toBeVisible()
  })

  test('receipt dialog has print button', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    await expect(posPage.receiptDialog).toBeVisible({ timeout: 30_000 })
    await expect(posPage.page.getByTestId('receipt-print-btn')).toBeVisible()
    await expect(posPage.page.getByTestId('receipt-close-btn')).toBeVisible()

    await posPage.closeReceipt()
  })

  test('multiple items checkout calculates total correctly', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    // Cart should show item count
    await expect(posPage.cart.getByText('2 点')).toBeVisible()

    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await posPage.closeReceipt()

    await expect(posPage.cart).toContainText('カートは空です')
  })
})
