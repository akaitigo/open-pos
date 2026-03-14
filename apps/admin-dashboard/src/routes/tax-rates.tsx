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
import { ConfirmDialog } from '@/components/confirm-dialog'
import { api } from '@/lib/api'
import type { TaxRate } from '@shared-types/openpos'
import { TaxRateSchema } from '@shared-types/openpos'
import { z } from 'zod'

export function TaxRatesPage() {
  const [taxRates, setTaxRates] = useState<TaxRate[]>([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingTaxRate, setEditingTaxRate] = useState<TaxRate | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)

  const fetchTaxRates = useCallback(async () => {
    const result = await api.get('/api/tax-rates', z.array(TaxRateSchema))
    setTaxRates(result)
  }, [])

  useEffect(() => {
    fetchTaxRates()
  }, [fetchTaxRates])

  function handleCreate() {
    setEditingTaxRate(null)
    setDialogOpen(true)
  }

  function handleEdit(taxRate: TaxRate) {
    setEditingTaxRate(taxRate)
    setDialogOpen(true)
  }

  async function handleDelete(id: string) {
    await api.delete(`/api/tax-rates/${id}`)
    fetchTaxRates()
  }

  async function handleSubmit(data: Record<string, unknown>) {
    if (editingTaxRate) {
      await api.put(`/api/tax-rates/${editingTaxRate.id}`, data, TaxRateSchema)
    } else {
      await api.post('/api/tax-rates', data, TaxRateSchema)
    }
    setDialogOpen(false)
    fetchTaxRates()
  }

  function formatRate(rate: string): string {
    const num = Number(rate)
    return `${(num * 100).toFixed(0)}%`
  }

  return (
    <>
      <Header title="税率管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-end">
          <Button onClick={handleCreate}>税率を追加</Button>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>税率名</TableHead>
                <TableHead className="text-right">税率</TableHead>
                <TableHead>軽減税率</TableHead>
                <TableHead>デフォルト</TableHead>
                <TableHead className="w-[120px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {taxRates.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    税率が登録されていません
                  </TableCell>
                </TableRow>
              ) : (
                taxRates.map((taxRate) => (
                  <TableRow key={taxRate.id}>
                    <TableCell className="font-medium">{taxRate.name}</TableCell>
                    <TableCell className="text-right font-mono">
                      {formatRate(taxRate.rate)}
                    </TableCell>
                    <TableCell>
                      {taxRate.isReduced && <Badge variant="outline">軽減</Badge>}
                    </TableCell>
                    <TableCell>
                      {taxRate.isDefault && <Badge variant="default">デフォルト</Badge>}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" onClick={() => handleEdit(taxRate)}>
                          編集
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setDeleteTarget(taxRate.id)}
                        >
                          削除
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        <TaxRateFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          taxRate={editingTaxRate}
          onSubmit={handleSubmit}
        />

        <ConfirmDialog
          open={deleteTarget !== null}
          onOpenChange={(open) => {
            if (!open) setDeleteTarget(null)
          }}
          title="税率を削除"
          description="本当に削除しますか？この操作は取り消せません。"
          onConfirm={() => {
            if (deleteTarget) handleDelete(deleteTarget)
            setDeleteTarget(null)
          }}
        />
      </div>
    </>
  )
}

function TaxRateFormDialog({
  open,
  onOpenChange,
  taxRate,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  taxRate: TaxRate | null
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [name, setName] = useState('')
  const [ratePercent, setRatePercent] = useState('')
  const [isReduced, setIsReduced] = useState(false)
  const [isDefault, setIsDefault] = useState(false)

  useEffect(() => {
    if (taxRate) {
      setName(taxRate.name)
      setRatePercent(String(Number(taxRate.rate) * 100))
      setIsReduced(taxRate.isReduced)
      setIsDefault(taxRate.isDefault)
    } else {
      setName('')
      setRatePercent('')
      setIsReduced(false)
      setIsDefault(false)
    }
  }, [taxRate, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const rateDecimal = String(Number(ratePercent) / 100)
    onSubmit({
      name,
      rate: rateDecimal,
      isReduced,
      isDefault,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{taxRate ? '税率を編集' : '税率を追加'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="tax-name">税率名 *</Label>
            <Input
              id="tax-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="標準税率"
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="tax-rate">税率（%） *</Label>
            <Input
              id="tax-rate"
              type="number"
              min="0"
              max="100"
              step="0.1"
              value={ratePercent}
              onChange={(e) => setRatePercent(e.target.value)}
              placeholder="10"
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex items-center gap-2">
              <input
                id="tax-reduced"
                type="checkbox"
                checked={isReduced}
                onChange={(e) => setIsReduced(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300"
              />
              <Label htmlFor="tax-reduced">軽減税率</Label>
            </div>
            <div className="flex items-center gap-2">
              <input
                id="tax-default"
                type="checkbox"
                checked={isDefault}
                onChange={(e) => setIsDefault(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300"
              />
              <Label htmlFor="tax-default">デフォルト</Label>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">{taxRate ? '更新' : '追加'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
