import { expect, test } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'
import { PosPage } from '../pages/pos-page'

/**
 * クロスアプリ checkout E2E テスト (#408)
 *
 * 管理画面で商品を作成し、POS端末でその商品が表示され、
 * チェックアウトできることを検証する。
 */
test.describe('Cross-App Checkout', () => {
  const uniqueSuffix = `e2e-${Date.now()}`
  const testProductName = `テスト商品 ${uniqueSuffix}`

  test('admin creates product, POS can see and checkout it', async ({ browser }) => {
    // --- Arrange: Admin creates a new product ---
    const adminContext = await browser.newContext()
    const adminPageInstance = await adminContext.newPage()
    const adminPage = new AdminPage(adminPageInstance)

    await adminPage.goto()
    await adminPage.navigateToProducts()

    // Record product count before
    const countBefore = await adminPage.getProductRowCount()

    // Open add product dialog
    await adminPage.clickAddProduct()
    await expect(adminPage.productDialog).toBeVisible()

    // Fill in product form
    await adminPage.fillProductForm({
      name: testProductName,
      price: '500',
    })

    // Submit the form
    const submitButton = adminPageInstance.getByRole('button', { name: '保存' })
    await submitButton.click()

    // Wait for the dialog to close and product to appear in the table
    await expect(adminPage.productDialog).not.toBeVisible({ timeout: 10_000 })

    // Verify the product appears in the admin product list
    await adminPage.searchProduct(testProductName)
    await adminPage.expectProductVisible(testProductName)

    await adminContext.close()

    // --- Act: POS terminal sees and checks out the product ---
    const posContext = await browser.newContext()
    const posPageInstance = await posContext.newPage()
    const posPage = new PosPage(posPageInstance)

    await posPage.goto()
    await posPage.login()

    // Search for the newly created product on POS
    await posPage.searchProduct(testProductName)

    // The product should be visible
    await expect(
      posPageInstance.locator('.cursor-pointer').filter({ hasText: testProductName }),
    ).toBeVisible({ timeout: 10_000 })

    // Add to cart
    await posPage.addProductToCart(testProductName)
    await expect(posPage.cart).toContainText(testProductName)

    // Dismiss any toast
    await posPage.dismissToastIfVisible()

    // Complete checkout
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    // Verify receipt
    await expect(posPage.receiptDialog).toBeVisible()
    await expect(posPage.receiptDialog).toContainText('レシート')

    // Close receipt
    await posPage.closeReceipt()

    // Cart should be empty after checkout
    await expect(posPage.cart).toContainText('カートは空です')

    await posContext.close()
  })
})
