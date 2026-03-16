import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { AgeConfirmDialog } from './age-confirm-dialog'

describe('AgeConfirmDialog', () => {
  const onConfirm = vi.fn()
  const onCancel = vi.fn()

  it('open=true のとき年齢確認ダイアログを表示する', () => {
    render(
      <AgeConfirmDialog
        open={true}
        productName="ビール"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    expect(screen.getByText('年齢確認')).toBeInTheDocument()
    expect(screen.getByText(/ビール/)).toBeInTheDocument()
    expect(screen.getByText('は年齢確認が必要な商品です。')).toBeTruthy()
    expect(screen.getByText('お客様は20歳以上ですか？')).toBeInTheDocument()
  })

  it('open=false のときダイアログが表示されない', () => {
    render(
      <AgeConfirmDialog
        open={false}
        productName="ビール"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    expect(screen.queryByText('年齢確認')).not.toBeInTheDocument()
  })

  it('「はい（20歳以上）」ボタンで onConfirm を呼ぶ', async () => {
    render(
      <AgeConfirmDialog
        open={true}
        productName="ビール"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'はい（20歳以上）' }))
    expect(onConfirm).toHaveBeenCalledOnce()
  })

  it('「いいえ」ボタンで onCancel を呼ぶ', async () => {
    render(
      <AgeConfirmDialog
        open={true}
        productName="ビール"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'いいえ' }))
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('商品名が正しく表示される', () => {
    render(
      <AgeConfirmDialog
        open={true}
        productName="日本酒"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    expect(screen.getByText('日本酒')).toBeInTheDocument()
  })
})
