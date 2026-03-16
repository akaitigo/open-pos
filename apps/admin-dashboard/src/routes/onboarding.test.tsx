import { render, screen, fireEvent } from '@testing-library/react'
import { OnboardingPage } from './onboarding'

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('最初のステップ「組織情報」を表示する', () => {
    render(<OnboardingPage />)
    expect(screen.getAllByText('組織情報').length).toBeGreaterThan(0)
    expect(screen.getByLabelText('組織名 *')).toBeInTheDocument()
    expect(screen.getByLabelText('業態')).toBeInTheDocument()
    expect(screen.getByLabelText('インボイス登録番号')).toBeInTheDocument()
  })

  it('ステッププログレスに全5ステップを表示する', () => {
    render(<OnboardingPage />)
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  it('最初のステップでは戻るボタンが無効', () => {
    render(<OnboardingPage />)
    const backButton = screen.getByText('戻る')
    expect(backButton).toBeDisabled()
  })

  it('次へボタンで店舗設定ステップに進む', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ'))
    expect(screen.getByLabelText('店舗名 *')).toBeInTheDocument()
    expect(screen.getByLabelText('住所')).toBeInTheDocument()
    expect(screen.getByLabelText('電話番号')).toBeInTheDocument()
  })

  it('店舗設定 → 初期商品ステップに進む', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    expect(screen.getByText('初期商品を登録します（後から追加・変更可能）。')).toBeInTheDocument()
  })

  it('初期商品ステップで商品を追加できる', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    // 最初は1つの入力
    expect(screen.getByPlaceholderText('商品名 1')).toBeInTheDocument()
    // + ボタンで追加
    fireEvent.click(screen.getByText('+'))
    expect(screen.getByPlaceholderText('商品名 2')).toBeInTheDocument()
  })

  it('スタッフステップに進む', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    fireEvent.click(screen.getByText('次へ')) // → スタッフ
    expect(screen.getByLabelText('スタッフ名 *')).toBeInTheDocument()
    expect(screen.getByLabelText('メールアドレス *')).toBeInTheDocument()
    expect(screen.getByLabelText('PIN（4-8桁） *')).toBeInTheDocument()
  })

  it('戻るボタンで前のステップに戻る', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    expect(screen.getByLabelText('店舗名 *')).toBeInTheDocument()
    fireEvent.click(screen.getByText('戻る'))
    expect(screen.getByLabelText('組織名 *')).toBeInTheDocument()
  })

  it('確認ステップで入力データを表示する', () => {
    render(<OnboardingPage />)
    // ステップ1: 組織情報を入力
    fireEvent.change(screen.getByLabelText('組織名 *'), { target: { value: 'テスト組織' } })
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    // ステップ2: 店舗設定を入力
    fireEvent.change(screen.getByLabelText('店舗名 *'), { target: { value: 'テスト店舗' } })
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    // ステップ3: 初期商品
    fireEvent.change(screen.getByPlaceholderText('商品名 1'), { target: { value: 'テスト商品' } })
    fireEvent.click(screen.getByText('次へ')) // → スタッフ
    // ステップ4: スタッフ
    fireEvent.change(screen.getByLabelText('スタッフ名 *'), { target: { value: '山田太郎' } })
    fireEvent.click(screen.getByText('次へ')) // → 確認
    // ステップ5: 確認
    expect(screen.getByText('テスト組織')).toBeInTheDocument()
    expect(screen.getByText('テスト店舗')).toBeInTheDocument()
    expect(screen.getByText('1件')).toBeInTheDocument()
    expect(screen.getByText('山田太郎')).toBeInTheDocument()
  })

  it('確認ステップでは「セットアップ完了」ボタンを表示する', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    fireEvent.click(screen.getByText('次へ')) // → スタッフ
    fireEvent.click(screen.getByText('次へ')) // → 確認
    expect(screen.getByText('セットアップ完了')).toBeInTheDocument()
  })

  it('セットアップ完了を押すと完了画面を表示する', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    fireEvent.click(screen.getByText('次へ')) // → スタッフ
    fireEvent.click(screen.getByText('次へ')) // → 確認
    fireEvent.click(screen.getByText('セットアップ完了'))
    expect(screen.getByText('セットアップ完了', { selector: 'h1' })).toBeInTheDocument()
    expect(
      screen.getByText(
        '組織・店舗・スタッフの初期設定が完了しました。ダッシュボードから各種設定を管理できます。',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('ダッシュボードへ')).toBeInTheDocument()
  })

  it('組織情報フォームで入力値を保持する', () => {
    render(<OnboardingPage />)
    fireEvent.change(screen.getByLabelText('組織名 *'), { target: { value: 'カフェ太郎' } })
    fireEvent.change(screen.getByLabelText('インボイス登録番号'), {
      target: { value: 'T1234567890123' },
    })
    fireEvent.click(screen.getByText('次へ'))
    fireEvent.click(screen.getByText('戻る'))
    expect(screen.getByDisplayValue('カフェ太郎')).toBeInTheDocument()
    expect(screen.getByDisplayValue('T1234567890123')).toBeInTheDocument()
  })

  it('空の商品名は確認ステップの件数に含まれない', () => {
    render(<OnboardingPage />)
    fireEvent.click(screen.getByText('次へ')) // → 店舗設定
    fireEvent.click(screen.getByText('次へ')) // → 初期商品
    // 商品名を空のまま
    fireEvent.click(screen.getByText('次へ')) // → スタッフ
    fireEvent.click(screen.getByText('次へ')) // → 確認
    expect(screen.getByText('0件')).toBeInTheDocument()
  })
})
