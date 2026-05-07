import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { t } from '@/i18n'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

interface BarcodeScannerDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onScanned: (barcode: string) => void
}

export function BarcodeScannerDialog({ open, onOpenChange, onScanned }: BarcodeScannerDialogProps) {
  const scannerRef = useRef<Html5Qrcode | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [manualBarcode, setManualBarcode] = useState('')

  useEffect(() => {
    if (!open) return

    const scannerId = 'barcode-scanner'
    let scanner: Html5Qrcode | null = null

    const startScanner = async () => {
      try {
        scanner = new Html5Qrcode(scannerId)
        scannerRef.current = scanner
        await scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 150 } },
          (decodedText) => {
            onScanned(decodedText)
          },
          () => {
            // scan failure — ignore (continuous scanning)
          },
        )
      } catch {
        setError('カメラにアクセスできません。手動でバーコードを入力してください。')
      }
    }

    const timer = setTimeout(startScanner, 100)

    return () => {
      clearTimeout(timer)
      if (scanner?.isScanning) {
        scanner.stop().catch(() => {})
      }
      scannerRef.current = null
      setError(null)
    }
  }, [open, onScanned])

  function handleManualSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (manualBarcode.trim()) {
      onScanned(manualBarcode.trim())
      setManualBarcode('')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>バーコードスキャン</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          {error ? (
            <p className="text-sm text-destructive">{error}</p>
          ) : (
            <div id="barcode-scanner" className="w-full overflow-hidden rounded-md" />
          )}

          <form onSubmit={handleManualSubmit} className="flex items-end gap-2">
            <label htmlFor="manual-barcode-input" className="sr-only">
              {t('products.barcodeManual')}
            </label>
            <Input
              id="manual-barcode-input"
              placeholder="バーコードを手入力..."
              value={manualBarcode}
              onChange={(event) => setManualBarcode(event.target.value)}
              className="flex-1"
            />
            <Button type="submit" variant="outline">
              検索
            </Button>
          </form>
        </div>
      </DialogContent>
    </Dialog>
  )
}
