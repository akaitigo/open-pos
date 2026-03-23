import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { ConfirmDialog } from './confirm-dialog'

describe('ConfirmDialog', () => {
  it('open=true のときダイアログを表示する', () => {
    render(<ConfirmDialog open={true} onOpenChange={vi.fn()} onConfirm={vi.fn()} />)
    expect(screen.getByText('確認')).toBeInTheDocument()
    expect(screen.getByText('本当に削除しますか？この操作は取り消せません。')).toBeInTheDocument()
  })

  it('カスタムタイトルと説明文を表示する', () => {
    render(
      <ConfirmDialog
        open={true}
        onOpenChange={vi.fn()}
        onConfirm={vi.fn()}
        title="カスタムタイトル"
        description="カスタム説明"
      />,
    )
    expect(screen.getByText('カスタムタイトル')).toBeInTheDocument()
    expect(screen.getByText('カスタム説明')).toBeInTheDocument()
  })

  it('確認ボタンクリックで onConfirm と onOpenChange(false) が呼ばれる', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()
    const onOpenChange = vi.fn()

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={onOpenChange}
        onConfirm={onConfirm}
        confirmLabel="実行"
      />,
    )

    await user.click(screen.getByText('実行'))
    expect(onConfirm).toHaveBeenCalledOnce()
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('キャンセルボタンクリックで onOpenChange(false) が呼ばれる', async () => {
    const user = userEvent.setup()
    const onOpenChange = vi.fn()

    render(
      <ConfirmDialog
        open={true}
        onOpenChange={onOpenChange}
        onConfirm={vi.fn()}
        cancelLabel="やめる"
      />,
    )

    await user.click(screen.getByText('やめる'))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})
