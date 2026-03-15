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

  it('「閉じる」ボタンで onClose が呼ばれる', async () => {
    const onClose = vi.fn()
    render(<ReceiptDialog open={true} receiptData="test" onClose={onClose} />)
    await userEvent.click(screen.getByText('閉じる'))
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('「印刷」ボタンが表示される', () => {
    render(<ReceiptDialog open={true} receiptData="test" onClose={vi.fn()} />)
    expect(screen.getByText('印刷')).toBeInTheDocument()
  })

  it('「印刷」ボタンをクリックすると window.print が呼ばれる', async () => {
    const printSpy = vi.spyOn(window, 'print').mockImplementation(() => {})
    render(<ReceiptDialog open={true} receiptData="test" onClose={vi.fn()} />)
    await userEvent.click(screen.getByText('印刷'))
    expect(printSpy).toHaveBeenCalledTimes(1)
    printSpy.mockRestore()
  })

  it('open=false のとき何も表示しない', () => {
    render(<ReceiptDialog open={false} receiptData="test" onClose={vi.fn()} />)
    expect(screen.queryByText('レシート')).not.toBeInTheDocument()
  })

  it('構造化されたレシートデータをテーブル形式で表示する', () => {
    const receiptData = [
      '渋谷本店',
      '取引番号: TX-001',
      '日時: 2026-03-15 14:30',
      '',
      'ドリップコーヒー 2 x 150 300',
      'サンドイッチ 1 x 500 500',
      '',
      '小計: 800',
      '税額: 80',
      '合計: 880',
      '',
      '現金: 880',
    ].join('\n')

    render(<ReceiptDialog open={true} receiptData={receiptData} onClose={vi.fn()} />)

    expect(screen.getByText('渋谷本店')).toBeInTheDocument()
    expect(screen.getByText('取引番号: TX-001')).toBeInTheDocument()
    expect(screen.getByText('ドリップコーヒー')).toBeInTheDocument()
    expect(screen.getByText('サンドイッチ')).toBeInTheDocument()
    expect(screen.getByText('ありがとうございました')).toBeInTheDocument()
  })

  it('構造化できないレシートはプレーンテキストで表示する', () => {
    const receiptData = 'シンプルなテキストレシート'
    render(<ReceiptDialog open={true} receiptData={receiptData} onClose={vi.fn()} />)
    expect(screen.getByText('シンプルなテキストレシート')).toBeInTheDocument()
  })

  it('receiptData が null の場合は空のプレーンテキストを表示する', () => {
    render(<ReceiptDialog open={true} receiptData={null} onClose={vi.fn()} />)
    expect(screen.getByText('レシート')).toBeInTheDocument()
  })
})
