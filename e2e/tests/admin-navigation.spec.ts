import { expect, test } from '@playwright/test'
import { AdminPage } from '../pages/admin-page'

test.describe('Admin Dashboard - Full Navigation', () => {
  let adminPage: AdminPage

  test.beforeEach(async ({ page }) => {
    adminPage = new AdminPage(page)
    await adminPage.goto()
  })

  test('dashboard page displays summary cards', async () => {
    await expect(adminPage.page.getByRole('heading', { name: 'ダッシュボード' })).toBeVisible()
    await expect(adminPage.page.getByTestId('summary-card-products')).toBeVisible()
    await expect(adminPage.page.getByTestId('summary-card-stores')).toBeVisible()
    await expect(adminPage.page.getByTestId('summary-card-staff')).toBeVisible()
    await expect(adminPage.page.getByTestId('summary-card-transactions')).toBeVisible()
  })

  test('can navigate to categories page', async () => {
    await adminPage.navigateToCategories()
    await expect(adminPage.page.getByRole('heading', { name: 'カテゴリ管理' })).toBeVisible()

    // Categories page should show table or tree view toggle
    await expect(adminPage.page.getByRole('button', { name: 'テーブル' })).toBeVisible()
    await expect(adminPage.page.getByRole('button', { name: 'ツリー' })).toBeVisible()
  })

  test('can navigate to stores page and see seeded stores', async () => {
    await adminPage.navigateToStores()
    await expect(adminPage.page.getByRole('heading', { name: '店舗管理' })).toBeVisible()

    // Stores table should have header columns
    await expect(adminPage.page.getByRole('columnheader', { name: '店舗名' })).toBeVisible()
    await expect(adminPage.page.getByRole('columnheader', { name: 'ステータス' })).toBeVisible()
  })

  test('can navigate to staff page', async () => {
    await adminPage.navigateToStaff()
    await expect(adminPage.page.getByRole('heading', { name: 'スタッフ管理' })).toBeVisible()

    // Staff page should show store selector
    await expect(adminPage.page.getByText('店舗:')).toBeVisible()
  })

  test('can navigate to inventory page', async () => {
    await adminPage.navigateToInventory()
    await expect(adminPage.page.getByRole('heading', { name: '在庫管理' })).toBeVisible()

    // Inventory page should show store ID input and low stock filter
    await expect(adminPage.page.getByPlaceholder('店舗IDを入力...')).toBeVisible()
    await expect(adminPage.page.getByRole('button', { name: '在庫低下のみ表示' })).toBeVisible()
  })

  test('can navigate to activity logs page', async () => {
    await adminPage.navigateToActivityLogs()
    await expect(adminPage.page.getByRole('heading', { name: '操作履歴' })).toBeVisible()
  })

  test('can navigate to settings page', async () => {
    await adminPage.navigateToSettings()
    await expect(adminPage.page.getByRole('heading', { name: '設定' })).toBeVisible()
  })

  test('sidebar shows all navigation items', async () => {
    const navItems = [
      'ダッシュボード',
      '商品管理',
      'カテゴリ管理',
      '在庫管理',
      '発注管理',
      '店舗管理',
      'スタッフ管理',
      '操作履歴',
      '設定',
    ]

    for (const item of navItems) {
      await expect(adminPage.page.getByRole('link', { name: item })).toBeVisible()
    }
  })
})

test.describe('Admin Dashboard - Category Operations', () => {
  let adminPage: AdminPage

  test.beforeEach(async ({ page }) => {
    adminPage = new AdminPage(page)
    await adminPage.goto()
    await adminPage.navigateToCategories()
  })

  test('category page shows view toggle buttons', async () => {
    // Verify both view toggle buttons exist
    await expect(adminPage.page.getByRole('button', { name: /テーブル/ })).toBeVisible()
    await expect(adminPage.page.getByRole('button', { name: /ツリー/ })).toBeVisible()
  })

  test('add category button opens form dialog', async () => {
    await adminPage.page.getByRole('button', { name: 'カテゴリを追加' }).click()
    await expect(adminPage.page.getByRole('heading', { name: 'カテゴリを追加' })).toBeVisible()
    await expect(adminPage.page.getByLabel('カテゴリ名 *')).toBeVisible()

    // Close dialog
    await adminPage.page.getByRole('button', { name: 'キャンセル' }).click()
  })
})

test.describe('Admin Dashboard - Store Operations', () => {
  let adminPage: AdminPage

  test.beforeEach(async ({ page }) => {
    adminPage = new AdminPage(page)
    await adminPage.goto()
    await adminPage.navigateToStores()
  })

  test('add store button opens form dialog', async () => {
    await adminPage.page.getByRole('button', { name: '店舗を追加' }).click()
    await expect(adminPage.page.getByRole('heading', { name: '店舗を追加' })).toBeVisible()
    await expect(adminPage.page.getByLabel('店舗名 *')).toBeVisible()

    // Close dialog
    await adminPage.page.getByRole('button', { name: 'キャンセル' }).click()
  })
})
