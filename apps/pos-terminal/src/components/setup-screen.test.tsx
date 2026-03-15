import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { SetupScreen } from './setup-screen'

describe('SetupScreen', () => {
  it('OpenPOS Terminal タイトルを表示する', () => {
    render(<SetupScreen />)
    expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
  })

  it('未構成の説明を表示する', () => {
    render(<SetupScreen />)
    expect(
      screen.getByText(
        (_, element) =>
          element?.tagName === 'P' &&
          (element?.textContent?.includes('デモ設定が未構成です') ?? false),
      ),
    ).toBeInTheDocument()
  })

  it('seed コマンドの実行手順を表示する', () => {
    render(<SetupScreen />)
    expect(
      screen.getByText(
        (_, element) =>
          element?.tagName === 'P' && (element?.textContent?.includes('make local-demo') ?? false),
      ),
    ).toBeInTheDocument()
  })

  it('demo-config.json のパスを表示する', () => {
    render(<SetupScreen />)
    expect(
      screen.getByText(
        (_, element) =>
          element?.tagName === 'P' && (element?.textContent?.includes('demo-config.json') ?? false),
      ),
    ).toBeInTheDocument()
  })
})
