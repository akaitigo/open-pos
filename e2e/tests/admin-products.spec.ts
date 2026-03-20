import { expect, test, type Page } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'

function summaryCardValue(page: Page, title: string) {
  return page
    .getByText(title, { exact: true })
    .locator('xpath=ancestor::div[contains(@class,"rounded-xl")]')
    .locator('.text-2xl.font-bold')
    .first()
}

async function summaryCardNumber(page: Page, title: string) {
  const value = await summaryCardValue(page, title).textContent()
  return Number(value?.trim() ?? '0')
}

test.describe('Admin Smoke', () => {
  let adminPage: AdminPage

  test.beforeEach(async ({ page }) => {
    adminPage = new AdminPage(page)
    await adminPage.goto()
  })

  test('dashboard shows seeded summary counts', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'ダッシュボード' })).toBeVisible()
    await expect(summaryCardValue(page, '商品数')).toHaveText('40')
    await expect(summaryCardValue(page, '店舗数')).toHaveText('2')
    await expect(summaryCardValue(page, 'スタッフ数')).toHaveText('3')
    await expect.poll(() => summaryCardNumber(page, '取引数')).toBeGreaterThanOrEqual(10)
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
