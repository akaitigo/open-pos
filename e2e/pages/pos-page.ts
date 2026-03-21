import { expect, type Locator, type Page } from '@playwright/test'

/**
 * POS 端末画面の Page Object Model
 *
 * Playwright ベストプラクティスに準拠:
 * - data-testid / getByRole ベースのロケータ（CSS クラス依存を排除）
 * - CSS 注入ハックなし（アプリ側で pointer-events: none 設定済み）
 * - Hard timeout なし（条件ベースの待機のみ）
 */
export class PosPage {
  readonly page: Page

  readonly productGrid: Locator
  readonly productSearchInput: Locator
  readonly cart: Locator
  readonly cartClearButton: Locator
  readonly payButton: Locator
  readonly paymentConfirmButton: Locator
  readonly receiptDialog: Locator
  readonly receiptCloseButton: Locator

  constructor(page: Page) {
    this.page = page

    this.productGrid = page.getByTestId('product-grid')
    this.productSearchInput = page.getByTestId('product-search-input')

    this.cart = page.locator('section').filter({
      has: page.getByRole('heading', { name: 'カート' }),
    })
    this.cartClearButton = page.getByRole('button', { name: 'クリア' })

    this.payButton = page.getByRole('button', { name: /お会計/ })
    this.paymentConfirmButton = page.getByTestId('checkout-confirm-btn')

    this.receiptDialog = page.getByTestId('receipt-dialog')
    this.receiptCloseButton = page.getByTestId('receipt-close-btn')
  }

  async goto(): Promise<void> {
    await this.page.goto('http://localhost:5173/')
    await expect(this.page.getByText('OpenPOS Terminal')).toBeVisible()
  }

  async login(staffName = '渋谷店 オーナー', pin = '1234'): Promise<void> {
    const staffButton = this.page.getByRole('button', { name: new RegExp(staffName) })
    await expect(staffButton).toBeVisible()
    await staffButton.click()
    for (const digit of pin.split('')) {
      await this.page.getByTestId(`pin-key-${digit}`).click()
    }
    await this.page.getByTestId('login-btn').click()
    await expect(this.productGrid).toBeVisible({ timeout: 15_000 })
  }

  async searchProduct(query: string): Promise<void> {
    await this.productSearchInput.fill(query)
  }

  async addProductToCart(productName: string): Promise<void> {
    const card = this.productGrid
      .locator(`[data-testid^="product-card-"]`)
      .filter({ hasText: productName })
      .first()
    await card.click()
  }

  async getCartItemCount(): Promise<number> {
    return this.cart.locator('input[aria-label$="の数量"]').count()
  }

  async clearCart(): Promise<void> {
    await this.cartClearButton.click()
  }

  async startPayment(): Promise<void> {
    await this.payButton.click()
  }

  async selectExactCashPayment(): Promise<void> {
    await this.page.getByTestId('checkout-exact-btn').click()
  }

  async confirmPayment(): Promise<void> {
    await this.paymentConfirmButton.click()
  }

  async closeReceipt(): Promise<void> {
    await expect(this.receiptDialog).toBeVisible({ timeout: 30_000 })
    await this.receiptCloseButton.click()
  }

  async navigateToHistory(): Promise<void> {
    await this.page.getByRole('link', { name: '履歴' }).click()
    await expect(this.page.getByTestId('history-table')).toBeVisible({ timeout: 15_000 })
  }
}
