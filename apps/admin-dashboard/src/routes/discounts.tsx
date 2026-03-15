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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { api } from '@/lib/api'
import type { Discount, Coupon } from '@shared-types/openpos'
import { DiscountSchema, CouponSchema } from '@shared-types/openpos'
import { z } from 'zod'

export function DiscountsPage() {
  const [discounts, setDiscounts] = useState<Discount[]>([])
  const [coupons, setCoupons] = useState<Coupon[]>([])
  const [discountDialogOpen, setDiscountDialogOpen] = useState(false)
  const [couponDialogOpen, setCouponDialogOpen] = useState(false)
  const [editingDiscount, setEditingDiscount] = useState<Discount | null>(null)

  const fetchDiscounts = useCallback(async () => {
    try {
      const data = await api.get('/api/discounts', z.array(DiscountSchema))
      setDiscounts(data)
    } catch {
      // ignore
    }
  }, [])

  const fetchCoupons = useCallback(async () => {
    try {
      const data = await api.get('/api/coupons', z.array(CouponSchema))
      setCoupons(data)
    } catch {
      // ignore
    }
  }, [])

  useEffect(() => {
    fetchDiscounts()
    fetchCoupons()
  }, [fetchDiscounts, fetchCoupons])

  function handleCreateDiscount() {
    setEditingDiscount(null)
    setDiscountDialogOpen(true)
  }

  function handleEditDiscount(discount: Discount) {
    setEditingDiscount(discount)
    setDiscountDialogOpen(true)
  }

  async function handleDeleteDiscount(id: string) {
    await api.delete(`/api/discounts/${id}`)
    fetchDiscounts()
  }

  async function handleSubmitDiscount(data: Record<string, unknown>) {
    if (editingDiscount) {
      await api.put(`/api/discounts/${editingDiscount.id}`, data, DiscountSchema)
    } else {
      await api.post('/api/discounts', data, DiscountSchema)
    }
    setDiscountDialogOpen(false)
    fetchDiscounts()
  }

  async function handleSubmitCoupon(data: Record<string, unknown>) {
    await api.post('/api/coupons', data, CouponSchema)
    setCouponDialogOpen(false)
    fetchCoupons()
  }

  return (
    <>
      <Header title="割引・クーポン管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <Tabs defaultValue="discounts">
          <TabsList>
            <TabsTrigger value="discounts">割引</TabsTrigger>
            <TabsTrigger value="coupons">クーポン</TabsTrigger>
          </TabsList>

          <TabsContent value="discounts" className="flex flex-col gap-4">
            <div className="flex items-center justify-end">
              <Button onClick={handleCreateDiscount}>割引を追加</Button>
            </div>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>名称</TableHead>
                    <TableHead>種別</TableHead>
                    <TableHead>値</TableHead>
                    <TableHead>期間</TableHead>
                    <TableHead>ステータス</TableHead>
                    <TableHead className="w-[120px]">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {discounts.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        割引が登録されていません
                      </TableCell>
                    </TableRow>
                  ) : (
                    discounts.map((discount) => (
                      <TableRow key={discount.id}>
                        <TableCell className="font-medium">{discount.name}</TableCell>
                        <TableCell>
                          {discount.discountType === 'PERCENTAGE' ? 'パーセント' : '固定額'}
                        </TableCell>
                        <TableCell>
                          {discount.discountType === 'PERCENTAGE'
                            ? `${discount.value}%`
                            : `${Number(discount.value) / 100}円`}
                        </TableCell>
                        <TableCell className="text-sm">
                          {discount.startDate && discount.endDate
                            ? `${discount.startDate.slice(0, 10)} ~ ${discount.endDate.slice(0, 10)}`
                            : '--'}
                        </TableCell>
                        <TableCell>
                          <Badge variant={discount.isActive ? 'default' : 'secondary'}>
                            {discount.isActive ? '有効' : '無効'}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleEditDiscount(discount)}
                            >
                              編集
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDeleteDiscount(discount.id)}
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
          </TabsContent>

          <TabsContent value="coupons" className="flex flex-col gap-4">
            <div className="flex items-center justify-end">
              <Button onClick={() => setCouponDialogOpen(true)}>クーポンを追加</Button>
            </div>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>コード</TableHead>
                    <TableHead>利用回数</TableHead>
                    <TableHead>上限</TableHead>
                    <TableHead>期間</TableHead>
                    <TableHead>ステータス</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {coupons.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground">
                        クーポンが登録されていません
                      </TableCell>
                    </TableRow>
                  ) : (
                    coupons.map((coupon) => (
                      <TableRow key={coupon.id}>
                        <TableCell className="font-mono font-medium">{coupon.code}</TableCell>
                        <TableCell>{coupon.currentUses}</TableCell>
                        <TableCell>{coupon.maxUses ?? '無制限'}</TableCell>
                        <TableCell className="text-sm">
                          {coupon.startDate && coupon.endDate
                            ? `${coupon.startDate.slice(0, 10)} ~ ${coupon.endDate.slice(0, 10)}`
                            : '--'}
                        </TableCell>
                        <TableCell>
                          <Badge variant={coupon.isActive ? 'default' : 'secondary'}>
                            {coupon.isActive ? '有効' : '無効'}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </TabsContent>
        </Tabs>

        <DiscountFormDialog
          open={discountDialogOpen}
          onOpenChange={setDiscountDialogOpen}
          discount={editingDiscount}
          onSubmit={handleSubmitDiscount}
        />

        <CouponFormDialog
          open={couponDialogOpen}
          onOpenChange={setCouponDialogOpen}
          discounts={discounts}
          onSubmit={handleSubmitCoupon}
        />
      </div>
    </>
  )
}

function DiscountFormDialog({
  open,
  onOpenChange,
  discount,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  discount: Discount | null
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [name, setName] = useState('')
  const [discountType, setDiscountType] = useState<'PERCENTAGE' | 'FIXED_AMOUNT'>('PERCENTAGE')
  const [value, setValue] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  useEffect(() => {
    if (discount) {
      setName(discount.name)
      setDiscountType(discount.discountType)
      setValue(discount.value)
      setStartDate(discount.startDate?.slice(0, 10) ?? '')
      setEndDate(discount.endDate?.slice(0, 10) ?? '')
    } else {
      setName('')
      setDiscountType('PERCENTAGE')
      setValue('')
      setStartDate('')
      setEndDate('')
    }
  }, [discount, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      name,
      discountType,
      value,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{discount ? '割引を編集' : '割引を追加'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="discount-name">名称 *</Label>
            <Input
              id="discount-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="discount-type">種別</Label>
              <select
                id="discount-type"
                value={discountType}
                onChange={(e) => setDiscountType(e.target.value as 'PERCENTAGE' | 'FIXED_AMOUNT')}
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
              >
                <option value="PERCENTAGE">パーセント</option>
                <option value="FIXED_AMOUNT">固定額（銭単位）</option>
              </select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="discount-value">値 *</Label>
              <Input
                id="discount-value"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder={discountType === 'PERCENTAGE' ? '10' : '10000'}
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="discount-start">開始日</Label>
              <Input
                id="discount-start"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="discount-end">終了日</Label>
              <Input
                id="discount-end"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">{discount ? '更新' : '追加'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function CouponFormDialog({
  open,
  onOpenChange,
  discounts,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  discounts: Discount[]
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [code, setCode] = useState('')
  const [discountId, setDiscountId] = useState('')
  const [maxUses, setMaxUses] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  useEffect(() => {
    if (open) {
      setCode('')
      setDiscountId(discounts[0]?.id ?? '')
      setMaxUses('')
      setStartDate('')
      setEndDate('')
    }
  }, [open, discounts])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      code,
      discountId,
      maxUses: maxUses ? Number(maxUses) : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>クーポンを追加</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="coupon-code">クーポンコード *</Label>
            <Input
              id="coupon-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="WELCOME2024"
              className="font-mono"
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="coupon-discount">適用割引 *</Label>
              <select
                id="coupon-discount"
                value={discountId}
                onChange={(e) => setDiscountId(e.target.value)}
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
                required
              >
                {discounts.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="coupon-max-uses">利用上限</Label>
              <Input
                id="coupon-max-uses"
                type="number"
                min="0"
                value={maxUses}
                onChange={(e) => setMaxUses(e.target.value)}
                placeholder="無制限"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="coupon-start">開始日</Label>
              <Input
                id="coupon-start"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="coupon-end">終了日</Label>
              <Input
                id="coupon-end"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">追加</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
