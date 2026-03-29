import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { exchangeCodeForTokens } from '@/lib/auth'

/**
 * OAuth2 PKCE コールバックルート。
 * Hydra から返された authorization code をトークンに交換する。
 */
export function CallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const code = searchParams.get('code')
    const state = searchParams.get('state')

    if (!code || !state) {
      navigate('/', { replace: true })
      return
    }

    exchangeCodeForTokens(code, state)
      .then(() => navigate('/', { replace: true }))
      .catch(() => navigate('/', { replace: true }))
  }, [searchParams, navigate])

  return (
    <div className="flex flex-1 items-center justify-center p-8">
      <div className="text-sm text-muted-foreground">認証処理中...</div>
    </div>
  )
}
