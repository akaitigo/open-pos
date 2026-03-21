import { expect, type Locator, type Page } from '@playwright/test'

/**
 * 管理画面の Page Object Model
 *
 * Playwright ベストプラクティスに準拠:
 * - data-testid / getByRole ベースのロケータ
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

    this.productTable = page.getByTestId('product-table')
    this.addProductButton = page.getByTestId('add-product-button')
    this.productSearchInput = page.getByTestId('product-search-input')

    this.productNameInput = page.getByLabel('商品名 *')
    this.productPriceInput = page.getByLabel('価格（円） *')
    this.productCategorySelect = page.locator('#category')
    this.productDialog = page.getByTestId('product-edit-dialog')
    this.productCancelButton = page.getByRole('button', { name: 'キャンセル' })
  }

  async goto(): Promise<void> {
    await this.page.goto('http://localhost:5174/')
    await expect(this.page.getByRole('heading', { name: 'ダッシュボード' })).toBeVisible({
      timeout: 15_000,
    })
  }

  async navigateToProducts(): Promise<void> {
    await this.productsNavLink.click()
    await expect(this.productTable).toBeVisible({ timeout: 15_000 })
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
    await this.productTable
      .locator('tbody tr')
      .first()
      .getByRole('button', { name: '編集' })
      .click()
  }

  async expectProductVisible(productName: string): Promise<void> {
    await expect(
      this.productTable.locator('tbody tr').filter({ hasText: productName }),
    ).toHaveCount(1, { timeout: 10_000 })
  }

  async expectProductNotVisible(productName: string): Promise<void> {
    await expect(
      this.productTable.locator('tbody tr').filter({ hasText: productName }),
    ).toHaveCount(0, { timeout: 10_000 })
  }

  async getProductRowCount(): Promise<number> {
    return this.productTable.locator('tbody tr').count()
  }
}
