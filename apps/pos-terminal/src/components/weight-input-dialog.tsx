import { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Scale } from 'lucide-react'

interface WeightInputDialogProps {
  open: boolean
  productName: string
  onConfirm: (weightGrams: number) => void
  onCancel: () => void
}

export function WeightInputDialog({
  open,
  productName,
  onConfirm,
  onCancel,
}: WeightInputDialogProps) {
  const [weight, setWeight] = useState('')

  const parsedWeight = Number.parseFloat(weight)
  const isValid = !Number.isNaN(parsedWeight) && parsedWeight > 0

  function handleConfirm() {
    if (isValid) {
      onConfirm(parsedWeight)
      setWeight('')
    }
  }

  function handleCancel() {
    setWeight('')
    onCancel()
  }

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && handleCancel()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Scale className="h-5 w-5" />
            重量入力
          </DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <p className="text-sm text-muted-foreground">
            <span className="font-medium text-foreground">{productName}</span>{' '}
            の重量を入力してください。
          </p>
          <div className="space-y-2">
            <Label htmlFor="weight-input">重量（g）</Label>
            <Input
              id="weight-input"
              type="number"
              inputMode="decimal"
              placeholder="0"
              value={weight}
              onChange={(e) => setWeight(e.target.value)}
              className="text-right text-lg"
              autoFocus
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={handleCancel}>
            キャンセル
          </Button>
          <Button onClick={handleConfirm} disabled={!isValid}>
            確定
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
