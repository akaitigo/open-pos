import { expect, test } from '@playwright/test'
import { PosPage } from '../pages/pos-page'

test.describe('Cart Operations', () => {
  let posPage: PosPage

  test.beforeEach(async ({ page }) => {
    posPage = new PosPage(page)
    await posPage.goto()
    await posPage.login()
  })

  test('adding a product shows it in cart with quantity 1', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')
    await expect(posPage.cart.getByText('1 点')).toBeVisible()
  })

  test('increasing item quantity updates the cart', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await posPage.increaseItemQuantity('ドリップコーヒー')

    // After increment, cart should show 2 items
    await expect(posPage.cart.getByText('2 点')).toBeVisible()
  })

  test('decreasing item quantity to zero removes it from cart', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    await posPage.decreaseItemQuantity('ドリップコーヒー')
    await expect(posPage.cart).toContainText('カートは空です')
  })

  test('remove button deletes item from cart', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    await posPage.removeItemFromCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('カートは空です')
  })

  test('clear cart removes all items', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.cart).toContainText('ドリップコーヒー')

    await posPage.clearCart()
    await expect(posPage.cart).toContainText('カートは空です')
  })

  test('cart shows subtotal and tax breakdown', async () => {
    await posPage.addProductToCart('ドリップコーヒー')

    await expect(posPage.cart.getByText('小計')).toBeVisible()
    await expect(posPage.cart.getByText('合計（税込）')).toBeVisible()
    await expect(posPage.cart.getByText('税率別内訳')).toBeVisible()
  })

  test('checkout button is disabled when cart is empty', async () => {
    await expect(posPage.page.getByTestId('checkout-button')).toBeDisabled()
  })

  test('checkout button is enabled when cart has items', async () => {
    await posPage.addProductToCart('ドリップコーヒー')
    await expect(posPage.page.getByTestId('checkout-button')).toBeEnabled()
  })

  test('adding multiple different products shows them all in cart', async () => {
    await posPage.addProductToCart('ドリップコーヒー')

    // Search for another product
    await posPage.searchProduct('北海道おにぎり鮭')
    await expect(posPage.productGrid.getByText('北海道おにぎり鮭', { exact: true })).toBeVisible({
      timeout: 10_000,
    })
    await posPage.addProductToCart('北海道おにぎり鮭')

    await expect(posPage.cart).toContainText('ドリップコーヒー')
    await expect(posPage.cart).toContainText('北海道おにぎり鮭')
    await expect(posPage.cart.getByText('2 点')).toBeVisible()
  })

  test('cart shows per-item unit price and subtotal', async () => {
    await posPage.addProductToCart('ドリップコーヒー')

    // Each item should display unit price
    await expect(posPage.cart.getByText('単価')).toBeVisible()
  })
})
