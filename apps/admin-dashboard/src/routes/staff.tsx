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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { api } from '@/lib/api'
import type { Staff, Store, PaginatedResponse } from '@shared-types/openpos'
import { StaffSchema, PaginatedStaffSchema, PaginatedStoresSchema } from '@shared-types/openpos'

const ROLE_LABELS: Record<string, string> = {
  OWNER: 'オーナー',
  MANAGER: 'マネージャー',
  CASHIER: 'キャッシャー',
}

export function StaffPage() {
  const [staff, setStaff] = useState<Staff[]>([])
  const [stores, setStores] = useState<Store[]>([])
  const [selectedStoreId, setSelectedStoreId] = useState<string>('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingStaff, setEditingStaff] = useState<Staff | null>(null)

  useEffect(() => {
    api
      .get<
        PaginatedResponse<Store>
      >('/api/stores', PaginatedStoresSchema, { params: { pageSize: 100 } })
      .then((result) => {
        setStores(result.data)
        const firstStore = result.data[0]
        if (firstStore && !selectedStoreId) {
          setSelectedStoreId(firstStore.id)
        }
      })
  }, [])

  const fetchStaff = useCallback(async () => {
    if (!selectedStoreId) return
    const result = await api.get<PaginatedResponse<Staff>>('/api/staff', PaginatedStaffSchema, {
      params: { storeId: selectedStoreId, page, pageSize: 20 },
    })
    setStaff(result.data)
    setTotalPages(result.pagination.totalPages)
  }, [selectedStoreId, page])

  useEffect(() => {
    fetchStaff()
  }, [fetchStaff])

  function handleStoreChange(storeId: string) {
    setSelectedStoreId(storeId)
    setPage(1)
  }

  function handleCreate() {
    setEditingStaff(null)
    setDialogOpen(true)
  }

  function handleEdit(s: Staff) {
    setEditingStaff(s)
    setDialogOpen(true)
  }

  async function handleSubmit(data: Record<string, unknown>) {
    if (editingStaff) {
      await api.put(`/api/staff/${editingStaff.id}`, data, StaffSchema)
    } else {
      await api.post('/api/staff', data, StaffSchema)
    }
    setDialogOpen(false)
    fetchStaff()
  }

  function getStoreName(storeId: string): string {
    return stores.find((s) => s.id === storeId)?.name ?? '—'
  }

  return (
    <>
      <Header title="スタッフ管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Label className="whitespace-nowrap">店舗:</Label>
            {stores.length > 0 ? (
              <Select value={selectedStoreId} onValueChange={handleStoreChange}>
                <SelectTrigger className="w-[240px]">
                  <SelectValue placeholder="店舗を選択" />
                </SelectTrigger>
                <SelectContent>
                  {stores.map((store) => (
                    <SelectItem key={store.id} value={store.id}>
                      {store.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            ) : (
              <span className="text-sm text-muted-foreground">店舗が未登録です</span>
            )}
          </div>
          <Button onClick={handleCreate} disabled={!selectedStoreId}>
            スタッフを追加
          </Button>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>名前</TableHead>
                <TableHead>メールアドレス</TableHead>
                <TableHead>ロール</TableHead>
                <TableHead>ステータス</TableHead>
                <TableHead>ロック</TableHead>
                <TableHead className="w-[120px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!selectedStoreId ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    店舗を選択してください
                  </TableCell>
                </TableRow>
              ) : staff.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    スタッフが登録されていません
                  </TableCell>
                </TableRow>
              ) : (
                staff.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell className="font-medium">{s.name}</TableCell>
                    <TableCell>{s.email ?? '—'}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{ROLE_LABELS[s.role] ?? s.role}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={s.isActive ? 'default' : 'secondary'}>
                        {s.isActive ? '有効' : '無効'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {s.isLocked && <Badge variant="destructive">ロック中</Badge>}
                    </TableCell>
                    <TableCell>
                      <Button variant="ghost" size="sm" onClick={() => handleEdit(s)}>
                        編集
                      </Button>
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

        <StaffFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          staff={editingStaff}
          storeId={selectedStoreId}
          storeName={getStoreName(selectedStoreId)}
          onSubmit={handleSubmit}
        />
      </div>
    </>
  )
}

function StaffFormDialog({
  open,
  onOpenChange,
  staff,
  storeId,
  storeName,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  staff: Staff | null
  storeId: string
  storeName: string
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<string>('CASHIER')
  const [pin, setPin] = useState('')

  useEffect(() => {
    if (staff) {
      setName(staff.name)
      setEmail(staff.email ?? '')
      setRole(staff.role)
      setPin('')
    } else {
      setName('')
      setEmail('')
      setRole('CASHIER')
      setPin('')
    }
  }, [staff, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const data: Record<string, unknown> = { name, role }
    if (email) data.email = email
    if (pin) data.pin = pin
    if (!staff) {
      data.storeId = storeId
    }
    onSubmit(data)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{staff ? 'スタッフを編集' : 'スタッフを追加'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {!staff && (
            <div className="flex flex-col gap-2">
              <Label>店舗</Label>
              <Input value={storeName} disabled />
            </div>
          )}
          <div className="flex flex-col gap-2">
            <Label htmlFor="staff-name">名前 *</Label>
            <Input
              id="staff-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="staff-email">メールアドレス</Label>
            <Input
              id="staff-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label>ロール *</Label>
              <Select value={role} onValueChange={setRole}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="OWNER">オーナー</SelectItem>
                  <SelectItem value="MANAGER">マネージャー</SelectItem>
                  <SelectItem value="CASHIER">キャッシャー</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="staff-pin">PIN {staff ? '（変更時のみ）' : '*'}</Label>
              <Input
                id="staff-pin"
                type="password"
                inputMode="numeric"
                pattern="\d{4,8}"
                minLength={4}
                maxLength={8}
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                placeholder="4〜8桁の数字"
                required={!staff}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">{staff ? '更新' : '追加'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
