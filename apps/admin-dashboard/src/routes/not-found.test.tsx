import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { NotFoundPage } from './not-found'

const mockNavigate = vi.fn()

vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

function renderPage() {
  return render(
    <MemoryRouter>
      <NotFoundPage />
    </MemoryRouter>,
  )
}

describe('NotFoundPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('404 コードを表示する', () => {
    renderPage()
    expect(screen.getByText('404')).toBeInTheDocument()
  })

  it('ページが見つかりませんメッセージを表示する', () => {
    renderPage()
    expect(screen.getByText('ページが見つかりません')).toBeInTheDocument()
  })

  it('説明テキストを表示する', () => {
    renderPage()
    expect(
      screen.getByText('お探しのページは存在しないか、移動した可能性があります。'),
    ).toBeInTheDocument()
  })

  it('ダッシュボードに戻るボタンを表示する', () => {
    renderPage()
    expect(screen.getByText('ダッシュボードに戻る')).toBeInTheDocument()
  })

  it('ダッシュボードに戻るボタンで / に遷移する', () => {
    renderPage()
    fireEvent.click(screen.getByText('ダッシュボードに戻る'))
    expect(mockNavigate).toHaveBeenCalledWith('/')
  })
})
