import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { PaymentCashTab } from './payment-cash-tab'

const defaultProps = {
  remainingAmount: 50000,
  receivedAmount: '',
  setReceivedAmount: vi.fn(),
  handleCashKeypadPress: vi.fn(),
  cashShortfall: 0,
  parsedReceivedAmount: 0,
  roundedRemainingAmount: 50000,
  currentChange: 0,
}

describe('PaymentCashTab', () => {
  it('残額と入力フィールドを表示する', () => {
    render(<PaymentCashTab {...defaultProps} />)
    expect(screen.getByText('今回の現金充当額')).toBeInTheDocument()
    expect(screen.getByText('お預かり金額（円）')).toBeInTheDocument()
  })

  it('クイック金額ボタンを表示する', () => {
    render(<PaymentCashTab {...defaultProps} />)
    expect(screen.getByText('ぴったり')).toBeInTheDocument()
    expect(screen.getByText('¥1,000')).toBeInTheDocument()
    expect(screen.getByText('¥5,000')).toBeInTheDocument()
    expect(screen.getByText('¥10,000')).toBeInTheDocument()
  })

  it('クイック金額ボタンクリックで setReceivedAmount が呼ばれる', () => {
    const setReceivedAmount = vi.fn()
    render(<PaymentCashTab {...defaultProps} setReceivedAmount={setReceivedAmount} />)
    fireEvent.click(screen.getByText('¥1,000'))
    expect(setReceivedAmount).toHaveBeenCalledWith('1000')
  })

  it('ぴったりボタンで端数切り上げ金額がセットされる', () => {
    const setReceivedAmount = vi.fn()
    render(
      <PaymentCashTab
        {...defaultProps}
        remainingAmount={50050}
        setReceivedAmount={setReceivedAmount}
      />,
    )
    fireEvent.click(screen.getByText('ぴったり'))
    expect(setReceivedAmount).toHaveBeenCalledWith('501')
  })

  it('Cボタンで金額がクリアされる', () => {
    const setReceivedAmount = vi.fn()
    render(<PaymentCashTab {...defaultProps} setReceivedAmount={setReceivedAmount} />)
    fireEvent.click(screen.getByLabelText('金額をクリア'))
    expect(setReceivedAmount).toHaveBeenCalledWith('')
  })

  it('不足表示が出る（cashShortfall > 0 かつ parsedReceivedAmount > 0）', () => {
    render(<PaymentCashTab {...defaultProps} cashShortfall={10000} parsedReceivedAmount={40000} />)
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText(/不足：/)).toBeInTheDocument()
  })

  it('不足なし時はアラートが表示されない', () => {
    render(<PaymentCashTab {...defaultProps} cashShortfall={0} parsedReceivedAmount={50000} />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('お釣り表示が出る（十分な支払い時）', () => {
    render(
      <PaymentCashTab
        {...defaultProps}
        parsedReceivedAmount={60000}
        roundedRemainingAmount={50000}
        remainingAmount={50000}
        currentChange={10000}
      />,
    )
    expect(screen.getByText('お釣り')).toBeInTheDocument()
  })

  it('テンキーボタンで handleCashKeypadPress が呼ばれる', () => {
    const handleCashKeypadPress = vi.fn()
    render(<PaymentCashTab {...defaultProps} handleCashKeypadPress={handleCashKeypadPress} />)
    fireEvent.click(screen.getByText('5'))
    expect(handleCashKeypadPress).toHaveBeenCalledWith('5')
  })

  it('入力フィールド変更で setReceivedAmount が呼ばれる', () => {
    const setReceivedAmount = vi.fn()
    render(<PaymentCashTab {...defaultProps} setReceivedAmount={setReceivedAmount} />)
    fireEvent.change(screen.getByPlaceholderText('0'), { target: { value: '500' } })
    expect(setReceivedAmount).toHaveBeenCalledWith('500')
  })
})
