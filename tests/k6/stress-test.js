import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter, Rate } from 'k6/metrics'
import {
  BASE_URL,
  defaultHeaders,
  STORE_ID,
  TERMINAL_ID,
  STAFF_ID,
  stressStages,
} from './config.js'

const errorsByEndpoint = new Counter('errors_by_endpoint')
const successRate = new Rate('overall_success_rate')

export const options = {
  stages: stressStages,
  thresholds: {
    // Stress test: observe degradation, don't enforce strict SLOs
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.20'],
  },
}

/**
 * Stress test: find the breaking point
 *
 * Mixed workload simulating realistic traffic distribution:
 *   - 50% read operations (product/category listing)
 *   - 30% transaction creation (write-heavy)
 *   - 20% full checkout flow
 */
export default function () {
  const scenario = Math.random()

  if (scenario < 0.5) {
    readWorkload()
  } else if (scenario < 0.8) {
    createTransactionWorkload()
  } else {
    fullCheckoutWorkload()
  }
}

function readWorkload() {
  const res = http.get(`${BASE_URL}/api/products?page=1&pageSize=20`, {
    headers: defaultHeaders,
    tags: { name: 'GET /api/products' },
  })

  const ok = check(res, { 'products: status 200': (r) => r.status === 200 })
  successRate.add(ok)
  if (!ok) errorsByEndpoint.add(1, { endpoint: 'products' })

  sleep(0.1)

  const catRes = http.get(`${BASE_URL}/api/categories`, {
    headers: defaultHeaders,
    tags: { name: 'GET /api/categories' },
  })

  const catOk = check(catRes, { 'categories: status 200': (r) => r.status === 200 })
  successRate.add(catOk)
  if (!catOk) errorsByEndpoint.add(1, { endpoint: 'categories' })

  sleep(0.1)
}

function createTransactionWorkload() {
  const createRes = http.post(
    `${BASE_URL}/api/transactions`,
    JSON.stringify({
      storeId: STORE_ID,
      terminalId: TERMINAL_ID,
      staffId: STAFF_ID,
      type: 'SALE',
      clientId: `k6-stress-${Date.now()}-${__VU}-${__ITER}`,
    }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions' } },
  )

  const ok = check(createRes, {
    'create tx: status 2xx': (r) => r.status >= 200 && r.status < 300,
  })
  successRate.add(ok)
  if (!ok) errorsByEndpoint.add(1, { endpoint: 'transactions_create' })

  sleep(0.1)
}

function fullCheckoutWorkload() {
  // Fetch product
  const productsRes = http.get(`${BASE_URL}/api/products?page=1&pageSize=5`, {
    headers: defaultHeaders,
    tags: { name: 'GET /api/products' },
  })

  if (productsRes.status !== 200) {
    successRate.add(false)
    errorsByEndpoint.add(1, { endpoint: 'products' })
    return
  }

  const products = productsRes.json()
  if (!products || !products.data || products.data.length === 0) return

  const productId = products.data[0].id

  // Create transaction
  const createRes = http.post(
    `${BASE_URL}/api/transactions`,
    JSON.stringify({
      storeId: STORE_ID,
      terminalId: TERMINAL_ID,
      staffId: STAFF_ID,
      type: 'SALE',
      clientId: `k6-stress-flow-${Date.now()}-${__VU}-${__ITER}`,
    }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions' } },
  )

  if (createRes.status >= 300) {
    successRate.add(false)
    errorsByEndpoint.add(1, { endpoint: 'transactions_create' })
    return
  }

  const transactionId = createRes.json().id

  // Add item
  const addRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/items`,
    JSON.stringify({ productId, quantity: 1 }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions/:id/items' } },
  )

  if (addRes.status >= 300) {
    successRate.add(false)
    errorsByEndpoint.add(1, { endpoint: 'transactions_add_item' })
    return
  }

  const total = addRes.json().total

  // Finalize
  const finalizeRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/finalize`,
    JSON.stringify({
      payments: [{ method: 'CASH', amount: total, received: total }],
    }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions/:id/finalize' } },
  )

  const ok = check(finalizeRes, {
    'finalize: status 2xx': (r) => r.status >= 200 && r.status < 300,
  })
  successRate.add(ok)
  if (!ok) errorsByEndpoint.add(1, { endpoint: 'transactions_finalize' })

  sleep(0.1)
}
