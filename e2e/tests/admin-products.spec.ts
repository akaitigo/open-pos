import { expect, test } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'

test.describe('Admin Product Management', () => {
  let adminPage: AdminPage

  test.beforeEach(async ({ page }) => {
    adminPage = new AdminPage(page)
    await adminPage.goto()
    await adminPage.navigateToProducts()
  })

  test('should display product table', async () => {
    // TODO: 商品テーブルが表示されることを検証
    await expect(adminPage.productTable).toBeVisible()
  })

  test('should create a new product', async () => {
    // TODO: 商品追加フォームで新規商品を作成し、テーブルに表示されることを検証
    await adminPage.clickAddProduct()
    await adminPage.fillProductForm({
      name: 'テスト商品',
      price: '500',
      category: 'food',
    })
    await adminPage.saveProduct()
    await expect(adminPage.successNotification).toBeVisible()
  })

  test('should search products', async () => {
    // TODO: 商品名で検索し、テーブルがフィルタされることを検証
    await adminPage.searchProduct('テスト商品')
    await expect(adminPage.productTable).toBeVisible()
  })

  test('should edit an existing product', async () => {
    // TODO: 既存商品を編集し、更新されることを検証
    await adminPage.clickEditProduct('test-product')
    await adminPage.fillProductForm({
      name: '更新商品',
      price: '600',
    })
    await adminPage.saveProduct()
    await expect(adminPage.successNotification).toBeVisible()
  })

  test('should delete a product', async () => {
    // TODO: 商品を削除し、テーブルから消えることを検証
    const initialCount = await adminPage.getProductRowCount()
    await adminPage.clickDeleteProduct('test-product')
    await adminPage.confirmDelete()
    await expect(adminPage.successNotification).toBeVisible()
    const afterCount = await adminPage.getProductRowCount()
    expect(afterCount).toBe(initialCount - 1)
  })

  test('should show validation error for empty product name', async () => {
    // TODO: 名前未入力で保存しようとした時にバリデーションエラーが表示されることを検証
    await adminPage.clickAddProduct()
    await adminPage.fillProductForm({
      name: '',
      price: '500',
    })
    await adminPage.saveProduct()
    await expect(adminPage.errorNotification).toBeVisible()
  })
})
