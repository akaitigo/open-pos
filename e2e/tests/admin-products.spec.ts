import { expect, test } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'

test.describe('Admin Smoke', () => {
  let adminPage: AdminPage

  test.beforeEach(async ({ page }) => {
    adminPage = new AdminPage(page)
    await adminPage.goto()
  })

  test('dashboard shows seeded summary counts', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'ダッシュボード' })).toBeVisible()

    const productCard = page.getByTestId('summary-card-products')
    const storeCard = page.getByTestId('summary-card-stores')
    const staffCard = page.getByTestId('summary-card-staff')
    const txCard = page.getByTestId('summary-card-transactions')

    await expect(productCard.locator('.text-2xl')).toHaveText('40', { timeout: 15_000 })
    await expect(storeCard.locator('.text-2xl')).toHaveText('2', { timeout: 15_000 })
    await expect(staffCard.locator('.text-2xl')).toHaveText('3', { timeout: 15_000 })
    await expect
      .poll(
        async () => {
          const text = await txCard.locator('.text-2xl').textContent()
          return Number(text?.trim() ?? '0')
        },
        { timeout: 15_000 },
      )
      .toBeGreaterThanOrEqual(10)
  })

  test('products page shows seeded products and supports search', async () => {
    await adminPage.navigateToProducts()
    await expect(adminPage.productTable).toBeVisible()
    expect(await adminPage.getProductRowCount()).toBeGreaterThan(0)
    await adminPage.expectProductVisible('ドリップコーヒー')

    await adminPage.searchProduct('ドリップコーヒー')
    await adminPage.expectProductVisible('ドリップコーヒー')
    await adminPage.expectProductNotVisible('北海道おにぎり鮭')
  })

  test('products page opens edit dialog for seeded product', async () => {
    await adminPage.navigateToProducts()
    await adminPage.openFirstProductEditor()
    await expect(adminPage.productDialog).toContainText('商品を編集')
    await expect(adminPage.productNameInput).not.toHaveValue('')
    await adminPage.productCancelButton.click()
    await expect(adminPage.productDialog).not.toBeVisible()
  })
})
