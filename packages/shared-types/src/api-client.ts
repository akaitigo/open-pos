import { z } from 'zod'
import { ApiErrorResponseSchema } from './api-types'

/** API クライアントのオプション */
export interface ApiClientOptions {
  /** ベース URL */
  baseUrl: string
  /** 組織 ID（マルチテナント用） */
  organizationId?: string
  /** リクエストごとに追加するヘッダー */
  headers?: Record<string, string>
}

/** API エラークラス */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/** HTTP メソッド */
type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

/** リクエストオプション */
interface RequestOptions {
  /** クエリパラメータ */
  params?: Record<string, string | number | boolean | undefined>
  /** リクエストヘッダー */
  headers?: Record<string, string>
  /** AbortSignal */
  signal?: AbortSignal
}

/** API クライアント */
export interface ApiClient {
  /** GET リクエスト（Zod スキーマでバリデーション） */
  get<T>(path: string, schema: z.ZodType<T>, options?: RequestOptions): Promise<T>

  /** POST リクエスト（Zod スキーマでバリデーション） */
  post<T>(path: string, body: unknown, schema: z.ZodType<T>, options?: RequestOptions): Promise<T>

  /** PUT リクエスト（Zod スキーマでバリデーション） */
  put<T>(path: string, body: unknown, schema: z.ZodType<T>, options?: RequestOptions): Promise<T>

  /** PATCH リクエスト（Zod スキーマでバリデーション） */
  patch<T>(path: string, body: unknown, schema: z.ZodType<T>, options?: RequestOptions): Promise<T>

  /** DELETE リクエスト */
  delete(path: string, options?: RequestOptions): Promise<void>

  /** 組織 ID を設定 */
  setOrganizationId(organizationId?: string | null): void

  /** ベース URL を設定 */
  setBaseUrl(baseUrl: string): void
}

/** URL にクエリパラメータを付与する */
function buildUrl(
  baseUrl: string,
  path: string,
  params?: Record<string, string | number | boolean | undefined>,
): string {
  const url = new URL(path, baseUrl)

  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value))
      }
    }
  }

  return url.toString()
}

/** レスポンスのエラー処理 */
async function handleErrorResponse(response: Response): Promise<never> {
  let code = 'UNKNOWN_ERROR'
  let message = `HTTP ${response.status}: ${response.statusText}`
  let details: Record<string, unknown> | undefined

  try {
    const body: unknown = await response.json()
    const parsed = ApiErrorResponseSchema.safeParse(body)
    if (parsed.success) {
      code = parsed.data.error
      message = parsed.data.message
      details = parsed.data.details
    }
  } catch {
    // JSON パース失敗時はデフォルトのメッセージを使用
  }

  throw new ApiError(response.status, code, message, details)
}

/** API クライアントを作成する */
export function createApiClient(options: ApiClientOptions | string): ApiClient {
  const config: ApiClientOptions =
    typeof options === 'string' ? { baseUrl: options } : { ...options }

  let baseUrl = config.baseUrl
  let organizationId = config.organizationId

  /** 共通リクエスト処理 */
  async function request<T>(
    method: HttpMethod,
    path: string,
    schema: z.ZodType<T> | null,
    body?: unknown,
    requestOptions?: RequestOptions,
  ): Promise<T> {
    const url = buildUrl(baseUrl, path, requestOptions?.params)

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...config.headers,
      ...requestOptions?.headers,
    }

    if (organizationId) {
      headers['X-Organization-Id'] = organizationId
    }

    const fetchOptions: RequestInit = {
      method,
      headers,
      signal: requestOptions?.signal,
    }

    if (body !== undefined && method !== 'GET' && method !== 'DELETE') {
      fetchOptions.body = JSON.stringify(body)
    }

    const response = await fetch(url, fetchOptions)

    if (!response.ok) {
      await handleErrorResponse(response)
    }

    // DELETE は void を返す
    if (method === 'DELETE' || response.status === 204) {
      return undefined as T
    }

    const json: unknown = await response.json()

    if (schema) {
      return schema.parse(json)
    }

    return json as T
  }

  return {
    get<T>(path: string, schema: z.ZodType<T>, options?: RequestOptions): Promise<T> {
      return request('GET', path, schema, undefined, options)
    },

    post<T>(
      path: string,
      body: unknown,
      schema: z.ZodType<T>,
      options?: RequestOptions,
    ): Promise<T> {
      return request('POST', path, schema, body, options)
    },

    put<T>(
      path: string,
      body: unknown,
      schema: z.ZodType<T>,
      options?: RequestOptions,
    ): Promise<T> {
      return request('PUT', path, schema, body, options)
    },

    patch<T>(
      path: string,
      body: unknown,
      schema: z.ZodType<T>,
      options?: RequestOptions,
    ): Promise<T> {
      return request('PATCH', path, schema, body, options)
    },

    async delete(path: string, options?: RequestOptions): Promise<void> {
      await request('DELETE', path, null, undefined, options)
    },

    setOrganizationId(id?: string | null): void {
      organizationId = id ?? undefined
    },

    setBaseUrl(nextBaseUrl: string): void {
      baseUrl = nextBaseUrl
    },
  }
}
