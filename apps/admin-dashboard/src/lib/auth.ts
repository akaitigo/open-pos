/**
 * PKCE (Proof Key for Code Exchange) authentication helpers.
 * Used for OAuth2/OIDC flow with ORY Hydra.
 */

import { createBrowserAuthClient } from '@shared-types/openpos'
export type { TokenData } from '@shared-types/openpos'

const authClient = createBrowserAuthClient({
  hydraPublicUrl: import.meta.env.VITE_HYDRA_PUBLIC_URL || 'http://localhost:14444',
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID || 'admin-dashboard',
  redirectUri: import.meta.env.VITE_OIDC_REDIRECT_URI || `${window.location.origin}/callback`,
  tokenStorageKey: 'openpos-admin-tokens',
  verifierStorageKey: 'openpos-admin-pkce-verifier',
  stateStorageKey: 'openpos-admin-pkce-state',
})

export const generateCodeVerifier = authClient.generateCodeVerifier
export const generateCodeChallenge = authClient.generateCodeChallenge
export const storeTokens = authClient.storeTokens
export const getStoredTokens = authClient.getStoredTokens
export const clearTokens = authClient.clearTokens
export const isTokenExpired = authClient.isTokenExpired
export const isRefreshTokenExpired = authClient.isRefreshTokenExpired
export const startAuthFlow = authClient.startAuthFlow
export const refreshAccessToken = authClient.refreshAccessToken
export const getValidAccessToken = authClient.getValidAccessToken
export const createAuthenticatedFetch = authClient.createAuthenticatedFetch

/** Validate the OAuth2 state parameter to prevent CSRF attacks */
export function validateState(returnedState: string): boolean {
  try {
    authClient.validateState(returnedState)
    return true
  } catch {
    return false
  }
}

/** Exchange authorization code for tokens */
export const exchangeCodeForTokens = authClient.exchangeCodeForTokens
