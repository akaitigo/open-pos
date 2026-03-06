import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { ReceiptDialog } from './receipt-dialog'

describe('ReceiptDialog', () => {
  it('レシートタイトルを表示する', () => {
    render(<ReceiptDialog open={true} receiptData="テストレシート" onClose={vi.fn()} />)
    expect(screen.getByText('レシート')).toBeInTheDocument()
  })

  it('レシートデータを表示する', () => {
    render(<ReceiptDialog open={true} receiptData="テストレシート" onClose={vi.fn()} />)
    expect(screen.getByText('テストレシート')).toBeInTheDocument()
  })

  it('「次のお客様へ」ボタンで onClose が呼ばれる', async () => {
    const onClose = vi.fn()
    render(<ReceiptDialog open={true} receiptData="test" onClose={onClose} />)
    await userEvent.click(screen.getByText('次のお客様へ'))
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('open=false のとき何も表示しない', () => {
    render(<ReceiptDialog open={false} receiptData="test" onClose={vi.fn()} />)
    expect(screen.queryByText('レシート')).not.toBeInTheDocument()
  })
})
