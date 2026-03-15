import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { SetupScreen } from './setup-screen'

describe('SetupScreen', () => {
  it('未構成メッセージのタイトルを表示する', () => {
    render(<SetupScreen />)
    expect(screen.getByText('デモ設定が未構成です')).toBeInTheDocument()
  })

  it('organization context に関する説明を表示する', () => {
    render(<SetupScreen />)
    expect(
      screen.getByText('管理画面は organization context がないとデータを表示できません。'),
    ).toBeInTheDocument()
  })

  it('seed コマンドの説明を表示する', () => {
    render(<SetupScreen />)
    expect(
      screen.getByText((_, element) => element?.textContent?.includes('make local-demo') ?? false),
    ).toBeInTheDocument()
  })

  it('demo-config.json の説明を表示する', () => {
    render(<SetupScreen />)
    expect(
      screen.getByText((_, element) => element?.textContent?.includes('demo-config.json') ?? false),
    ).toBeInTheDocument()
  })
})
