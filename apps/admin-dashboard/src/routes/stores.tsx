import { useCallback, useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { api } from '@/lib/api'
import type { Store, Terminal, PaginatedResponse } from '@shared-types/openpos'
import { StoreSchema, TerminalSchema, PaginatedStoresSchema } from '@shared-types/openpos'
import { z } from 'zod'

export function StoresPage() {
  const [stores, setStores] = useState<Store[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingStore, setEditingStore] = useState<Store | null>(null)
  const [terminalDialogStore, setTerminalDialogStore] = useState<Store | null>(null)

  const fetchStores = useCallback(async () => {
    const result = await api.get<PaginatedResponse<Store>>('/api/stores', PaginatedStoresSchema, {
      params: { page, pageSize: 20 },
    })
    setStores(result.data)
    setTotalPages(result.pagination.totalPages)
  }, [page])

  useEffect(() => {
    fetchStores()
  }, [fetchStores])

  function handleCreate() {
    setEditingStore(null)
    setDialogOpen(true)
  }

  function handleEdit(store: Store) {
    setEditingStore(store)
    setDialogOpen(true)
  }

  async function handleSubmit(data: Record<string, unknown>) {
    if (editingStore) {
      await api.put(`/api/stores/${editingStore.id}`, data, StoreSchema)
    } else {
      await api.post('/api/stores', data, StoreSchema)
    }
    setDialogOpen(false)
    fetchStores()
  }

  return (
    <>
      <Header title="店舗管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-end">
          <Button onClick={handleCreate}>店舗を追加</Button>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>店舗名</TableHead>
                <TableHead>住所</TableHead>
                <TableHead>電話番号</TableHead>
                <TableHead>タイムゾーン</TableHead>
                <TableHead>ステータス</TableHead>
                <TableHead className="w-[180px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {stores.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    店舗が登録されていません
                  </TableCell>
                </TableRow>
              ) : (
                stores.map((store) => (
                  <TableRow key={store.id}>
                    <TableCell className="font-medium">{store.name}</TableCell>
                    <TableCell>{store.address ?? '—'}</TableCell>
                    <TableCell>{store.phone ?? '—'}</TableCell>
                    <TableCell className="font-mono text-sm">{store.timezone}</TableCell>
                    <TableCell>
                      <Badge variant={store.isActive ? 'default' : 'secondary'}>
                        {store.isActive ? '有効' : '無効'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" onClick={() => handleEdit(store)}>
                          編集
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setTerminalDialogStore(store)}
                        >
                          端末
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              前へ
            </Button>
            <span className="text-sm text-muted-foreground">
              {page} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              次へ
            </Button>
          </div>
        )}

        <StoreFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          store={editingStore}
          onSubmit={handleSubmit}
        />

        <TerminalDialog store={terminalDialogStore} onClose={() => setTerminalDialogStore(null)} />
      </div>
    </>
  )
}

function StoreFormDialog({
  open,
  onOpenChange,
  store,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  store: Store | null
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [timezone, setTimezone] = useState('Asia/Tokyo')

  useEffect(() => {
    if (store) {
      setName(store.name)
      setAddress(store.address ?? '')
      setPhone(store.phone ?? '')
      setTimezone(store.timezone)
    } else {
      setName('')
      setAddress('')
      setPhone('')
      setTimezone('Asia/Tokyo')
    }
  }, [store, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      name,
      address: address || undefined,
      phone: phone || undefined,
      timezone,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{store ? '店舗を編集' : '店舗を追加'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="store-name">店舗名 *</Label>
            <Input
              id="store-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="store-address">住所</Label>
            <Input
              id="store-address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="store-phone">電話番号</Label>
              <Input id="store-phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="store-timezone">タイムゾーン</Label>
              <Input
                id="store-timezone"
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">{store ? '更新' : '追加'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function TerminalDialog({ store, onClose }: { store: Store | null; onClose: () => void }) {
  const [terminals, setTerminals] = useState<Terminal[]>([])
  const [terminalCode, setTerminalCode] = useState('')
  const [terminalName, setTerminalName] = useState('')

  useEffect(() => {
    if (store) {
      api.get(`/api/stores/${store.id}/terminals`, z.array(TerminalSchema)).then(setTerminals)
    }
  }, [store])

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault()
    if (!store) return
    await api.post(
      `/api/stores/${store.id}/terminals`,
      { terminalCode, name: terminalName },
      TerminalSchema,
    )
    setTerminalCode('')
    setTerminalName('')
    const updated = await api.get(`/api/stores/${store.id}/terminals`, z.array(TerminalSchema))
    setTerminals(updated)
  }

  return (
    <Dialog open={store !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{store?.name} — 端末管理</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>端末コード</TableHead>
                  <TableHead>名前</TableHead>
                  <TableHead>ステータス</TableHead>
                  <TableHead>最終同期</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {terminals.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      端末が登録されていません
                    </TableCell>
                  </TableRow>
                ) : (
                  terminals.map((t) => (
                    <TableRow key={t.id}>
                      <TableCell className="font-mono">{t.terminalCode}</TableCell>
                      <TableCell>{t.name ?? '—'}</TableCell>
                      <TableCell>
                        <Badge variant={t.isActive ? 'default' : 'secondary'}>
                          {t.isActive ? '有効' : '無効'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {t.lastSyncAt ?? '未同期'}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          <form onSubmit={handleRegister} className="flex items-end gap-2">
            <div className="flex flex-1 flex-col gap-1">
              <Label htmlFor="terminal-code">端末コード</Label>
              <Input
                id="terminal-code"
                value={terminalCode}
                onChange={(e) => setTerminalCode(e.target.value)}
                placeholder="POS-001"
                required
              />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <Label htmlFor="terminal-name">端末名</Label>
              <Input
                id="terminal-name"
                value={terminalName}
                onChange={(e) => setTerminalName(e.target.value)}
                placeholder="レジ1"
                required
              />
            </div>
            <Button type="submit">登録</Button>
          </form>
        </div>
      </DialogContent>
    </Dialog>
  )
}
