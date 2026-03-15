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
import { PaginatedStoresSchema, TerminalSchema } from '@shared-types/openpos'
import { z } from 'zod'

export function TerminalsPage() {
  const [stores, setStores] = useState<Store[]>([])
  const [selectedStoreId, setSelectedStoreId] = useState<string>('')
  const [terminals, setTerminals] = useState<Terminal[]>([])
  const [registerDialogOpen, setRegisterDialogOpen] = useState(false)

  const fetchStores = useCallback(async () => {
    const result = await api.get<PaginatedResponse<Store>>('/api/stores', PaginatedStoresSchema, {
      params: { page: 1, pageSize: 100 },
    })
    setStores(result.data)
    const firstStore = result.data[0]
    if (result.data.length > 0 && !selectedStoreId && firstStore) {
      setSelectedStoreId(firstStore.id)
    }
  }, [selectedStoreId])

  useEffect(() => {
    fetchStores()
  }, [fetchStores])

  const fetchTerminals = useCallback(async () => {
    if (!selectedStoreId) return
    const data = await api.get(`/api/stores/${selectedStoreId}/terminals`, z.array(TerminalSchema))
    setTerminals(data)
  }, [selectedStoreId])

  useEffect(() => {
    fetchTerminals()
  }, [fetchTerminals])

  async function handleRegister(data: { terminalCode: string; name: string }) {
    if (!selectedStoreId) return
    await api.post(
      `/api/stores/${selectedStoreId}/terminals`,
      { terminalCode: data.terminalCode, name: data.name },
      TerminalSchema,
    )
    setRegisterDialogOpen(false)
    fetchTerminals()
  }

  return (
    <>
      <Header title="端末管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Label htmlFor="store-select">店舗:</Label>
            <select
              id="store-select"
              value={selectedStoreId}
              onChange={(e) => setSelectedStoreId(e.target.value)}
              className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
            >
              {stores.map((store) => (
                <option key={store.id} value={store.id}>
                  {store.name}
                </option>
              ))}
            </select>
          </div>
          <Button onClick={() => setRegisterDialogOpen(true)}>端末を登録</Button>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>端末コード</TableHead>
                <TableHead>名前</TableHead>
                <TableHead>ステータス</TableHead>
                <TableHead>最終同期</TableHead>
                <TableHead>登録日</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {terminals.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    端末が登録されていません
                  </TableCell>
                </TableRow>
              ) : (
                terminals.map((terminal) => (
                  <TableRow key={terminal.id}>
                    <TableCell className="font-mono">{terminal.terminalCode}</TableCell>
                    <TableCell>{terminal.name ?? '--'}</TableCell>
                    <TableCell>
                      <Badge variant={terminal.isActive ? 'default' : 'secondary'}>
                        {terminal.isActive ? 'オンライン' : 'オフライン'}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {terminal.lastSyncAt ?? '未同期'}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {terminal.createdAt.slice(0, 10)}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        <RegisterTerminalDialog
          open={registerDialogOpen}
          onOpenChange={setRegisterDialogOpen}
          onSubmit={handleRegister}
        />
      </div>
    </>
  )
}

function RegisterTerminalDialog({
  open,
  onOpenChange,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (data: { terminalCode: string; name: string }) => void
}) {
  const [terminalCode, setTerminalCode] = useState('')
  const [name, setName] = useState('')

  useEffect(() => {
    if (open) {
      setTerminalCode('')
      setName('')
    }
  }, [open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({ terminalCode, name })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>端末を登録</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="reg-terminal-code">端末コード *</Label>
            <Input
              id="reg-terminal-code"
              value={terminalCode}
              onChange={(e) => setTerminalCode(e.target.value)}
              placeholder="POS-001"
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="reg-terminal-name">端末名 *</Label>
            <Input
              id="reg-terminal-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="レジ1"
              required
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">登録</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
