import { z } from 'zod'

/** Stored token data */
export interface TokenData {
  accessToken: string
  refreshToken: string | null
  expiresAt: number
  idToken: string | null
}

/** Minimal storage contract for browser auth flows */
export interface StorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

/** Minimal location contract for browser redirects */
export interface BrowserLocationLike {
  href: string
}

/** Environment dependencies needed for PKCE auth helpers */
export interface BrowserAuthDependencies {
  storage: StorageLike
  location: BrowserLocationLike
  crypto: Crypto
  fetch: typeof fetch
  btoa: (value: string) => string
  atob: (value: string) => string
  now?: () => number
}

/** Static PKCE auth configuration */
export interface BrowserAuthConfig {
  hydraPublicUrl: string
  clientId: string
  redirectUri: string
  tokenStorageKey: string
  verifierStorageKey: string
  stateStorageKey: string
  maxRefreshRetries?: number
}

/** PKCE auth client */
export interface BrowserAuthClient {
  generateCodeVerifier(): string
  generateCodeChallenge(verifier: string): Promise<string>
  storeTokens(tokens: TokenData): void
  getStoredTokens(): TokenData | null
  clearTokens(): void
  isTokenExpired(): boolean
  isRefreshTokenExpired(refreshToken: string): boolean
  startAuthFlow(): Promise<void>
  validateState(callbackState: string): void
  exchangeCodeForTokens(code: string, state: string): Promise<TokenData>
  refreshAccessToken(): Promise<TokenData>
  getValidAccessToken(): Promise<string | null>
  createAuthenticatedFetch(): typeof fetch
}

const DEFAULT_MAX_REFRESH_RETRIES = 3

const TokenDataSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string().nullable(),
  expiresAt: z.number(),
  idToken: z.string().nullable(),
})

const OAuthTokenResponseSchema = z.object({
  access_token: z.string(),
  refresh_token: z.string().optional(),
  expires_in: z.number(),
  id_token: z.string().optional(),
})

const JwtExpPayloadSchema = z
  .object({
    exp: z.number(),
  })
  .passthrough()

function createDefaultDependencies(): BrowserAuthDependencies {
  return {
    storage: sessionStorage,
    location: window.location,
    crypto: globalThis.crypto,
    fetch: globalThis.fetch.bind(globalThis),
    btoa: globalThis.btoa.bind(globalThis),
    atob: globalThis.atob.bind(globalThis),
    now: () => Date.now(),
  }
}

export function createBrowserAuthClient(
  config: BrowserAuthConfig,
  dependencies: BrowserAuthDependencies = createDefaultDependencies(),
): BrowserAuthClient {
  const {
    storage,
    location,
    crypto,
    fetch: fetcher,
    btoa: encodeBase64,
    atob: decodeBase64,
    now = () => Date.now(),
  } = dependencies

  const maxRefreshRetries = config.maxRefreshRetries ?? DEFAULT_MAX_REFRESH_RETRIES
  let refreshRetryCount = 0

  function base64UrlEncode(buffer: Uint8Array): string {
    let binary = ''
    for (let i = 0; i < buffer.length; i++) {
      binary += String.fromCharCode(buffer[i] ?? 0)
    }
    return encodeBase64(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  }

  function getJwtExpiration(token: string): number | null {
    try {
      const parts = token.split('.')
      if (parts.length !== 3) return null
      const payloadPart = parts[1]
      if (!payloadPart) return null

      const padded = payloadPart + '='.repeat((4 - (payloadPart.length % 4)) % 4)
      const decoded = decodeBase64(padded.replace(/-/g, '+').replace(/_/g, '/'))
      const json: unknown = JSON.parse(decoded)
      const result = JwtExpPayloadSchema.safeParse(json)
      if (result.success) {
        return result.data.exp
      }
      return null
    } catch {
      return null
    }
  }

  async function generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder()
    const data = encoder.encode(verifier)
    const digest = await crypto.subtle.digest('SHA-256', data)
    return base64UrlEncode(new Uint8Array(digest))
  }

  function generateCodeVerifier(): string {
    const array = new Uint8Array(32)
    crypto.getRandomValues(array)
    return base64UrlEncode(array)
  }

  function storeTokens(tokens: TokenData): void {
    storage.setItem(config.tokenStorageKey, JSON.stringify(tokens))
  }

  function getStoredTokens(): TokenData | null {
    const raw = storage.getItem(config.tokenStorageKey)
    if (!raw) return null

    try {
      const parsed: unknown = JSON.parse(raw)
      const result = TokenDataSchema.safeParse(parsed)
      if (result.success) {
        return result.data
      }
      return null
    } catch {
      return null
    }
  }

  function clearTokens(): void {
    storage.removeItem(config.tokenStorageKey)
    storage.removeItem(config.verifierStorageKey)
    storage.removeItem(config.stateStorageKey)
    refreshRetryCount = 0
  }

  function isTokenExpired(): boolean {
    const tokens = getStoredTokens()
    if (!tokens) return true
    return now() >= tokens.expiresAt - 30_000
  }

  function isRefreshTokenExpired(refreshToken: string): boolean {
    const exp = getJwtExpiration(refreshToken)
    if (exp === null) {
      return false
    }
    return now() >= exp * 1000 - 30_000
  }

  async function startAuthFlow(): Promise<void> {
    const verifier = generateCodeVerifier()
    const challenge = await generateCodeChallenge(verifier)
    const state = crypto.randomUUID()

    storage.setItem(config.verifierStorageKey, verifier)
    storage.setItem(config.stateStorageKey, state)

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: config.clientId,
      redirect_uri: config.redirectUri,
      scope: 'openid offline_access',
      code_challenge: challenge,
      code_challenge_method: 'S256',
      state,
    })

    location.href = `${config.hydraPublicUrl}/oauth2/auth?${params.toString()}`
  }

  function validateState(callbackState: string): void {
    const savedState = storage.getItem(config.stateStorageKey)
    if (!savedState) {
      throw new Error('OAuth state not found in session - possible CSRF attack')
    }
    if (callbackState !== savedState) {
      storage.removeItem(config.stateStorageKey)
      throw new Error('OAuth state mismatch - possible CSRF attack')
    }
    storage.removeItem(config.stateStorageKey)
  }

  async function exchangeCodeForTokens(code: string, state: string): Promise<TokenData> {
    validateState(state)

    const verifier = storage.getItem(config.verifierStorageKey)
    if (!verifier) {
      throw new Error('PKCE verifier not found')
    }

    const response = await fetcher(`${config.hydraPublicUrl}/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        redirect_uri: config.redirectUri,
        client_id: config.clientId,
        code_verifier: verifier,
      }),
    })

    if (!response.ok) {
      throw new Error(`Token exchange failed: ${response.status}`)
    }

    const json: unknown = await response.json()
    const data = OAuthTokenResponseSchema.parse(json)

    const tokens: TokenData = {
      accessToken: data.access_token,
      refreshToken: data.refresh_token ?? null,
      expiresAt: now() + data.expires_in * 1000,
      idToken: data.id_token ?? null,
    }

    storeTokens(tokens)
    storage.removeItem(config.verifierStorageKey)
    refreshRetryCount = 0
    return tokens
  }

  async function refreshAccessToken(): Promise<TokenData> {
    const tokens = getStoredTokens()
    if (!tokens?.refreshToken) {
      throw new Error('No refresh token available')
    }

    if (isRefreshTokenExpired(tokens.refreshToken)) {
      clearTokens()
      throw new Error('Refresh token expired')
    }

    if (refreshRetryCount >= maxRefreshRetries) {
      clearTokens()
      throw new Error('Max refresh retries exceeded')
    }
    refreshRetryCount++

    const response = await fetcher(`${config.hydraPublicUrl}/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        refresh_token: tokens.refreshToken,
        client_id: config.clientId,
      }),
    })

    if (!response.ok) {
      clearTokens()
      throw new Error(`Token refresh failed: ${response.status}`)
    }

    const json: unknown = await response.json()
    const data = OAuthTokenResponseSchema.parse(json)

    const newTokens: TokenData = {
      accessToken: data.access_token,
      refreshToken: data.refresh_token ?? tokens.refreshToken,
      expiresAt: now() + data.expires_in * 1000,
      idToken: data.id_token ?? tokens.idToken,
    }

    storeTokens(newTokens)
    refreshRetryCount = 0
    return newTokens
  }

  async function getValidAccessToken(): Promise<string | null> {
    const tokens = getStoredTokens()
    if (!tokens) return null

    if (isTokenExpired()) {
      if (!tokens.refreshToken) return null

      if (isRefreshTokenExpired(tokens.refreshToken)) {
        clearTokens()
        await startAuthFlow()
        return null
      }

      try {
        const refreshed = await refreshAccessToken()
        return refreshed.accessToken
      } catch {
        await startAuthFlow()
        return null
      }
    }

    return tokens.accessToken
  }

  function createAuthenticatedFetch(): typeof fetch {
    return async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
      const token = await getValidAccessToken()
      const headers = new Headers(init?.headers)
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
      return fetcher(input, { ...init, headers })
    }
  }

  return {
    generateCodeVerifier,
    generateCodeChallenge,
    storeTokens,
    getStoredTokens,
    clearTokens,
    isTokenExpired,
    isRefreshTokenExpired,
    startAuthFlow,
    validateState,
    exchangeCodeForTokens,
    refreshAccessToken,
    getValidAccessToken,
    createAuthenticatedFetch,
  }
}
