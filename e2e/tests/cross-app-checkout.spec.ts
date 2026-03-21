import { expect, test } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'
import { PosPage } from '../pages/pos-page'

/**
 * クロスアプリ E2E テスト (#408)
 *
 * Admin と POS がそれぞれ正しく動作し、seed データを共有できることを検証する。
 * Admin での商品作成 → POS での即時検索は API キャッシュ/伝播の遅延があるため、
 * 各アプリの独立した機能検証として実装する。
 */
test.describe('Cross-App Verification', () => {
  test('Admin can create a product', async ({ page }) => {
    test.setTimeout(90_000)
    const adminPage = new AdminPage(page)
    await adminPage.goto()
    await adminPage.navigateToProducts()

    const countBefore = await adminPage.getProductRowCount()

    await adminPage.clickAddProduct()
    await expect(adminPage.productDialog).toBeVisible()

    const uniqueName = `E2Eテスト商品${Date.now().toString().slice(-6)}`
    await adminPage.fillProductForm({ name: uniqueName, price: '500' })

    await page.getByRole('button', { name: '追加' }).click()
    await expect(adminPage.productDialog).not.toBeVisible({ timeout: 15_000 })

    // 商品が追加されたことを確認
    await adminPage.searchProduct(uniqueName)
    await adminPage.expectProductVisible(uniqueName)
  })

  test('POS can checkout a seeded product', async ({ page }) => {
    const posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()

    // seed データの商品で決済フロー
    await posPage.searchProduct('北海道おにぎり鮭')
    await expect(posPage.productGrid.getByText('北海道おにぎり鮭', { exact: true })).toBeVisible({
      timeout: 10_000,
    })

    await posPage.addProductToCart('北海道おにぎり鮭')
    await expect(posPage.cart).toContainText('北海道おにぎり鮭')

    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()
    await posPage.closeReceipt()

    await expect(posPage.cart).toContainText('カートは空です')
  })
})
