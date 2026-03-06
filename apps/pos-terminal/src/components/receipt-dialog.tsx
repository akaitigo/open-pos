import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface ReceiptDialogProps {
  open: boolean
  receiptData: string | null
  onClose: () => void
}

export function ReceiptDialog({ open, receiptData, onClose }: ReceiptDialogProps) {
  return (
    <Dialog
      open={open}
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose()
      }}
    >
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>レシート</DialogTitle>
        </DialogHeader>
        <div className="max-h-[60vh] overflow-auto rounded-lg bg-white p-4">
          <pre className="whitespace-pre-wrap font-mono text-xs leading-relaxed text-black">
            {receiptData ?? ''}
          </pre>
        </div>
        <DialogFooter>
          <Button className="w-full" size="lg" onClick={onClose}>
            次のお客様へ
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
