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
    await expect(page.locator('.text-2xl.font-bold').nth(0)).toHaveText('17')
    await expect(page.locator('.text-2xl.font-bold').nth(1)).toHaveText('1')
    await expect(page.locator('.text-2xl.font-bold').nth(2)).toHaveText('2')
    await expect(page.locator('.text-2xl.font-bold').nth(3)).not.toHaveText('...')
  })

  test('products page shows seeded products and supports search', async () => {
    await adminPage.navigateToProducts()
    await expect(adminPage.productTable).toBeVisible()
    expect(await adminPage.getProductRowCount()).toBeGreaterThan(0)
    await adminPage.expectProductVisible('ドリップコーヒー')

    await adminPage.searchProduct('ドリップコーヒー')
    await adminPage.expectProductVisible('ドリップコーヒー')
    await adminPage.expectProductNotVisible('おにぎり 梅')
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
