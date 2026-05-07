import { useEffect, useState } from 'react'
import { t } from '@/i18n'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

interface OpenPriceDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  productName: string
  onSubmit: (priceInSen: number) => void
}

export function OpenPriceDialog({
  open,
  onOpenChange,
  productName,
  onSubmit,
}: OpenPriceDialogProps) {
  const [priceInput, setPriceInput] = useState('')

  useEffect(() => {
    if (open) {
      setPriceInput('')
    }
  }, [open])

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    const yen = Number(priceInput)
    if (!Number.isFinite(yen) || yen <= 0) return
    onSubmit(Math.round(yen * 100))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>価格入力 — {productName}</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground">
          オープンプライス商品です。販売価格を入力してください。
        </p>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label htmlFor="open-price-input" className="text-sm font-medium">
              {t('accessibility.openPriceLabelYen')}
            </label>
            <Input
              id="open-price-input"
              type="number"
              min="1"
              step="1"
              placeholder="0"
              value={priceInput}
              onChange={(event) => setPriceInput(event.target.value)}
              autoFocus
              required
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">カートに追加</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
