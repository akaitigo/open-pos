import { expect, type Locator, type Page } from '@playwright/test'

/**
 * POS 端末画面の Page Object Model
 *
 * 現行 UI に対する POS smoke 操作を抽象化する。
 */
export class PosPage {
  readonly page: Page

  readonly productList: Locator
  readonly productSearchInput: Locator
  readonly cart: Locator
  readonly cartClearButton: Locator
  readonly payButton: Locator
  readonly paymentAmountInput: Locator
  readonly paymentConfirmButton: Locator
  readonly notificationCloseButton: Locator
  readonly receiptDialog: Locator
  readonly receiptCloseButton: Locator

  constructor(page: Page) {
    this.page = page

    this.productList = page.locator('.grid .cursor-pointer')
    this.productSearchInput = page.getByPlaceholder('商品名・バーコードで検索...')

    this.cart = page.locator('section').filter({
      has: page.getByRole('heading', { name: 'カート' }),
    })
    this.cartClearButton = page.getByRole('button', { name: 'クリア' })

    this.payButton = page.getByRole('button', { name: /お会計/ })
    this.paymentAmountInput = page.getByLabel('お預かり金額（円）')
    this.paymentConfirmButton = page.getByRole('button', { name: 'お会計を確定' })
    this.notificationCloseButton = page.getByRole('button', { name: 'Close notification' }).last()

    this.receiptDialog = page.getByRole('dialog').filter({ hasText: 'レシート' })
    this.receiptCloseButton = page.getByRole('button', { name: '次のお客様へ' })
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
      await this.page.getByRole('button', { name: digit, exact: true }).click()
    }
    await this.page.getByRole('button', { name: 'ログイン' }).click()
    await expect(this.page.getByRole('link', { name: 'OpenPOS' })).toBeVisible()
  }

  async searchProduct(query: string): Promise<void> {
    await this.productSearchInput.fill(query)
  }

  async addProductToCart(productName: string): Promise<void> {
    await this.page.locator('.cursor-pointer').filter({ hasText: productName }).first().click()
  }

  async dismissToastIfVisible(): Promise<void> {
    // Wait briefly for any toast animation to appear
    await this.page.waitForTimeout(500)
    const toastContainer = this.page.locator('.fixed.right-0.top-0.z-100')
    if (await toastContainer.isVisible().catch(() => false)) {
      // Try clicking the close button, or wait for auto-dismiss
      if (await this.notificationCloseButton.isVisible().catch(() => false)) {
        await this.notificationCloseButton.click()
      }
      // Wait for toast to disappear
      await toastContainer.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
    }
  }

  async getCartItemCount(): Promise<number> {
    return this.cart.locator('input[aria-label$="の数量"]').count()
  }

  async clearCart(): Promise<void> {
    await this.cartClearButton.click()
  }

  async startPayment(): Promise<void> {
    await this.dismissToastIfVisible()
    await this.payButton.click({ force: true })
  }

  async selectExactCashPayment(): Promise<void> {
    await this.page.getByRole('button', { name: 'ぴったり' }).click({ force: true })
  }

  async enterPaymentAmount(amount: string): Promise<void> {
    await this.paymentAmountInput.fill(amount)
  }

  async confirmPayment(): Promise<void> {
    await this.paymentConfirmButton.click({ force: true })
  }

  async closeReceipt(): Promise<void> {
    await this.receiptCloseButton.click({ force: true })
  }

  async navigateToHistory(): Promise<void> {
    await this.dismissToastIfVisible()
    // force: true でtoastオーバーレイ(z-100)によるクリックブロックをバイパス
    await this.page.getByRole('link', { name: '履歴' }).click({ force: true })
    await expect(this.page.getByText('取引履歴')).toBeVisible()
  }
}
