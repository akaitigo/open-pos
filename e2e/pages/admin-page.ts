import { type Locator, type Page } from '@playwright/test'

/**
 * 管理画面の Page Object Model
 *
 * 商品 CRUD 操作を抽象化する。
 * セレクタは全て data-testid ベース。
 */
export class AdminPage {
  readonly page: Page

  // ナビゲーション
  readonly productsNavLink: Locator

  // 商品一覧
  readonly productTable: Locator
  readonly addProductButton: Locator
  readonly productSearchInput: Locator

  // 商品フォーム
  readonly productNameInput: Locator
  readonly productPriceInput: Locator
  readonly productCategorySelect: Locator
  readonly productSaveButton: Locator
  readonly productCancelButton: Locator

  // 削除確認
  readonly deleteConfirmButton: Locator

  // 通知
  readonly successNotification: Locator
  readonly errorNotification: Locator

  constructor(page: Page) {
    this.page = page

    this.productsNavLink = page.getByTestId('nav-products')

    this.productTable = page.getByTestId('product-table')
    this.addProductButton = page.getByTestId('add-product-button')
    this.productSearchInput = page.getByTestId('product-search-input')

    this.productNameInput = page.getByTestId('product-name-input')
    this.productPriceInput = page.getByTestId('product-price-input')
    this.productCategorySelect = page.getByTestId('product-category-select')
    this.productSaveButton = page.getByTestId('product-save-button')
    this.productCancelButton = page.getByTestId('product-cancel-button')

    this.deleteConfirmButton = page.getByTestId('delete-confirm-button')

    this.successNotification = page.getByTestId('notification-success')
    this.errorNotification = page.getByTestId('notification-error')
  }

  async goto(): Promise<void> {
    // admin-dashboard は port 5174
    await this.page.goto('http://localhost:5174/')
  }

  async navigateToProducts(): Promise<void> {
    await this.productsNavLink.click()
  }

  async clickAddProduct(): Promise<void> {
    await this.addProductButton.click()
  }

  async fillProductForm(params: { name: string; price: string; category?: string }): Promise<void> {
    await this.productNameInput.fill(params.name)
    await this.productPriceInput.fill(params.price)
    if (params.category) {
      await this.productCategorySelect.selectOption(params.category)
    }
  }

  async saveProduct(): Promise<void> {
    await this.productSaveButton.click()
  }

  async searchProduct(query: string): Promise<void> {
    await this.productSearchInput.fill(query)
  }

  async clickEditProduct(productName: string): Promise<void> {
    await this.productTable.getByTestId(`edit-product-${productName}`).click()
  }

  async clickDeleteProduct(productName: string): Promise<void> {
    await this.productTable.getByTestId(`delete-product-${productName}`).click()
  }

  async confirmDelete(): Promise<void> {
    await this.deleteConfirmButton.click()
  }

  async getProductRowCount(): Promise<number> {
    return this.productTable.getByTestId('product-row').count()
  }
}
