import http from 'k6/http'
import { check, sleep } from 'k6'
import { BASE_URL, defaultHeaders, standardStages, defaultThresholds } from './config.js'

export const options = {
  stages: standardStages,
  thresholds: defaultThresholds,
}

/**
 * 商品一覧 API の負荷テスト
 *
 * シナリオ:
 *   1. 商品一覧取得 (ページネーション付き)
 *   2. カテゴリ一覧取得
 *   3. 商品検索
 */
export default function () {
  // 商品一覧 (1ページ目, 20件)
  const listRes = http.get(`${BASE_URL}/api/products?page=1&pageSize=20`, {
    headers: defaultHeaders,
  })
  check(listRes, {
    'product list: status 200': (r) => r.status === 200,
    'product list: has data': (r) => {
      const body = r.json()
      return body && body.data && body.data.length > 0
    },
    'product list: has pagination': (r) => {
      const body = r.json()
      return body && body.pagination && body.pagination.totalCount > 0
    },
  })

  sleep(0.5)

  // カテゴリ一覧
  const catRes = http.get(`${BASE_URL}/api/categories`, {
    headers: defaultHeaders,
  })
  check(catRes, {
    'categories: status 200': (r) => r.status === 200,
    'categories: non-empty': (r) => {
      const body = r.json()
      return Array.isArray(body) && body.length > 0
    },
  })

  sleep(0.5)

  // 2ページ目の取得
  const page2Res = http.get(`${BASE_URL}/api/products?page=2&pageSize=20`, {
    headers: defaultHeaders,
  })
  check(page2Res, {
    'product page 2: status 200': (r) => r.status === 200,
  })

  sleep(1)
}
