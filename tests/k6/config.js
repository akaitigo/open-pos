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

export const defaultThresholds = {
  http_req_duration: ['p(95)<2000'],
  http_req_failed: ['rate<0.05'],
}
