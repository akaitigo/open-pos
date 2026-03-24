import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

test.describe('Staff PIN Authentication', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
  })

  test('displays staff selection screen on startup', async () => {
    await expect(posPage.page.getByText('スタッフを選択してください')).toBeVisible()
    await expect(posPage.page.getByText('渋谷店 オーナー')).toBeVisible()
  })

  test('selecting a staff member shows PIN input', async () => {
    await posPage.page.getByRole('button', { name: /渋谷店 オーナー/ }).click()
    await expect(posPage.page.getByText('PINを入力してください')).toBeVisible()
    await expect(posPage.page.getByTestId('pin-input')).toBeVisible()
    await expect(posPage.page.getByTestId('login-btn')).toBeVisible()
  })

  test('PIN keypad enters digits and login button is disabled until 4 digits', async () => {
    await posPage.page.getByRole('button', { name: /渋谷店 オーナー/ }).click()

    // Login button should be disabled with less than 4 digits
    await expect(posPage.page.getByTestId('login-btn')).toBeDisabled()

    await posPage.page.getByTestId('pin-key-1').click()
    await posPage.page.getByTestId('pin-key-2').click()
    await posPage.page.getByTestId('pin-key-3').click()
    await expect(posPage.page.getByTestId('login-btn')).toBeDisabled()

    // After 4 digits, login button should be enabled
    await posPage.page.getByTestId('pin-key-4').click()
    await expect(posPage.page.getByTestId('login-btn')).toBeEnabled()
  })

  test('successful login navigates to product grid', async () => {
    await posPage.login()
    await expect(posPage.productGrid).toBeVisible()
    await expect(posPage.page.getByText('ドリップコーヒー', { exact: true })).toBeVisible()
  })

  test('clear button resets PIN input', async () => {
    await posPage.page.getByRole('button', { name: /渋谷店 オーナー/ }).click()

    await posPage.page.getByTestId('pin-key-1').click()
    await posPage.page.getByTestId('pin-key-2').click()
    await posPage.page.getByTestId('pin-key-C').click()

    // After clear, login button should be disabled (no digits)
    await expect(posPage.page.getByTestId('login-btn')).toBeDisabled()
  })

  test('back button returns to staff selection', async () => {
    await posPage.page.getByRole('button', { name: /渋谷店 オーナー/ }).click()
    await expect(posPage.page.getByText('PINを入力してください')).toBeVisible()

    // Click back arrow button
    const backButton = posPage.page
      .locator('button')
      .filter({ has: posPage.page.locator('svg') })
      .first()
    await backButton.click()

    await expect(posPage.page.getByText('スタッフを選択してください')).toBeVisible()
  })
})
