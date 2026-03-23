import type React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ErrorBoundary } from './error-boundary'

vi.mock('@/lib/error-reporter', () => ({
  reportError: vi.fn(),
}))

function ThrowingChild({ error }: { error: Error }): React.ReactNode {
  throw error
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('正常時は children をレンダリングする', () => {
    render(
      <ErrorBoundary>
        <p>正常なコンテンツ</p>
      </ErrorBoundary>,
    )
    expect(screen.getByText('正常なコンテンツ')).toBeInTheDocument()
  })

  it('エラー時にデフォルトのエラー画面を表示する', () => {
    render(
      <ErrorBoundary>
        <ThrowingChild error={new Error('テストエラー')} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('エラーが発生しました')).toBeInTheDocument()
    expect(screen.getByText('テストエラー')).toBeInTheDocument()
  })

  it('カスタム fallback を表示できる', () => {
    render(
      <ErrorBoundary fallback={<p>カスタムエラー画面</p>}>
        <ThrowingChild error={new Error('テスト')} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('カスタムエラー画面')).toBeInTheDocument()
  })

  it('再試行ボタンで children を再レンダリングする', async () => {
    const user = userEvent.setup()
    let shouldThrow = true

    function ConditionalChild() {
      if (shouldThrow) {
        throw new Error('初回エラー')
      }
      return <p>復旧しました</p>
    }

    render(
      <ErrorBoundary>
        <ConditionalChild />
      </ErrorBoundary>,
    )

    expect(screen.getByText('エラーが発生しました')).toBeInTheDocument()

    shouldThrow = false
    await user.click(screen.getByText('再試行'))

    expect(screen.getByText('復旧しました')).toBeInTheDocument()
  })

  it('ページを再読み込みボタンが表示される', () => {
    render(
      <ErrorBoundary>
        <ThrowingChild error={new Error('テスト')} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('ページを再読み込み')).toBeInTheDocument()
  })
})
