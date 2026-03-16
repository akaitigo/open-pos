import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { WeightInputDialog } from './weight-input-dialog'

describe('WeightInputDialog', () => {
  const onConfirm = vi.fn()
  const onCancel = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('open=true のとき重量入力ダイアログを表示する', () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    expect(screen.getByText('重量入力')).toBeInTheDocument()
    expect(screen.getByText(/バナナ/)).toBeInTheDocument()
    expect(screen.getByText(/の重量を入力してください。/)).toBeInTheDocument()
    expect(screen.getByLabelText('重量（g）')).toBeInTheDocument()
  })

  it('open=false のときダイアログが表示されない', () => {
    render(
      <WeightInputDialog
        open={false}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    expect(screen.queryByText('重量入力')).not.toBeInTheDocument()
  })

  it('未入力時は確定ボタンが無効', () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    expect(screen.getByRole('button', { name: '確定' })).toBeDisabled()
  })

  it('有効な重量を入力すると確定ボタンが有効になる', async () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    const input = screen.getByLabelText('重量（g）')
    await userEvent.type(input, '250')
    expect(screen.getByRole('button', { name: '確定' })).not.toBeDisabled()
  })

  it('確定ボタンで onConfirm にグラム数を渡す', async () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    const input = screen.getByLabelText('重量（g）')
    await userEvent.type(input, '350')
    await userEvent.click(screen.getByRole('button', { name: '確定' }))
    expect(onConfirm).toHaveBeenCalledWith(350)
  })

  it('キャンセルボタンで onCancel を呼ぶ', async () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'キャンセル' }))
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('0 以下の値では確定ボタンが無効のまま', async () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    const input = screen.getByLabelText('重量（g）')
    await userEvent.type(input, '0')
    expect(screen.getByRole('button', { name: '確定' })).toBeDisabled()
  })

  it('負の値では確定ボタンが無効のまま', async () => {
    render(
      <WeightInputDialog
        open={true}
        productName="バナナ"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )
    const input = screen.getByLabelText('重量（g）')
    await userEvent.type(input, '-100')
    expect(screen.getByRole('button', { name: '確定' })).toBeDisabled()
  })
})
