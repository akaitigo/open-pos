import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

test.describe('POS Transaction', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
  })

  test('should display product list on load', async () => {
    // TODO: 商品一覧が表示されることを検証
    await expect(posPage.productList).toBeVisible()
  })

  test('should search products by name', async () => {
    // TODO: 商品名で検索し、結果がフィルタされることを検証
    await posPage.searchProduct('コーヒー')
    await expect(posPage.productList).toBeVisible()
  })

  test('should add product to cart', async () => {
    // TODO: 商品を選択してカートに追加されることを検証
    await posPage.addProductToCart('coffee')
    const count = await posPage.getCartItemCount()
    expect(count).toBe(1)
  })

  test('should update cart total when adding products', async () => {
    // TODO: 商品追加時にカート合計金額が更新されることを検証
    await posPage.addProductToCart('coffee')
    await expect(posPage.cartTotalAmount).not.toHaveText('¥0')
  })

  test('should clear cart', async () => {
    // TODO: カートクリア後に商品が0件になることを検証
    await posPage.addProductToCart('coffee')
    await posPage.clearCart()
    const count = await posPage.getCartItemCount()
    expect(count).toBe(0)
  })

  test('should complete cash payment flow', async () => {
    // TODO: 商品選択 → 支払開始 → 現金選択 → 金額入力 → 確定 → レシート表示
    await posPage.addProductToCart('coffee')
    await posPage.startPayment()
    await posPage.selectCashPayment()
    await posPage.enterPaymentAmount('1000')
    await posPage.confirmPayment()
    await expect(posPage.receiptDialog).toBeVisible()
  })

  test('should close receipt and return to product list', async () => {
    // TODO: レシート表示後に閉じて商品一覧に戻ることを検証
    await posPage.addProductToCart('coffee')
    await posPage.startPayment()
    await posPage.selectCashPayment()
    await posPage.enterPaymentAmount('1000')
    await posPage.confirmPayment()
    await expect(posPage.receiptDialog).toBeVisible()
    await posPage.closeReceipt()
    await expect(posPage.productList).toBeVisible()
  })
})
