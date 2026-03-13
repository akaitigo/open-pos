import { beforeEach, describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { Layout } from './layout'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

describe('Layout', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('サイドバーとメインコンテンツエリアをレンダリングする', () => {
    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )
    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
    expect(screen.getByText('管理ダッシュボード')).toBeInTheDocument()
  })

  it('organization context が未設定ならセットアップ案内を表示する', () => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: null,
    })

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(screen.getByText('デモ設定が未構成です')).toBeInTheDocument()
  })
})
