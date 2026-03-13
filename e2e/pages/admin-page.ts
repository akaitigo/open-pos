import { expect, type Locator, type Page } from '@playwright/test'

/**
 * 管理画面の Page Object Model
 *
 * 現行 UI に対する商品管理 smoke 操作を抽象化する。
 */
export class AdminPage {
  readonly page: Page

  readonly productsNavLink: Locator
  readonly productTable: Locator
  readonly addProductButton: Locator
  readonly productSearchInput: Locator
  readonly productNameInput: Locator
  readonly productPriceInput: Locator
  readonly productCategorySelect: Locator
  readonly productDialog: Locator
  readonly productCancelButton: Locator

  constructor(page: Page) {
    this.page = page

    this.productsNavLink = page.getByRole('link', { name: '商品管理' })

    this.productTable = page.locator('table')
    this.addProductButton = page.getByRole('button', { name: '商品を追加' })
    this.productSearchInput = page.getByPlaceholder('商品名・バーコード・SKUで検索...')

    this.productNameInput = page.getByLabel('商品名 *')
    this.productPriceInput = page.getByLabel('価格（円） *')
    this.productCategorySelect = page.locator('#category')
    this.productDialog = page.getByRole('dialog')
    this.productCancelButton = page.getByRole('button', { name: 'キャンセル' })
  }

  async goto(): Promise<void> {
    await this.page.goto('http://localhost:5174/')
    await expect(this.page.getByText('管理ダッシュボード')).toBeVisible()
  }

  async navigateToProducts(): Promise<void> {
    await this.productsNavLink.click()
    await expect(this.page.getByText('商品管理')).toBeVisible()
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

  async searchProduct(query: string): Promise<void> {
    await this.productSearchInput.fill(query)
  }

  async openFirstProductEditor(): Promise<void> {
    await this.page.locator('tbody tr').first().getByRole('button', { name: '編集' }).click()
  }

  async expectProductVisible(productName: string): Promise<void> {
    await expect(this.page.locator('tbody tr').filter({ hasText: productName })).toHaveCount(1)
  }

  async expectProductNotVisible(productName: string): Promise<void> {
    await expect(this.page.locator('tbody tr').filter({ hasText: productName })).toHaveCount(0)
  }

  async getProductRowCount(): Promise<number> {
    return this.page.locator('tbody tr').count()
  }
}
