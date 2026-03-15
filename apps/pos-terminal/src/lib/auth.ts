/**
 * PKCE (Proof Key for Code Exchange) authentication helpers.
 * Used for OAuth2/OIDC flow with ORY Hydra.
 */

const HYDRA_PUBLIC_URL = import.meta.env.VITE_HYDRA_PUBLIC_URL || 'http://localhost:14444'
const CLIENT_ID = import.meta.env.VITE_OIDC_CLIENT_ID || 'pos-terminal'
const REDIRECT_URI = import.meta.env.VITE_OIDC_REDIRECT_URI || `${window.location.origin}/callback`

/** Generate a cryptographically random code verifier (43-128 chars) */
export function generateCodeVerifier(): string {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return base64UrlEncode(array)
}

/** Generate a code challenge from a code verifier using SHA-256 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(verifier)
  const digest = await crypto.subtle.digest('SHA-256', data)
  return base64UrlEncode(new Uint8Array(digest))
}

/** Base64 URL-safe encoding (no padding) */
function base64UrlEncode(buffer: Uint8Array): string {
  let binary = ''
  for (let i = 0; i < buffer.length; i++) {
    binary += String.fromCharCode(buffer[i])
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/** Stored token data */
export interface TokenData {
  accessToken: string
  refreshToken: string | null
  expiresAt: number
  idToken: string | null
}

const TOKEN_STORAGE_KEY = 'openpos-tokens'
const VERIFIER_STORAGE_KEY = 'openpos-pkce-verifier'

/** Store tokens securely (sessionStorage, never localStorage) */
export function storeTokens(tokens: TokenData): void {
  sessionStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens))
}

/** Retrieve stored tokens */
export function getStoredTokens(): TokenData | null {
  const raw = sessionStorage.getItem(TOKEN_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as TokenData
  } catch {
    return null
  }
}

/** Clear stored tokens */
export function clearTokens(): void {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY)
  sessionStorage.removeItem(VERIFIER_STORAGE_KEY)
}

/** Check if stored tokens are expired */
export function isTokenExpired(): boolean {
  const tokens = getStoredTokens()
  if (!tokens) return true
  return Date.now() >= tokens.expiresAt - 30_000 // 30s buffer
}

/** Start the PKCE authorization flow */
export async function startAuthFlow(): Promise<void> {
  const verifier = generateCodeVerifier()
  const challenge = await generateCodeChallenge(verifier)

  sessionStorage.setItem(VERIFIER_STORAGE_KEY, verifier)

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: 'openid offline_access',
    code_challenge: challenge,
    code_challenge_method: 'S256',
    state: crypto.randomUUID(),
  })

  window.location.href = `${HYDRA_PUBLIC_URL}/oauth2/auth?${params.toString()}`
}

/** Exchange authorization code for tokens */
export async function exchangeCodeForTokens(code: string): Promise<TokenData> {
  const verifier = sessionStorage.getItem(VERIFIER_STORAGE_KEY)
  if (!verifier) throw new Error('PKCE verifier not found')

  const response = await fetch(`${HYDRA_PUBLIC_URL}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: REDIRECT_URI,
      client_id: CLIENT_ID,
      code_verifier: verifier,
    }),
  })

  if (!response.ok) {
    throw new Error(`Token exchange failed: ${response.status}`)
  }

  const data = (await response.json()) as {
    access_token: string
    refresh_token?: string
    expires_in: number
    id_token?: string
  }

  const tokens: TokenData = {
    accessToken: data.access_token,
    refreshToken: data.refresh_token ?? null,
    expiresAt: Date.now() + data.expires_in * 1000,
    idToken: data.id_token ?? null,
  }

  storeTokens(tokens)
  sessionStorage.removeItem(VERIFIER_STORAGE_KEY)
  return tokens
}

/** Refresh the access token using the refresh token */
export async function refreshAccessToken(): Promise<TokenData> {
  const tokens = getStoredTokens()
  if (!tokens?.refreshToken) throw new Error('No refresh token available')

  const response = await fetch(`${HYDRA_PUBLIC_URL}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      refresh_token: tokens.refreshToken,
      client_id: CLIENT_ID,
    }),
  })

  if (!response.ok) {
    clearTokens()
    throw new Error(`Token refresh failed: ${response.status}`)
  }

  const data = (await response.json()) as {
    access_token: string
    refresh_token?: string
    expires_in: number
    id_token?: string
  }

  const newTokens: TokenData = {
    accessToken: data.access_token,
    refreshToken: data.refresh_token ?? tokens.refreshToken,
    expiresAt: Date.now() + data.expires_in * 1000,
    idToken: data.id_token ?? tokens.idToken,
  }

  storeTokens(newTokens)
  return newTokens
}

/**
 * Get a valid access token, refreshing if necessary.
 * Returns null if no tokens are available.
 */
export async function getValidAccessToken(): Promise<string | null> {
  const tokens = getStoredTokens()
  if (!tokens) return null

  if (isTokenExpired()) {
    if (!tokens.refreshToken) return null
    try {
      const refreshed = await refreshAccessToken()
      return refreshed.accessToken
    } catch {
      return null
    }
  }

  return tokens.accessToken
}

/**
 * Create a fetch wrapper that adds Authorization header.
 * Falls back to no-auth if no tokens are available.
 */
export function createAuthenticatedFetch(): typeof fetch {
  return async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const token = await getValidAccessToken()
    const headers = new Headers(init?.headers)
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return fetch(input, { ...init, headers })
  }
}
