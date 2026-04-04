/**
 * 店舗/端末選択コンポーネント (#438)
 * POS端末起動時に使用する店舗と端末を選択する画面。
 * runtime-config に storeId/terminalId が未設定、または手動選択モードで表示される。
 */

import { useEffect, useState } from 'react'
import { z } from 'zod'
import { Loader2, Store, Monitor } from 'lucide-react'
import { t } from '@/i18n'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { api } from '@/lib/api'
import { toast } from '@/hooks/use-toast'
import { StoreSchema, TerminalSchema, PaginationResponseSchema } from '@shared-types/openpos'
import type { Store as StoreType, Terminal } from '@shared-types/openpos'

const PaginatedStoresSchema = z.object({
  data: z.array(StoreSchema),
  pagination: PaginationResponseSchema,
})

const TerminalListSchema = z.array(TerminalSchema)

interface StoreTerminalSelectorProps {
  onSelect: (storeId: string, storeName: string, terminalId: string) => void
}

export function StoreTerminalSelector({ onSelect }: StoreTerminalSelectorProps) {
  const [stores, setStores] = useState<StoreType[]>([])
  const [terminals, setTerminals] = useState<Terminal[]>([])
  const [selectedStore, setSelectedStore] = useState<StoreType | null>(null)
  const [loadingStores, setLoadingStores] = useState(true)
  const [loadingTerminals, setLoadingTerminals] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function loadStores() {
      setLoadingStores(true)
      try {
        const result = await api.get('/api/stores', PaginatedStoresSchema, {
          params: { page: 1, pageSize: 100 },
        })
        if (!cancelled) {
          setStores(result.data.filter((s) => s.isActive))
        }
      } catch {
        if (!cancelled) {
          toast({
            title: 'エラー',
            description: '店舗一覧の取得に失敗しました',
            variant: 'destructive',
          })
        }
      } finally {
        if (!cancelled) {
          setLoadingStores(false)
        }
      }
    }

    void loadStores()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!selectedStore) {
      setTerminals([])
      return
    }

    let cancelled = false

    async function loadTerminals() {
      if (!selectedStore) return
      setLoadingTerminals(true)
      try {
        const result = await api.get(
          `/api/stores/${selectedStore.id}/terminals`,
          TerminalListSchema,
        )
        if (!cancelled) {
          setTerminals(result.filter((t) => t.isActive))
        }
      } catch {
        if (!cancelled) {
          toast({
            title: 'エラー',
            description: '端末一覧の取得に失敗しました',
            variant: 'destructive',
          })
        }
      } finally {
        if (!cancelled) {
          setLoadingTerminals(false)
        }
      }
    }

    void loadTerminals()
    return () => {
      cancelled = true
    }
  }, [selectedStore])

  function handleTerminalSelect(terminal: Terminal) {
    if (!selectedStore) return
    onSelect(selectedStore.id, selectedStore.name, terminal.id)
  }

  if (loadingStores) {
    return (
      <div
        className="flex min-h-svh items-center justify-center bg-muted/30"
        data-testid="store-selector-loading"
        role="status"
        aria-label={t('common.loading')}
      >
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" aria-hidden="true" />
      </div>
    )
  }

  return (
    <div
      className="flex min-h-svh items-center justify-center bg-muted/30 p-4"
      data-testid="store-terminal-selector"
    >
      <Card className="w-full max-w-lg p-6">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold">OpenPOS Terminal</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {selectedStore ? '端末を選択してください' : '店舗を選択してください'}
          </p>
        </div>

        {!selectedStore ? (
          <div className="space-y-3" data-testid="store-list">
            {stores.length === 0 ? (
              <p className="text-center text-sm text-muted-foreground">
                利用可能な店舗がありません
              </p>
            ) : (
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {stores.map((store) => (
                  <Button
                    key={store.id}
                    variant="outline"
                    className="h-auto flex-col gap-2 py-4"
                    onClick={() => setSelectedStore(store)}
                    data-testid={`store-item-${store.id}`}
                  >
                    <Store className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
                    <span className="text-sm font-medium">{store.name}</span>
                    {store.address && (
                      <span className="text-xs text-muted-foreground">{store.address}</span>
                    )}
                  </Button>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-3" data-testid="terminal-list">
            <Button
              variant="ghost"
              size="sm"
              className="mb-2"
              onClick={() => setSelectedStore(null)}
              data-testid="back-to-stores"
              aria-label={t('accessibility.backToStores')}
            >
              ← 店舗選択に戻る
            </Button>

            <p className="text-center text-sm font-medium">{selectedStore.name}</p>

            {loadingTerminals ? (
              <div
                className="flex justify-center py-8"
                role="status"
                aria-label={t('common.loading')}
              >
                <Loader2
                  className="h-6 w-6 animate-spin text-muted-foreground"
                  aria-hidden="true"
                />
              </div>
            ) : terminals.length === 0 ? (
              <p className="text-center text-sm text-muted-foreground">
                利用可能な端末がありません
              </p>
            ) : (
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {terminals.map((terminal) => (
                  <Button
                    key={terminal.id}
                    variant="outline"
                    className="h-auto flex-col gap-2 py-4"
                    onClick={() => handleTerminalSelect(terminal)}
                    data-testid={`terminal-item-${terminal.id}`}
                  >
                    <Monitor className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
                    <span className="text-sm font-medium">
                      {terminal.name ?? terminal.terminalCode}
                    </span>
                    <span className="text-xs text-muted-foreground">{terminal.terminalCode}</span>
                  </Button>
                ))}
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  )
}
