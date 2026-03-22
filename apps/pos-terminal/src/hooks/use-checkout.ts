import { useState } from 'react'
import { useCartStore, getCartSubtotal } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { useDiscountStore } from '@/stores/discount-store'
import { api } from '@/lib/api'
import { saveOfflineTransaction } from '@/lib/offline-db'
import type { OfflineTransactionItem, OfflinePayment } from '@/lib/offline-db'
import {
  ApiError,
  FinalizeTransactionResponseSchema,
  TransactionSchema,
  ValidateCouponResponseSchema,
} from '@shared-types/openpos'
import { toast } from '@/hooks/use-toast'
import { useTaxRates } from '@/hooks/use-tax-rates'
import { getCartEstimatedTotal } from '@/lib/cart-totals'

export type PaymentMethod = 'CASH' | 'CREDIT_CARD' | 'QR_CODE'

export interface PaymentEntry {
  id: string
  method: PaymentMethod
  amount: number
  reference?: string
  received?: number
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: '現金',
  CREDIT_CARD: 'カード',
  QR_CODE: 'QR',
}

export const QUICK_CASH_AMOUNTS = [100, 500, 1000, 5000, 10000]
export const CASH_KEYPAD_ROWS = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
  ['00', '0', '⌫'],
]

export function parseYenInput(value: string): number {
  const digits = value.replace(/[^\d]/g, '')
  if (!digits) return 0
  return Number.parseInt(digits, 10) * 100
}

export function ceilToWholeYen(amount: number): number {
  if (amount <= 0) return 0
  return Math.ceil(amount / 100) * 100
}

export function normalizeNumericInput(value: string): string {
  const digits = value.replace(/[^\d]/g, '')
  if (!digits) return ''
  return String(Number.parseInt(digits, 10))
}

function createPaymentEntryId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function useCheckout(onOpenChange: (open: boolean) => void) {
  const items = useCartStore((s) => s.items)
  const clearCart = useCartStore((s) => s.clearCart)
  const { storeId, terminalId, staff } = useAuthStore()

  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH')
  const [paymentAmount, setPaymentAmount] = useState('')
  const [receivedAmount, setReceivedAmount] = useState('')
  const [reference, setReference] = useState('')
  const [addedPayments, setAddedPayments] = useState<PaymentEntry[]>([])
  const [processing, setProcessing] = useState(false)
  const [receiptData, setReceiptData] = useState<string | null>(null)
  const [receiptOpen, setReceiptOpen] = useState(false)
  const [couponCode, setCouponCode] = useState('')
  const [couponValidating, setCouponValidating] = useState(false)
  const taxRates = useTaxRates()
  const appliedDiscounts = useDiscountStore((s) => s.appliedDiscounts)
  const addDiscount = useDiscountStore((s) => s.addDiscount)
  const removeDiscount = useDiscountStore((s) => s.removeDiscount)
  const clearDiscounts = useDiscountStore((s) => s.clearDiscounts)

  const subtotal = getCartSubtotal(items)
  const grossTotal = getCartEstimatedTotal(items, taxRates)
  const taxTotal = grossTotal - subtotal
  const discountTotal = appliedDiscounts.reduce((sum, d) => sum + d.amount, 0)
  const total = Math.max(grossTotal - discountTotal, 0)

  const paidAmount = addedPayments.reduce((sum, payment) => sum + payment.amount, 0)
  const remainingAmount = Math.max(total - paidAmount, 0)
  const roundedRemainingAmount = ceilToWholeYen(remainingAmount)
  const parsedPaymentAmount = parseYenInput(paymentAmount)
  const parsedReceivedAmount = parseYenInput(receivedAmount)

  const currentCoverageAmount =
    paymentMethod === 'CASH'
      ? parsedReceivedAmount >= roundedRemainingAmount
        ? remainingAmount
        : 0
      : Math.min(parsedPaymentAmount, remainingAmount)
  const paymentTotalWithCurrent = paidAmount + currentCoverageAmount
  const remainingAfterCurrent = Math.max(total - paymentTotalWithCurrent, 0)
  const currentChange =
    paymentMethod === 'CASH' && parsedReceivedAmount > roundedRemainingAmount
      ? parsedReceivedAmount - roundedRemainingAmount
      : 0
  const cashShortfall =
    paymentMethod === 'CASH' ? Math.max(roundedRemainingAmount - parsedReceivedAmount, 0) : 0
  const nonCashShortfall =
    paymentMethod !== 'CASH' ? Math.max(total - (paidAmount + parsedPaymentAmount), 0) : 0
  const nonCashOverpayment =
    paymentMethod !== 'CASH' ? Math.max(parsedPaymentAmount - remainingAmount, 0) : 0
  const canAddCurrentPayment =
    paymentMethod !== 'CASH' &&
    parsedPaymentAmount > 0 &&
    parsedPaymentAmount <= remainingAmount &&
    reference.trim().length > 0 &&
    paidAmount < total
  const canFinalize =
    items.length > 0 &&
    (paidAmount >= total ||
      (paymentMethod === 'CASH'
        ? remainingAmount > 0 && parsedReceivedAmount >= roundedRemainingAmount
        : parsedPaymentAmount > 0 &&
          parsedPaymentAmount <= remainingAmount &&
          paidAmount + parsedPaymentAmount >= total))

  function resetDialogState() {
    setPaymentMethod('CASH')
    setPaymentAmount('')
    setReceivedAmount('')
    setReference('')
    setAddedPayments([])
    setCouponCode('')
  }

  async function handleFinalize() {
    if (!storeId || !terminalId || !staff) return

    const payments: Array<{
      method: PaymentMethod
      amount: number
      reference?: string
      received?: number
    }> = addedPayments.map(({ method, amount, reference: paymentReference, received }) => ({
      method,
      amount,
      ...(paymentReference ? { reference: paymentReference } : {}),
      ...(received != null ? { received } : {}),
    }))

    if (payments.reduce((sum, payment) => sum + payment.amount, 0) < total) {
      if (paymentMethod === 'CASH') {
        if (remainingAmount <= 0 || parsedReceivedAmount < roundedRemainingAmount) return
        payments.push({
          method: 'CASH',
          amount: remainingAmount,
          received: parsedReceivedAmount,
        })
      } else {
        if (parsedPaymentAmount <= 0 || parsedPaymentAmount > remainingAmount) return
        payments.push({
          method: paymentMethod,
          amount: parsedPaymentAmount,
          ...(reference.trim() ? { reference: reference.trim() } : {}),
        })
      }
    }

    setProcessing(true)
    try {
      const tx = await api.post(
        '/api/transactions',
        { storeId, terminalId, staffId: staff.id },
        TransactionSchema,
      )

      for (const item of items) {
        await api.post(
          `/api/transactions/${tx.id}/items`,
          { productId: item.product.id, quantity: item.quantity },
          TransactionSchema,
        )
      }

      for (const entry of appliedDiscounts) {
        await api.post(
          `/api/transactions/${tx.id}/discount`,
          { couponCode: entry.couponCode },
          TransactionSchema,
        )
      }

      const result = await api.post(
        `/api/transactions/${tx.id}/finalize`,
        { payments },
        FinalizeTransactionResponseSchema,
      )

      setReceiptData(result.receipt.receiptData)
      setReceiptOpen(true)
      onOpenChange(false)
    } catch (err) {
      const isNetworkError =
        err instanceof TypeError || (!navigator.onLine && !(err instanceof ApiError))
      if (isNetworkError && storeId && terminalId && staff) {
        try {
          const offlineItems: OfflineTransactionItem[] = items.map((item) => ({
            productId: item.product.id,
            productName: item.product.name,
            unitPrice: item.product.price,
            quantity: item.quantity,
            taxRateName: '',
            taxRate: '0',
            isReducedTax: false,
          }))
          const offlinePayments: OfflinePayment[] = payments.map((p) => ({
            method: p.method,
            amount: p.amount,
            ...(p.received != null ? { received: p.received } : {}),
            ...(p.reference ? { reference: p.reference } : {}),
          }))
          await saveOfflineTransaction({
            clientId: crypto.randomUUID(),
            storeId,
            terminalId,
            staffId: staff.id,
            items: offlineItems,
            payments: offlinePayments,
            createdAt: new Date().toISOString(),
            syncStatus: 'pending',
          })
          toast({
            title: 'オフライン保存',
            description:
              'ネットワークに接続できません。取引をローカルに保存しました。オンライン復帰時に自動同期されます。',
          })
          clearCart()
          clearDiscounts()
          resetDialogState()
          onOpenChange(false)
        } catch {
          toast({
            title: 'エラー',
            description: 'オフライン保存にも失敗しました',
            variant: 'destructive',
          })
        }
      } else {
        const message = err instanceof Error ? err.message : '決済処理に失敗しました'
        toast({ title: 'エラー', description: message, variant: 'destructive' })
      }
    } finally {
      setProcessing(false)
    }
  }

  async function handleApplyCoupon() {
    const code = couponCode.trim()
    if (!code) return
    if (appliedDiscounts.some((d) => d.couponCode === code)) {
      toast({ title: 'このクーポンは既に適用されています', variant: 'destructive' })
      return
    }

    setCouponValidating(true)
    try {
      const result = await api.get(
        `/api/coupons/validate/${encodeURIComponent(code)}`,
        ValidateCouponResponseSchema,
      )

      if (!result.isValid || !result.discount) {
        toast({
          title: 'クーポン無効',
          description: result.reason ?? 'このクーポンは使用できません。',
          variant: 'destructive',
        })
        return
      }

      addDiscount(code, result.discount, subtotal)
      setCouponCode('')
      toast({ title: `クーポン「${code}」を適用しました` })
    } catch (err) {
      const message = err instanceof Error ? err.message : 'クーポンの検証に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    } finally {
      setCouponValidating(false)
    }
  }

  function handleReceiptClose() {
    setReceiptOpen(false)
    setReceiptData(null)
    clearCart()
    clearDiscounts()
    resetDialogState()
  }

  function handleClose(isOpen: boolean) {
    if (!processing) {
      onOpenChange(isOpen)
      if (!isOpen) resetDialogState()
    }
  }

  function handleMethodChange(nextMethod: string) {
    const method = nextMethod as PaymentMethod
    setPaymentMethod(method)
    if (method !== 'CASH' && !paymentAmount) {
      setPaymentAmount(String(Math.ceil(remainingAmount / 100)))
    }
  }

  function handleCashKeypadPress(key: string) {
    if (key === '⌫') {
      setReceivedAmount((current) => current.slice(0, -1))
      return
    }

    setReceivedAmount((current) => normalizeNumericInput(`${current}${key}`))
  }

  function handleAddCurrentPayment() {
    if (!canAddCurrentPayment) return

    setAddedPayments((current) => [
      ...current,
      {
        id: createPaymentEntryId(),
        method: paymentMethod,
        amount: parsedPaymentAmount,
        reference: reference.trim(),
      },
    ])
    setPaymentAmount('')
    setReference('')
    setPaymentMethod('CASH')
  }

  function handleRemovePayment(paymentId: string) {
    setAddedPayments((current) => current.filter((payment) => payment.id !== paymentId))
  }

  return {
    // State
    items,
    paymentMethod,
    paymentAmount,
    receivedAmount,
    reference,
    addedPayments,
    processing,
    receiptData,
    receiptOpen,
    couponCode,
    couponValidating,
    appliedDiscounts,

    // Computed
    subtotal,
    grossTotal,
    taxTotal,
    discountTotal,
    total,
    paidAmount,
    remainingAmount,
    roundedRemainingAmount,
    parsedPaymentAmount,
    parsedReceivedAmount,
    remainingAfterCurrent,
    currentChange,
    cashShortfall,
    nonCashShortfall,
    nonCashOverpayment,
    canAddCurrentPayment,
    canFinalize,

    // Setters
    setPaymentAmount,
    setReceivedAmount,
    setReference,
    setCouponCode,

    // Handlers
    handleFinalize,
    handleApplyCoupon,
    handleReceiptClose,
    handleClose,
    handleMethodChange,
    handleCashKeypadPress,
    handleAddCurrentPayment,
    handleRemovePayment,
    removeDiscount,
  }
}
