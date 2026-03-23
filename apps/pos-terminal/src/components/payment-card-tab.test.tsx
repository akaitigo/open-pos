import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { PaymentCardTab } from './payment-card-tab'

const defaultProps = {
  mode: 'CREDIT_CARD' as const,
  paymentAmount: '',
  setPaymentAmount: vi.fn(),
  reference: '',
  setReference: vi.fn(),
  remainingAmount: 50000,
  nonCashShortfall: 0,
  nonCashOverpayment: 0,
  parsedPaymentAmount: 0,
}

describe('PaymentCardTab', () => {
  it('カードモードでカードアイコンとテキストを表示する', () => {
    render(<PaymentCardTab {...defaultProps} mode="CREDIT_CARD" />)
    expect(screen.getByText('カード端末プレースホルダー')).toBeInTheDocument()
  })

  it('QRモードでQRコードとテキストを表示する', () => {
    render(<PaymentCardTab {...defaultProps} mode="QR_CODE" />)
    expect(screen.getByText('QR 決済プレースホルダー')).toBeInTheDocument()
    expect(screen.getByText('OPENPOS-QR')).toBeInTheDocument()
  })

  it('残額ボタンで setPaymentAmount が呼ばれる', () => {
    const setPaymentAmount = vi.fn()
    render(
      <PaymentCardTab
        {...defaultProps}
        remainingAmount={50000}
        setPaymentAmount={setPaymentAmount}
      />,
    )
    fireEvent.click(screen.getByText('残額'))
    expect(setPaymentAmount).toHaveBeenCalledWith('500')
  })

  it('nonCashShortfall > 0 で残額メッセージを表示する', () => {
    render(
      <PaymentCardTab {...defaultProps} nonCashShortfall={20000} parsedPaymentAmount={30000} />,
    )
    expect(screen.getByText(/残ります/)).toBeInTheDocument()
  })

  it('nonCashOverpayment > 0 で超過メッセージを表示する', () => {
    render(<PaymentCardTab {...defaultProps} nonCashOverpayment={10000} />)
    expect(screen.getByText(/残額を超える金額は追加できません/)).toBeInTheDocument()
  })

  it('参照番号入力で setReference が呼ばれる', () => {
    const setReference = vi.fn()
    render(<PaymentCardTab {...defaultProps} setReference={setReference} />)
    fireEvent.change(screen.getByPlaceholderText('カード承認番号'), {
      target: { value: 'AUTH-001' },
    })
    expect(setReference).toHaveBeenCalledWith('AUTH-001')
  })

  it('QRモードでは参照番号のプレースホルダーが決済IDになる', () => {
    render(<PaymentCardTab {...defaultProps} mode="QR_CODE" />)
    expect(screen.getByPlaceholderText('決済ID')).toBeInTheDocument()
  })
})
