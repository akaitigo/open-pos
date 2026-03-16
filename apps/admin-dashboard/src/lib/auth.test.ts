import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  generateCodeVerifier,
  generateCodeChallenge,
  storeTokens,
  getStoredTokens,
  clearTokens,
  isTokenExpired,
  getValidAccessToken,
  type TokenData,
} from './auth'

describe('auth', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  describe('generateCodeVerifier', () => {
    it('文字列を返す', () => {
      const verifier = generateCodeVerifier()
      expect(typeof verifier).toBe('string')
      expect(verifier.length).toBeGreaterThan(0)
    })

    it('Base64 URL-safe 文字のみ含む', () => {
      const verifier = generateCodeVerifier()
      expect(verifier).toMatch(/^[A-Za-z0-9_-]+$/)
    })

    it('毎回異なる値を生成する', () => {
      const v1 = generateCodeVerifier()
      const v2 = generateCodeVerifier()
      expect(v1).not.toBe(v2)
    })
  })

  describe('generateCodeChallenge', () => {
    it('verifier から challenge を生成する', async () => {
      const verifier = generateCodeVerifier()
      const challenge = await generateCodeChallenge(verifier)
      expect(typeof challenge).toBe('string')
      expect(challenge.length).toBeGreaterThan(0)
    })

    it('Base64 URL-safe 文字のみ含む（パディングなし）', async () => {
      const verifier = generateCodeVerifier()
      const challenge = await generateCodeChallenge(verifier)
      expect(challenge).toMatch(/^[A-Za-z0-9_-]+$/)
      expect(challenge).not.toContain('=')
    })

    it('同じ verifier から同じ challenge を生成する', async () => {
      const verifier = generateCodeVerifier()
      const c1 = await generateCodeChallenge(verifier)
      const c2 = await generateCodeChallenge(verifier)
      expect(c1).toBe(c2)
    })
  })

  describe('storeTokens / getStoredTokens', () => {
    const mockTokens: TokenData = {
      accessToken: 'access-token-123',
      refreshToken: 'refresh-token-456',
      expiresAt: Date.now() + 3600_000,
      idToken: 'id-token-789',
    }

    it('トークンを保存して取得できる', () => {
      storeTokens(mockTokens)
      const stored = getStoredTokens()
      expect(stored).toEqual(mockTokens)
    })

    it('トークンがない場合は null を返す', () => {
      expect(getStoredTokens()).toBeNull()
    })

    it('不正な JSON の場合は null を返す', () => {
      sessionStorage.setItem('openpos-admin-tokens', 'invalid-json')
      expect(getStoredTokens()).toBeNull()
    })
  })

  describe('clearTokens', () => {
    it('トークンとverifierを削除する', () => {
      sessionStorage.setItem('openpos-admin-tokens', '{}')
      sessionStorage.setItem('openpos-admin-pkce-verifier', 'test-verifier')
      clearTokens()
      expect(sessionStorage.getItem('openpos-admin-tokens')).toBeNull()
      expect(sessionStorage.getItem('openpos-admin-pkce-verifier')).toBeNull()
    })
  })

  describe('isTokenExpired', () => {
    it('トークンがない場合は true を返す', () => {
      expect(isTokenExpired()).toBe(true)
    })

    it('有効期限内のトークンは false を返す', () => {
      const tokens: TokenData = {
        accessToken: 'test',
        refreshToken: null,
        expiresAt: Date.now() + 60_000, // 60秒後
        idToken: null,
      }
      storeTokens(tokens)
      expect(isTokenExpired()).toBe(false)
    })

    it('有効期限切れのトークンは true を返す', () => {
      const tokens: TokenData = {
        accessToken: 'test',
        refreshToken: null,
        expiresAt: Date.now() - 1000, // 過去
        idToken: null,
      }
      storeTokens(tokens)
      expect(isTokenExpired()).toBe(true)
    })

    it('30秒バッファを考慮する', () => {
      const tokens: TokenData = {
        accessToken: 'test',
        refreshToken: null,
        expiresAt: Date.now() + 20_000, // 20秒後（バッファ30秒未満）
        idToken: null,
      }
      storeTokens(tokens)
      expect(isTokenExpired()).toBe(true)
    })
  })

  describe('getValidAccessToken', () => {
    it('トークンがない場合は null を返す', async () => {
      const token = await getValidAccessToken()
      expect(token).toBeNull()
    })

    it('有効なトークンがある場合はアクセストークンを返す', async () => {
      const tokens: TokenData = {
        accessToken: 'valid-access-token',
        refreshToken: 'refresh',
        expiresAt: Date.now() + 60_000,
        idToken: null,
      }
      storeTokens(tokens)
      const token = await getValidAccessToken()
      expect(token).toBe('valid-access-token')
    })

    it('トークン期限切れでリフレッシュトークンがない場合は null を返す', async () => {
      const tokens: TokenData = {
        accessToken: 'expired-token',
        refreshToken: null,
        expiresAt: Date.now() - 1000,
        idToken: null,
      }
      storeTokens(tokens)
      const token = await getValidAccessToken()
      expect(token).toBeNull()
    })
  })
})
