import { type Locator, type Page } from '@playwright/test'

/**
 * POS 端末画面の Page Object Model
 *
 * 商品選択、カート操作、支払、レシート表示を抽象化する。
 * セレクタは全て data-testid ベース。
 */
export class PosPage {
  readonly page: Page

  // 商品一覧
  readonly productList: Locator
  readonly productSearchInput: Locator

  // カート
  readonly cartItems: Locator
  readonly cartTotalAmount: Locator
  readonly cartClearButton: Locator

  // 支払
  readonly payButton: Locator
  readonly paymentMethodCash: Locator
  readonly paymentAmountInput: Locator
  readonly paymentConfirmButton: Locator

  // レシート
  readonly receiptDialog: Locator
  readonly receiptCloseButton: Locator

  constructor(page: Page) {
    this.page = page

    this.productList = page.getByTestId('product-list')
    this.productSearchInput = page.getByTestId('product-search-input')

    this.cartItems = page.getByTestId('cart-items')
    this.cartTotalAmount = page.getByTestId('cart-total-amount')
    this.cartClearButton = page.getByTestId('cart-clear-button')

    this.payButton = page.getByTestId('pay-button')
    this.paymentMethodCash = page.getByTestId('payment-method-cash')
    this.paymentAmountInput = page.getByTestId('payment-amount-input')
    this.paymentConfirmButton = page.getByTestId('payment-confirm-button')

    this.receiptDialog = page.getByTestId('receipt-dialog')
    this.receiptCloseButton = page.getByTestId('receipt-close-button')
  }

  async goto(): Promise<void> {
    await this.page.goto('/')
  }

  async searchProduct(query: string): Promise<void> {
    await this.productSearchInput.fill(query)
  }

  async addProductToCart(productName: string): Promise<void> {
    await this.productList.getByTestId(`product-item-${productName}`).click()
  }

  async getCartItemCount(): Promise<number> {
    return this.cartItems.getByTestId('cart-item').count()
  }

  async clearCart(): Promise<void> {
    await this.cartClearButton.click()
  }

  async startPayment(): Promise<void> {
    await this.payButton.click()
  }

  async selectCashPayment(): Promise<void> {
    await this.paymentMethodCash.click()
  }

  async enterPaymentAmount(amount: string): Promise<void> {
    await this.paymentAmountInput.fill(amount)
  }

  async confirmPayment(): Promise<void> {
    await this.paymentConfirmButton.click()
  }

  async closeReceipt(): Promise<void> {
    await this.receiptCloseButton.click()
  }
}
