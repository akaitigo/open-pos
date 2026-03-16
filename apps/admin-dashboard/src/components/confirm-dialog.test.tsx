import { render, screen, fireEvent } from '@testing-library/react'
import { ConfirmDialog } from './confirm-dialog'

describe('ConfirmDialog', () => {
  const defaultProps = {
    open: true,
    onOpenChange: vi.fn(),
    onConfirm: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('デフォルトのタイトルと説明文を表示する', () => {
    render(<ConfirmDialog {...defaultProps} />)
    expect(screen.getByText('確認')).toBeInTheDocument()
    expect(screen.getByText('本当に削除しますか？この操作は取り消せません。')).toBeInTheDocument()
  })

  it('カスタムタイトルと説明文を表示する', () => {
    render(
      <ConfirmDialog {...defaultProps} title="カスタムタイトル" description="カスタム説明文" />,
    )
    expect(screen.getByText('カスタムタイトル')).toBeInTheDocument()
    expect(screen.getByText('カスタム説明文')).toBeInTheDocument()
  })

  it('デフォルトのボタンラベルを表示する', () => {
    render(<ConfirmDialog {...defaultProps} />)
    expect(screen.getByText('削除')).toBeInTheDocument()
    expect(screen.getByText('キャンセル')).toBeInTheDocument()
  })

  it('カスタムボタンラベルを表示する', () => {
    render(<ConfirmDialog {...defaultProps} confirmLabel="承認" cancelLabel="やめる" />)
    expect(screen.getByText('承認')).toBeInTheDocument()
    expect(screen.getByText('やめる')).toBeInTheDocument()
  })

  it('確認ボタンを押すと onConfirm と onOpenChange(false) が呼ばれる', () => {
    render(<ConfirmDialog {...defaultProps} />)
    fireEvent.click(screen.getByText('削除'))
    expect(defaultProps.onConfirm).toHaveBeenCalledTimes(1)
    expect(defaultProps.onOpenChange).toHaveBeenCalledWith(false)
  })

  it('キャンセルボタンを押すと onOpenChange(false) が呼ばれる', () => {
    render(<ConfirmDialog {...defaultProps} />)
    fireEvent.click(screen.getByText('キャンセル'))
    expect(defaultProps.onOpenChange).toHaveBeenCalledWith(false)
  })

  it('open が false の場合はダイアログを表示しない', () => {
    render(<ConfirmDialog {...defaultProps} open={false} />)
    expect(screen.queryByText('確認')).not.toBeInTheDocument()
  })
})
