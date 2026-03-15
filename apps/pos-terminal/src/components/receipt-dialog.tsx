import { useRef } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Printer } from 'lucide-react'

interface ReceiptDialogProps {
  open: boolean
  receiptData: string | null
  onClose: () => void
}

interface ParsedReceipt {
  storeName: string
  transactionNumber: string
  dateTime: string
  items: Array<{ name: string; quantity: number; unitPrice: number; subtotal: number }>
  subtotal: number
  taxTotal: number
  total: number
  payments: Array<{ method: string; amount: number; received?: number; change?: number }>
  raw: string
}

function parseReceiptData(data: string): ParsedReceipt {
  const lines = data.split('\n').map((line) => line.trim())

  const storeName = lines[0] ?? '店舗名'
  const transactionNumberLine = lines.find((line) => line.startsWith('取引番号:'))
  const transactionNumber = transactionNumberLine?.replace('取引番号:', '').trim() ?? ''
  const dateTimeLine = lines.find((line) => line.startsWith('日時:'))
  const dateTime = dateTimeLine?.replace('日時:', '').trim() ?? ''

  const items: ParsedReceipt['items'] = []
  const itemPattern = /^(.+?)\s+(\d+)\s*x\s*([\d,]+)\s+([\d,]+)$/
  for (const line of lines) {
    const match = itemPattern.exec(line)
    if (match) {
      items.push({
        name: match[1]!.trim(),
        quantity: Number.parseInt(match[2]!, 10),
        unitPrice: Number.parseInt(match[3]!.replace(/,/g, ''), 10),
        subtotal: Number.parseInt(match[4]!.replace(/,/g, ''), 10),
      })
    }
  }

  const extractAmount = (prefix: string): number => {
    const line = lines.find((l) => l.startsWith(prefix))
    if (!line) return 0
    const numMatch = /[\d,]+/.exec(line.replace(prefix, ''))
    return numMatch ? Number.parseInt(numMatch[0].replace(/,/g, ''), 10) : 0
  }

  const subtotal = extractAmount('小計:')
  const taxTotal = extractAmount('税額:')
  const total = extractAmount('合計:')

  const payments: ParsedReceipt['payments'] = []
  const paymentPattern = /^(現金|カード|QR)\s*:\s*([\d,]+)/
  for (const line of lines) {
    const match = paymentPattern.exec(line)
    if (match) {
      payments.push({
        method: match[1]!,
        amount: Number.parseInt(match[2]!.replace(/,/g, ''), 10),
      })
    }
  }

  const receivedLine = lines.find((l) => l.startsWith('お預かり:'))
  if (receivedLine) {
    const numMatch = /[\d,]+/.exec(receivedLine.replace('お預かり:', ''))
    if (numMatch && payments.length > 0) {
      const cashPayment = payments.find((p) => p.method === '現金')
      if (cashPayment) {
        cashPayment.received = Number.parseInt(numMatch[0].replace(/,/g, ''), 10)
      }
    }
  }

  const changeLine = lines.find((l) => l.startsWith('お釣り:'))
  if (changeLine) {
    const numMatch = /[\d,]+/.exec(changeLine.replace('お釣り:', ''))
    if (numMatch && payments.length > 0) {
      const cashPayment = payments.find((p) => p.method === '現金')
      if (cashPayment) {
        cashPayment.change = Number.parseInt(numMatch[0].replace(/,/g, ''), 10)
      }
    }
  }

  return {
    storeName,
    transactionNumber,
    dateTime,
    items,
    subtotal,
    taxTotal,
    total,
    payments,
    raw: data,
  }
}

function formatYen(amount: number): string {
  return `\u00A5${amount.toLocaleString('ja-JP')}`
}

export function ReceiptDialog({ open, receiptData, onClose }: ReceiptDialogProps) {
  const receiptRef = useRef<HTMLDivElement>(null)

  function handlePrint() {
    window.print()
  }

  const parsed = receiptData ? parseReceiptData(receiptData) : null
  const hasStructuredData = parsed && parsed.items.length > 0

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
        <div
          ref={receiptRef}
          id="receipt-print-area"
          className="max-h-[60vh] overflow-auto rounded-lg bg-white p-4"
        >
          {hasStructuredData ? (
            <div className="space-y-3 text-black">
              <div className="text-center">
                <p className="text-base font-bold">{parsed.storeName}</p>
                {parsed.transactionNumber && (
                  <p className="mt-1 font-mono text-xs text-gray-600">
                    取引番号: {parsed.transactionNumber}
                  </p>
                )}
                {parsed.dateTime && (
                  <p className="font-mono text-xs text-gray-600">{parsed.dateTime}</p>
                )}
              </div>

              <Separator className="bg-gray-300" />

              <table className="w-full text-xs">
                <thead>
                  <tr className="border-b border-gray-300">
                    <th className="pb-1 text-left font-medium">商品</th>
                    <th className="pb-1 text-center font-medium">数量</th>
                    <th className="pb-1 text-right font-medium">単価</th>
                    <th className="pb-1 text-right font-medium">小計</th>
                  </tr>
                </thead>
                <tbody>
                  {parsed.items.map((item, index) => (
                    <tr key={index} className="border-b border-dashed border-gray-200">
                      <td className="py-1 pr-2">{item.name}</td>
                      <td className="py-1 text-center">{item.quantity}</td>
                      <td className="py-1 text-right">{formatYen(item.unitPrice)}</td>
                      <td className="py-1 text-right">{formatYen(item.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <Separator className="bg-gray-300" />

              <div className="space-y-1 text-xs">
                <div className="flex justify-between">
                  <span>小計</span>
                  <span>{formatYen(parsed.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>税額</span>
                  <span>{formatYen(parsed.taxTotal)}</span>
                </div>
                <div className="flex justify-between text-sm font-bold">
                  <span>合計</span>
                  <span>{formatYen(parsed.total)}</span>
                </div>
              </div>

              {parsed.payments.length > 0 && (
                <>
                  <Separator className="bg-gray-300" />
                  <div className="space-y-1 text-xs">
                    {parsed.payments.map((payment, index) => (
                      <div key={index} className="flex justify-between">
                        <span>{payment.method}</span>
                        <span>{formatYen(payment.amount)}</span>
                      </div>
                    ))}
                    {parsed.payments.some((p) => p.received != null) && (
                      <div className="flex justify-between">
                        <span>お預かり</span>
                        <span>
                          {formatYen(
                            parsed.payments.find((p) => p.received != null)?.received ?? 0,
                          )}
                        </span>
                      </div>
                    )}
                    {parsed.payments.some((p) => p.change != null && p.change > 0) && (
                      <div className="flex justify-between font-medium">
                        <span>お釣り</span>
                        <span>
                          {formatYen(parsed.payments.find((p) => p.change != null)?.change ?? 0)}
                        </span>
                      </div>
                    )}
                  </div>
                </>
              )}

              <Separator className="bg-gray-300" />
              <p className="text-center text-xs text-gray-500">ありがとうございました</p>
            </div>
          ) : (
            <pre className="whitespace-pre-wrap font-mono text-xs leading-relaxed text-black">
              {receiptData ?? ''}
            </pre>
          )}
        </div>
        <DialogFooter className="flex-col gap-2 sm:flex-col">
          <Button variant="outline" className="w-full gap-2" size="lg" onClick={handlePrint}>
            <Printer className="h-4 w-4" />
            印刷
          </Button>
          <Button className="w-full" size="lg" onClick={onClose}>
            閉じる
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
