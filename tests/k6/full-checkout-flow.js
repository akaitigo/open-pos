import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter, Rate, Trend } from 'k6/metrics'
import {
  BASE_URL,
  defaultHeaders,
  STORE_ID,
  TERMINAL_ID,
  STAFF_ID,
  highLoadStages,
  sloThresholds,
} from './config.js'

// Custom metrics
const checkoutSuccess = new Counter('checkout_success_total')
const checkoutFailure = new Counter('checkout_failure_total')
const checkoutRate = new Rate('checkout_success_rate')
const checkoutDuration = new Trend('checkout_e2e_duration')

export const options = {
  stages: highLoadStages,
  thresholds: {
    ...sloThresholds,
    checkout_success_rate: ['rate>0.95'],
    checkout_e2e_duration: ['p(95)<1000'],
  },
}

/**
 * Full POS checkout flow load test (500+ RPS target)
 *
 * End-to-end scenario:
 *   1. Fetch product list
 *   2. Create a new transaction (DRAFT)
 *   3. Add item to transaction
 *   4. Finalize transaction (cash payment)
 *   5. Verify completed status
 */
export default function () {
  const start = Date.now()

  // 1. Fetch products
  const productsRes = http.get(`${BASE_URL}/api/products?page=1&pageSize=5`, {
    headers: defaultHeaders,
    tags: { name: 'GET /api/products' },
  })

  if (productsRes.status !== 200) {
    checkoutFailure.add(1)
    checkoutRate.add(false)
    return
  }

  const products = productsRes.json()
  if (!products || !products.data || products.data.length === 0) {
    checkoutFailure.add(1)
    checkoutRate.add(false)
    return
  }

  const productId = products.data[0].id

  sleep(0.1)

  // 2. Create transaction
  const createRes = http.post(
    `${BASE_URL}/api/transactions`,
    JSON.stringify({
      storeId: STORE_ID,
      terminalId: TERMINAL_ID,
      staffId: STAFF_ID,
      type: 'SALE',
      clientId: `k6-flow-${Date.now()}-${__VU}-${__ITER}`,
    }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions' } },
  )

  if (createRes.status >= 300) {
    checkoutFailure.add(1)
    checkoutRate.add(false)
    return
  }

  const transactionId = createRes.json().id
  check(createRes, {
    'create: status DRAFT': (r) => r.json().status === 'DRAFT',
  })

  sleep(0.1)

  // 3. Add item
  const addItemRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/items`,
    JSON.stringify({ productId: productId, quantity: 1 }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions/:id/items' } },
  )

  if (addItemRes.status >= 300) {
    checkoutFailure.add(1)
    checkoutRate.add(false)
    return
  }

  const total = addItemRes.json().total
  check(addItemRes, {
    'add item: has items': (r) => {
      const body = r.json()
      return body && body.items && body.items.length > 0
    },
  })

  sleep(0.1)

  // 4. Finalize (cash payment)
  const finalizeRes = http.post(
    `${BASE_URL}/api/transactions/${transactionId}/finalize`,
    JSON.stringify({
      payments: [{ method: 'CASH', amount: total, received: total }],
    }),
    { headers: defaultHeaders, tags: { name: 'POST /api/transactions/:id/finalize' } },
  )

  check(finalizeRes, {
    'finalize: status 2xx': (r) => r.status >= 200 && r.status < 300,
    'finalize: COMPLETED': (r) => {
      const body = r.json()
      return body && body.transaction && body.transaction.status === 'COMPLETED'
    },
  })

  if (finalizeRes.status < 300) {
    checkoutSuccess.add(1)
    checkoutRate.add(true)
  } else {
    checkoutFailure.add(1)
    checkoutRate.add(false)
  }

  const duration = Date.now() - start
  checkoutDuration.add(duration)

  sleep(0.2)
}
