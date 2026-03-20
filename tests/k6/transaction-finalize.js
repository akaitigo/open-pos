import http from 'k6/http'
import { check, sleep } from 'k6'
import {
  BASE_URL,
  defaultHeaders,
  STORE_ID,
  TERMINAL_ID,
  STAFF_ID,
  smokeStages,
  defaultThresholds,
} from './config.js'

export const options = {
  // Use lighter smoke profile for finalization since it has write-side effects
  stages: smokeStages,
  thresholds: defaultThresholds,
}

/**
 * 取引ファイナライズ（決済確定） API の負荷テスト
 *
 * シナリオ:
 *   1. 新規取引を作成
 *   2. 商品を追加
 *   3. 取引をファイナライズ（現金決済）
 *   4. ファイナライズ後のステータスを確認
 */
export default function () {
  // 商品一覧から1件取得
  const productsRes = http.get(`${BASE_URL}/api/products?page=1&pageSize=5`, {
    headers: defaultHeaders,
  })

  if (productsRes.status !== 200) {
    console.error('Failed to fetch products')
    return
  }

  const products = productsRes.json()
  if (!products || !products.data || products.data.length === 0) {
    console.error('No products available')
    return
  }

  const productId = products.data[0].id

  // 新規取引作成
  const createPayload = JSON.stringify({
    storeId: STORE_ID,
    terminalId: TERMINAL_ID,
    staffId: STAFF_ID,
    type: 'SALE',
    clientId: `k6-finalize-${Date.now()}-${__VU}-${__ITER}`,
  })

  const createRes = http.post(`${BASE_URL}/api/transactions`, createPayload, {
    headers: defaultHeaders,
  })

  if (createRes.status >= 300) {
    console.error(`Transaction create failed: ${createRes.status}`)
    return
  }

  const transactionId = createRes.json().id

  sleep(0.2)

  // 商品追加
  const addItemRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/items`,
    JSON.stringify({ productId: productId, quantity: 1 }),
    { headers: defaultHeaders },
  )

  if (addItemRes.status >= 300) {
    console.error(`Add item failed: ${addItemRes.status}`)
    return
  }

  const total = addItemRes.json().total

  sleep(0.2)

  // ファイナライズ（現金決済）
  const finalizePayload = JSON.stringify({
    payments: [
      {
        method: 'CASH',
        amount: total,
        received: total,
      },
    ],
  })

  const finalizeRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/finalize`,
    finalizePayload,
    { headers: defaultHeaders },
  )

  check(finalizeRes, {
    'finalize: status 2xx': (r) => r.status >= 200 && r.status < 300,
    'finalize: transaction completed': (r) => {
      const body = r.json()
      return body && body.transaction && body.transaction.status === 'COMPLETED'
    },
  })

  sleep(0.3)

  // ファイナライズ後の取引詳細
  const detailRes = http.get(`${BASE_URL}/api/transactions/${transactionId}`, {
    headers: defaultHeaders,
  })
  check(detailRes, {
    'post-finalize: status 200': (r) => r.status === 200,
    'post-finalize: COMPLETED': (r) => {
      const body = r.json()
      return body && body.status === 'COMPLETED'
    },
  })

  sleep(0.5)
}
