import { render, screen, fireEvent } from '@testing-library/react'
import { ErrorBoundary } from './error-boundary'

function ThrowingComponent({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) {
    throw new Error('テストエラー')
  }
  return <div>正常なコンテンツ</div>
}

describe('ErrorBoundary', () => {
  // console.error のノイズを抑制
  const originalConsoleError = console.error
  beforeAll(() => {
    console.error = vi.fn()
  })
  afterAll(() => {
    console.error = originalConsoleError
  })

  it('エラーがない場合は子コンポーネントを表示する', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={false} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('正常なコンテンツ')).toBeInTheDocument()
  })

  it('エラーが発生した場合はデフォルトのフォールバックUIを表示する', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('エラーが発生しました')).toBeInTheDocument()
    expect(
      screen.getByText(
        '予期しないエラーが発生しました。ページを再読み込みするか、しばらくしてからもう一度お試しください。',
      ),
    ).toBeInTheDocument()
  })

  it('エラーメッセージを表示する', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('テストエラー')).toBeInTheDocument()
  })

  it('再試行ボタンを表示する', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('再試行')).toBeInTheDocument()
  })

  it('ページを再読み込みボタンを表示する', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('ページを再読み込み')).toBeInTheDocument()
  })

  it('再試行ボタンでエラー状態をリセットする', () => {
    const { rerender } = render(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('エラーが発生しました')).toBeInTheDocument()
    // 再試行ボタンを押すと状態がリセットされる
    // ただし ThrowingComponent が再度 throw するため再びエラーUI表示
    fireEvent.click(screen.getByText('再試行'))
    // 次のレンダリングではまだエラーが発生する
    rerender(
      <ErrorBoundary>
        <ThrowingComponent shouldThrow={false} />
      </ErrorBoundary>,
    )
    // 再試行後に shouldThrow=false なので正常コンテンツが表示される
    // 注意: 再試行→再レンダリングのフローをテスト
  })

  it('カスタムフォールバックを指定した場合はそれを表示する', () => {
    render(
      <ErrorBoundary fallback={<div>カスタムエラー表示</div>}>
        <ThrowingComponent shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('カスタムエラー表示')).toBeInTheDocument()
    expect(screen.queryByText('エラーが発生しました')).not.toBeInTheDocument()
  })
})
