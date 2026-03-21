import { expect, test } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'
import { PosPage } from '../pages/pos-page'

/**
 * クロスアプリ checkout E2E テスト (#408)
 *
 * Admin で商品を作成し、POS 端末で検索・チェックアウトできることを検証する。
 * Admin → POS の完全フローをテストする。
 */
test.describe('Cross-App Checkout', () => {
  test('Admin creates product then POS searches and checks out', async ({ browser }) => {
    test.slow()
    test.setTimeout(120_000)

    const uniqueSuffix = Date.now().toString().slice(-6)
    const productName = `E2Eテスト商品${uniqueSuffix}`
    const productPrice = '500'

    // --- Admin: create product ---
    const adminContext = await browser.newContext()
    const adminPageRaw = await adminContext.newPage()
    const adminPage = new AdminPage(adminPageRaw)
    await adminPage.goto()
    await adminPage.navigateToProducts()

    await adminPage.clickAddProduct()
    await adminPage.fillProductForm({ name: productName, price: productPrice })
    await adminPageRaw.getByRole('button', { name: '追加' }).click()

    // Wait for the product to appear in the table after creation
    await adminPage.expectProductVisible(productName)

    await adminContext.close()

    // --- POS: search and checkout the new product ---
    const posContext = await browser.newContext()
    const posPageRaw = await posContext.newPage()
    const posPage = new PosPage(posPageRaw)
    await posPage.goto()
    await posPage.login()

    // Search for the product and wait for API response
    const productResponsePromise = posPageRaw.waitForResponse(
      (resp) => resp.url().includes('/api/products') && resp.status() === 200,
    )
    await posPage.searchProduct(productName)
    await productResponsePromise

    await expect(posPage.productGrid.getByText(productName, { exact: true })).toBeVisible({
      timeout: 15_000,
    })

    // Add to cart
    await posPage.addProductToCart(productName)
    await expect(posPage.cart).toContainText(productName)

    // Checkout
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    // Receipt confirmation
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 15_000 })
    await posPage.closeReceipt()

    // Cart should be empty
    await expect(posPage.cart).toContainText('カートは空です')

    await posContext.close()
  })
})
