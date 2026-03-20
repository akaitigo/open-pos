import http from 'k6/http'
import { check, sleep } from 'k6'
import {
  BASE_URL,
  defaultHeaders,
  STORE_ID,
  TERMINAL_ID,
  STAFF_ID,
  standardStages,
  defaultThresholds,
} from './config.js'

export const options = {
  stages: standardStages,
  thresholds: defaultThresholds,
}

/**
 * 取引作成 API の負荷テスト
 *
 * シナリオ:
 *   1. 新規取引を作成 (DRAFT)
 *   2. 商品一覧から最初の商品を取得
 *   3. 取引に商品を追加
 *   4. 取引詳細を確認
 */
export default function () {
  // 商品一覧から1件取得
  const productsRes = http.get(`${BASE_URL}/api/products?page=1&pageSize=5`, {
    headers: defaultHeaders,
  })
  check(productsRes, {
    'products: status 200': (r) => r.status === 200,
  })

  const products = productsRes.json()
  if (!products || !products.data || products.data.length === 0) {
    console.error('No products available for transaction test')
    return
  }

  const productId = products.data[0].id

  sleep(0.3)

  // 新規取引作成
  const createPayload = JSON.stringify({
    storeId: STORE_ID,
    terminalId: TERMINAL_ID,
    staffId: STAFF_ID,
    type: 'SALE',
    clientId: `k6-load-${Date.now()}-${__VU}-${__ITER}`,
  })

  const createRes = http.post(`${BASE_URL}/api/transactions`, createPayload, {
    headers: defaultHeaders,
  })
  check(createRes, {
    'transaction create: status 2xx': (r) => r.status >= 200 && r.status < 300,
    'transaction create: has id': (r) => {
      const body = r.json()
      return body && body.id
    },
    'transaction create: status DRAFT': (r) => {
      const body = r.json()
      return body && body.status === 'DRAFT'
    },
  })

  if (createRes.status >= 300) {
    return
  }

  const transactionId = createRes.json().id

  sleep(0.3)

  // 取引に商品を追加
  const addItemPayload = JSON.stringify({
    productId: productId,
    quantity: 1,
  })

  const addItemRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/items`,
    addItemPayload,
    { headers: defaultHeaders },
  )
  check(addItemRes, {
    'add item: status 2xx': (r) => r.status >= 200 && r.status < 300,
    'add item: has items': (r) => {
      const body = r.json()
      return body && body.items && body.items.length > 0
    },
  })

  sleep(0.3)

  // 取引詳細を確認
  const detailRes = http.get(`${BASE_URL}/api/transactions/${transactionId}`, {
    headers: defaultHeaders,
  })
  check(detailRes, {
    'transaction detail: status 200': (r) => r.status === 200,
    'transaction detail: matches id': (r) => {
      const body = r.json()
      return body && body.id === transactionId
    },
  })

  sleep(0.5)
}
