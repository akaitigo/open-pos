import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ShieldAlert } from 'lucide-react'

interface AgeConfirmDialogProps {
  open: boolean
  productName: string
  onConfirm: () => void
  onCancel: () => void
}

export function AgeConfirmDialog({
  open,
  productName,
  onConfirm,
  onCancel,
}: AgeConfirmDialogProps) {
  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onCancel()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-amber-500" />
            年齢確認
          </DialogTitle>
        </DialogHeader>
        <div className="space-y-3 py-2">
          <p className="text-sm">
            <span className="font-medium">{productName}</span> は年齢確認が必要な商品です。
          </p>
          <p className="text-sm text-muted-foreground">お客様は20歳以上ですか？</p>
        </div>
        <DialogFooter className="flex-row gap-2">
          <Button variant="outline" className="flex-1" onClick={onCancel}>
            いいえ
          </Button>
          <Button className="flex-1" onClick={onConfirm}>
            はい（20歳以上）
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
