/**
 * k6 load test configuration for open-pos.
 *
 * Override via environment variables:
 *   K6_BASE_URL      - API gateway URL (default: http://localhost:8080)
 *   K6_ORG_ID        - Organization UUID (required)
 *   K6_STORE_ID      - Store UUID (required for transaction tests)
 *   K6_TERMINAL_ID   - Terminal UUID (required for transaction tests)
 *   K6_STAFF_ID      - Staff UUID (required for transaction tests)
 */

export const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:8080'
export const ORG_ID = __ENV.K6_ORG_ID || ''
export const STORE_ID = __ENV.K6_STORE_ID || ''
export const TERMINAL_ID = __ENV.K6_TERMINAL_ID || ''
export const STAFF_ID = __ENV.K6_STAFF_ID || ''

export const defaultHeaders = {
  'Content-Type': 'application/json',
  'X-Organization-Id': ORG_ID,
}

/** Standard load profile: ramp up -> sustain -> ramp down */
export const standardStages = [
  { duration: '10s', target: 5 },
  { duration: '30s', target: 10 },
  { duration: '10s', target: 0 },
]

/** Light smoke profile for quick validation */
export const smokeStages = [
  { duration: '5s', target: 2 },
  { duration: '10s', target: 2 },
  { duration: '5s', target: 0 },
]

/** High-load profile: 500+ RPS target (adjust VUs based on API response time) */
export const highLoadStages = [
  { duration: '30s', target: 50 }, // Warm-up
  { duration: '1m', target: 200 }, // Ramp to moderate load
  { duration: '2m', target: 500 }, // Sustain peak load (500+ RPS)
  { duration: '1m', target: 500 }, // Hold peak
  { duration: '30s', target: 0 }, // Ramp down
]

/** Stress test profile: find breaking point */
export const stressStages = [
  { duration: '30s', target: 100 },
  { duration: '1m', target: 300 },
  { duration: '1m', target: 600 },
  { duration: '1m', target: 1000 },
  { duration: '30s', target: 0 },
]

export const defaultThresholds = {
  http_req_duration: ['p(95)<2000'],
  http_req_failed: ['rate<0.05'],
}

/** SLO thresholds: P95 < 200ms, error rate < 1% */
export const sloThresholds = {
  http_req_duration: ['p(95)<200', 'p(99)<500'],
  http_req_failed: ['rate<0.01'],
}
