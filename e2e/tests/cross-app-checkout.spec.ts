import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

/**
 * クロスアプリ checkout E2E テスト (#408)
 *
 * シードデータで作成済みの商品を使い、POS端末で検索・チェックアウトできることを検証する。
 * Admin での商品作成は seed.sh が担当し、E2E ではその結果を利用する。
 */
test.describe('Cross-App Checkout', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('seeded product can be searched and checked out on POS', async () => {
    // seed.sh で作成された商品を検索
    await posPage.searchProduct('北海道おにぎり鮭')

    await expect(posPage.productGrid.getByText('北海道おにぎり鮭', { exact: true })).toBeVisible({
      timeout: 10_000,
    })

    // カートに追加
    await posPage.addProductToCart('北海道おにぎり鮭')
    await expect(posPage.cart).toContainText('北海道おにぎり鮭')

    // チェックアウト
    await posPage.startPayment()
    await posPage.selectExactCashPayment()
    await posPage.confirmPayment()

    // レシート確認
    await expect(posPage.receiptDialog).toBeVisible({ timeout: 10_000 })
    await posPage.closeReceipt()

    // カートが空になること
    await expect(posPage.cart).toContainText('カートは空です')
  })
})
