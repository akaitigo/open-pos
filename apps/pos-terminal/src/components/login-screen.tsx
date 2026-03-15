import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useAuthStore } from '@/stores/auth-store'
import { api } from '@/lib/api'
import { getRuntimeConfig } from '@/lib/runtime-config'
import { toast } from '@/hooks/use-toast'
import {
  StoreSchema,
  StaffSchema,
  AuthenticateByPinResponseSchema,
  PaginationResponseSchema,
} from '@shared-types/openpos'
import type { Staff } from '@shared-types/openpos'
import { z } from 'zod'
import { Loader2, ArrowLeft, Delete } from 'lucide-react'

const PaginatedStaffSchema = z.object({
  data: z.array(StaffSchema),
  pagination: PaginationResponseSchema,
})

export function LoginScreen() {
  const [staffList, setStaffList] = useState<Staff[]>([])
  const [selectedStaff, setSelectedStaff] = useState<Staff | null>(null)
  const [pin, setPin] = useState('')
  const [storeName, setStoreName] = useState('')
  const [loading, setLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const login = useAuthStore((s) => s.login)

  const { storeId, terminalId } = getRuntimeConfig()

  useEffect(() => {
    if (!storeId) {
      setInitialLoading(false)
      return
    }

    Promise.all([
      api.get(`/api/stores/${storeId}`, StoreSchema).then((store) => setStoreName(store.name)),
      api
        .get('/api/staff', PaginatedStaffSchema, { params: { storeId } })
        .then((result) => setStaffList(result.data.filter((s) => s.isActive))),
    ])
      .catch(() => {
        toast({
          title: 'エラー',
          description: '店舗情報の取得に失敗しました',
          variant: 'destructive',
        })
      })
      .finally(() => setInitialLoading(false))
  }, [storeId])

  async function handleLogin() {
    if (!selectedStaff || pin.length < 4) return
    if (!storeId || !terminalId) {
      toast({
        title: '設定不足',
        description: '店舗または端末の設定が見つかりません',
        variant: 'destructive',
      })
      return
    }
    setLoading(true)
    try {
      const result = await api.post(
        `/api/staff/${selectedStaff.id}/authenticate`,
        { storeId, pin },
        AuthenticateByPinResponseSchema,
      )
      if (result.success && result.staff) {
        login(result.staff, storeId, storeName, terminalId, result.token ?? undefined)
      } else {
        toast({
          title: '認証失敗',
          description: result.reason ?? 'PINが正しくありません',
          variant: 'destructive',
        })
      }
    } catch {
      toast({ title: 'エラー', description: '認証処理に失敗しました', variant: 'destructive' })
    } finally {
      setLoading(false)
      setPin('')
    }
  }

  function handleKeyPress(key: string) {
    if (key === 'C') {
      setPin('')
      return
    }
    if (key === '←') {
      setPin((p) => p.slice(0, -1))
      return
    }
    if (pin.length < 8) {
      setPin((p) => p + key)
    }
  }

  if (initialLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center bg-muted/30">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md p-6">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold">OpenPOS Terminal</h1>
          {storeName && <p className="mt-1 text-sm text-muted-foreground">{storeName}</p>}
        </div>

        {!selectedStaff ? (
          <div className="space-y-3">
            <p className="text-center text-sm font-medium text-muted-foreground">
              スタッフを選択してください
            </p>
            <div className="grid grid-cols-2 gap-2">
              {staffList.map((staff) => (
                <Button
                  key={staff.id}
                  variant="outline"
                  className="h-auto flex-col gap-1 py-4"
                  onClick={() => setSelectedStaff(staff)}
                >
                  <span className="text-sm font-medium">{staff.name}</span>
                  <span className="text-xs text-muted-foreground">{staff.role}</span>
                </Button>
              ))}
            </div>
            {staffList.length === 0 && (
              <p className="text-center text-sm text-muted-foreground">
                スタッフが登録されていません
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={() => {
                  setSelectedStaff(null)
                  setPin('')
                }}
              >
                <ArrowLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm font-medium">{selectedStaff.name}</span>
            </div>

            <div className="text-center">
              <p className="mb-2 text-sm text-muted-foreground">PINを入力してください</p>
              <div className="flex justify-center gap-2">
                {Array.from({ length: 8 }).map((_, i) => (
                  <div
                    key={i}
                    className={`h-3 w-3 rounded-full ${i < pin.length ? 'bg-primary' : 'bg-muted'}`}
                  />
                ))}
              </div>
            </div>

            <div className="grid grid-cols-3 gap-2">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', '0', '←'].map((key) => (
                <Button
                  key={key}
                  variant={key === 'C' ? 'destructive' : key === '←' ? 'secondary' : 'outline'}
                  className="h-14 text-lg"
                  onClick={() => handleKeyPress(key)}
                >
                  {key === '←' ? <Delete className="h-5 w-5" /> : key}
                </Button>
              ))}
            </div>

            <Button
              className="w-full"
              size="lg"
              disabled={loading || pin.length < 4}
              onClick={handleLogin}
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  認証中...
                </>
              ) : (
                'ログイン'
              )}
            </Button>
          </div>
        )}
      </Card>
    </div>
  )
}
