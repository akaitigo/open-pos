import { describe, it, expect, vi, beforeEach } from 'vitest'
import { z } from 'zod'
import { createApiClient, ApiError } from './api-client'

const TestSchema = z.object({
  id: z.string(),
  name: z.string(),
})

describe('createApiClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('GET リクエストを送信しレスポンスをバリデーションできる', async () => {
    const mockData = { id: '123', name: 'Test' }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const client = createApiClient('http://localhost:8080')
    const result = await client.get('/api/items/123', TestSchema)

    expect(result).toEqual(mockData)
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/items/123',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Accept: 'application/json',
        }),
      }),
    )
  })

  it('POST リクエストでボディを送信できる', async () => {
    const mockData = { id: '123', name: 'New Item' }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(mockData), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const client = createApiClient('http://localhost:8080')
    const result = await client.post('/api/items', { name: 'New Item' }, TestSchema)

    expect(result).toEqual(mockData)
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/items',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'New Item' }),
      }),
    )
  })

  it('DELETE リクエストが void を返す', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))

    const client = createApiClient('http://localhost:8080')
    await expect(client.delete('/api/items/123')).resolves.toBeUndefined()
  })

  it('X-Organization-Id ヘッダーを設定できる', async () => {
    const mockData = { id: '123', name: 'Test' }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const client = createApiClient({
      baseUrl: 'http://localhost:8080',
      organizationId: 'org-123',
    })
    await client.get('/api/items/123', TestSchema)

    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/items/123',
      expect.objectContaining({
        headers: expect.objectContaining({
          'X-Organization-Id': 'org-123',
        }),
      }),
    )
  })

  it('setOrganizationId で組織 ID を変更できる', async () => {
    const mockData = { id: '123', name: 'Test' }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const client = createApiClient('http://localhost:8080')
    client.setOrganizationId('org-456')
    await client.get('/api/items/123', TestSchema)

    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/items/123',
      expect.objectContaining({
        headers: expect.objectContaining({
          'X-Organization-Id': 'org-456',
        }),
      }),
    )
  })

  it('クエリパラメータを URL に付与できる', async () => {
    const mockData = { id: '123', name: 'Test' }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const client = createApiClient('http://localhost:8080')
    await client.get('/api/items/123', TestSchema, {
      params: { page: 1, pageSize: 20, active: true, search: undefined },
    })

    const calledUrl = vi.mocked(fetch).mock.calls[0]?.[0] as string
    expect(calledUrl).toContain('page=1')
    expect(calledUrl).toContain('pageSize=20')
    expect(calledUrl).toContain('active=true')
    expect(calledUrl).not.toContain('search')
  })

  it('HTTP エラー時に ApiError をスローする', async () => {
    const errorBody = { error: 'NOT_FOUND', message: 'Item not found' }
    vi.spyOn(globalThis, 'fetch').mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify(errorBody), {
          status: 404,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    const client = createApiClient('http://localhost:8080')

    await expect(client.get('/api/items/999', TestSchema)).rejects.toThrow(ApiError)
    await expect(client.get('/api/items/999', TestSchema)).rejects.toMatchObject({
      status: 404,
      code: 'NOT_FOUND',
      message: 'Item not found',
    })
  })

  it('Zod バリデーション失敗時にエラーをスローする', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ id: 123, name: 'Test' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const client = createApiClient('http://localhost:8080')

    // id が string でなく number なのでバリデーションエラー
    await expect(client.get('/api/items/123', TestSchema)).rejects.toThrow()
  })

  it('文字列でクライアントを作成できる', () => {
    const client = createApiClient('http://localhost:8080')
    expect(client).toBeDefined()
    expect(client.get).toBeDefined()
    expect(client.post).toBeDefined()
    expect(client.put).toBeDefined()
    expect(client.patch).toBeDefined()
    expect(client.delete).toBeDefined()
  })
})
