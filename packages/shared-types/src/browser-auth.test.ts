import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createBrowserAuthClient,
  type BrowserAuthDependencies,
  type StorageLike,
  type TokenData,
} from './browser-auth'

class MemoryStorage implements StorageLike {
  private readonly data = new Map<string, string>()

  getItem(key: string): string | null {
    return this.data.get(key) ?? null
  }

  setItem(key: string, value: string): void {
    this.data.set(key, value)
  }

  removeItem(key: string): void {
    this.data.delete(key)
  }
}

const FIXED_NOW = 1_700_000_000_000

function createJwtWithExp(expSeconds: number): string {
  const payload = globalThis
    .btoa(JSON.stringify({ exp: expSeconds }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  return `header.${payload}.signature`
}

function createDependencies(
  overrides: Partial<BrowserAuthDependencies> = {},
): BrowserAuthDependencies & { storage: MemoryStorage } {
  const storage = new MemoryStorage()

  const defaults: BrowserAuthDependencies & { storage: MemoryStorage } = {
    storage,
    location: { href: 'http://localhost/app' },
    crypto: {
      getRandomValues(array: Uint8Array) {
        array.forEach((_, index) => {
          array[index] = index + 1
        })
        return array
      },
      randomUUID: () => 'state-123',
      subtle: {
        digest: vi.fn().mockResolvedValue(Uint8Array.from([1, 2, 3, 4]).buffer),
      },
    } as unknown as Crypto,
    fetch: vi.fn(),
    btoa: (value) => globalThis.btoa(value),
    atob: (value) => globalThis.atob(value),
    now: () => FIXED_NOW,
  }

  return {
    ...defaults,
    ...overrides,
    storage: overrides.storage instanceof MemoryStorage ? overrides.storage : storage,
  }
}

function createClient(
  overrides: Partial<BrowserAuthDependencies> = {},
): ReturnType<typeof createBrowserAuthClient> & { __deps: ReturnType<typeof createDependencies> } {
  const deps = createDependencies(overrides)
  const client = createBrowserAuthClient(
    {
      hydraPublicUrl: 'http://hydra.local',
      clientId: 'pos-terminal',
      redirectUri: 'http://localhost/callback',
      tokenStorageKey: 'tokens',
      verifierStorageKey: 'verifier',
      stateStorageKey: 'state',
    },
    deps,
  )

  return Object.assign(client, { __deps: deps })
}

describe('createBrowserAuthClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('認可コードをトークンに交換して保存できる', async () => {
    const client = createClient()
    const fetchMock = vi.mocked(client.__deps.fetch)

    client.__deps.storage.setItem('state', 'expected-state')
    client.__deps.storage.setItem('verifier', 'pkce-verifier')
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          access_token: 'access-123',
          refresh_token: 'refresh-123',
          expires_in: 300,
          id_token: 'id-123',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )

    const tokens = await client.exchangeCodeForTokens('auth-code', 'expected-state')

    expect(tokens).toEqual<TokenData>({
      accessToken: 'access-123',
      refreshToken: 'refresh-123',
      expiresAt: FIXED_NOW + 300_000,
      idToken: 'id-123',
    })
    expect(client.getStoredTokens()).toEqual(tokens)
    expect(client.__deps.storage.getItem('state')).toBeNull()
    expect(client.__deps.storage.getItem('verifier')).toBeNull()

    const [url, init] = fetchMock.mock.calls[0] ?? []
    expect(url).toBe('http://hydra.local/oauth2/token')
    expect(init?.method).toBe('POST')
    expect(init?.headers).toEqual({ 'Content-Type': 'application/x-www-form-urlencoded' })
    expect((init?.body as URLSearchParams).get('grant_type')).toBe('authorization_code')
    expect((init?.body as URLSearchParams).get('code')).toBe('auth-code')
    expect((init?.body as URLSearchParams).get('code_verifier')).toBe('pkce-verifier')
  })

  it('state mismatch 時は state を破棄して CSRF として拒否する', () => {
    const client = createClient()
    client.__deps.storage.setItem('state', 'expected-state')

    expect(() => client.validateState('unexpected-state')).toThrow(
      'OAuth state mismatch - possible CSRF attack',
    )
    expect(client.__deps.storage.getItem('state')).toBeNull()
  })

  it('期限切れアクセストークンは refresh 後に Authorization ヘッダー付きで送信する', async () => {
    const client = createClient()
    const fetchMock = vi.mocked(client.__deps.fetch)
    const refreshToken = createJwtWithExp(Math.floor(FIXED_NOW / 1000) + 3600)

    client.storeTokens({
      accessToken: 'expired-access',
      refreshToken,
      expiresAt: FIXED_NOW - 1_000,
      idToken: null,
    })

    fetchMock.mockImplementation(async (input, init) => {
      const url = typeof input === 'string' ? input : input.toString()

      if (url.endsWith('/oauth2/token')) {
        return new Response(
          JSON.stringify({
            access_token: 'fresh-access',
            refresh_token: refreshToken,
            expires_in: 600,
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        )
      }

      const authorization = new Headers(init?.headers).get('Authorization')
      return new Response(JSON.stringify({ authorization }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    const authenticatedFetch = client.createAuthenticatedFetch()
    const response = await authenticatedFetch('http://api.local/orders')
    const json = (await response.json()) as { authorization: string | null }

    expect(json.authorization).toBe('Bearer fresh-access')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('期限切れ refresh token はログインフローを再開して token を破棄する', async () => {
    const client = createClient()
    const expiredRefreshToken = createJwtWithExp(Math.floor(FIXED_NOW / 1000) - 60)

    client.storeTokens({
      accessToken: 'expired-access',
      refreshToken: expiredRefreshToken,
      expiresAt: FIXED_NOW - 1_000,
      idToken: null,
    })

    const accessToken = await client.getValidAccessToken()

    expect(accessToken).toBeNull()
    expect(client.getStoredTokens()).toBeNull()
    expect(client.__deps.storage.getItem('verifier')).toBeTruthy()
    expect(client.__deps.storage.getItem('state')).toBe('state-123')
    expect(client.__deps.location.href).toContain('http://hydra.local/oauth2/auth?')
  })
})
