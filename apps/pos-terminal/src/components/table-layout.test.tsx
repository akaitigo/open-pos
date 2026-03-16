import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { TableLayout, createDefaultTables, type TableInfo } from './table-layout'

const mockTables: TableInfo[] = [
  {
    tableNumber: '1',
    status: 'OPEN',
    guestCount: 0,
    totalAmount: 0,
    orderedAt: null,
  },
  {
    tableNumber: '2',
    status: 'OCCUPIED',
    guestCount: 3,
    totalAmount: 450000,
    orderedAt: '2026-01-01T12:00:00Z',
  },
  {
    tableNumber: '3',
    status: 'BILL_REQUESTED',
    guestCount: 2,
    totalAmount: 300000,
    orderedAt: '2026-01-01T11:30:00Z',
  },
]

describe('TableLayout', () => {
  const onSelectTable = vi.fn()
  const onRequestBill = vi.fn()
  const onCloseTable = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('テーブル一覧を表示する', () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    expect(screen.getByText('テーブル一覧')).toBeInTheDocument()
    expect(screen.getByTestId('table-1')).toBeInTheDocument()
    expect(screen.getByTestId('table-2')).toBeInTheDocument()
    expect(screen.getByTestId('table-3')).toBeInTheDocument()
  })

  it('OPEN テーブルのステータスを表示する', () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    expect(screen.getByText('空席')).toBeInTheDocument()
  })

  it('OCCUPIED テーブルのステータスとゲスト数を表示する', () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    expect(screen.getByText('使用中')).toBeInTheDocument()
    expect(screen.getByText('3名')).toBeInTheDocument()
  })

  it('BILL_REQUESTED テーブルのステータスを表示する', () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    expect(screen.getByText('会計待ち')).toBeInTheDocument()
  })

  it('テーブルをクリックすると onSelectTable が呼ばれアクションパネルが表示される', async () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    await userEvent.click(screen.getByTestId('table-1'))
    expect(onSelectTable).toHaveBeenCalledWith('1')
    expect(screen.getByText('テーブル 1')).toBeInTheDocument()
    expect(screen.getByTestId('table-order')).toBeInTheDocument()
    expect(screen.getByTestId('table-bill')).toBeInTheDocument()
    expect(screen.getByTestId('table-close')).toBeInTheDocument()
  })

  it('注文するボタンで onSelectTable を呼ぶ', async () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    await userEvent.click(screen.getByTestId('table-2'))
    onSelectTable.mockClear()

    await userEvent.click(screen.getByTestId('table-order'))
    expect(onSelectTable).toHaveBeenCalledWith('2')
  })

  it('会計ボタンで onRequestBill を呼ぶ', async () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    await userEvent.click(screen.getByTestId('table-2'))
    await userEvent.click(screen.getByTestId('table-bill'))
    expect(onRequestBill).toHaveBeenCalledWith('2')
  })

  it('退席ボタンで onCloseTable を呼びアクションパネルが消える', async () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    await userEvent.click(screen.getByTestId('table-2'))
    await userEvent.click(screen.getByTestId('table-close'))
    expect(onCloseTable).toHaveBeenCalledWith('2')
    expect(screen.queryByTestId('table-order')).not.toBeInTheDocument()
  })

  it('初期状態ではアクションパネルが表示されない', () => {
    render(
      <TableLayout
        tables={mockTables}
        onSelectTable={onSelectTable}
        onRequestBill={onRequestBill}
        onCloseTable={onCloseTable}
      />,
    )
    expect(screen.queryByTestId('table-order')).not.toBeInTheDocument()
  })
})

describe('createDefaultTables', () => {
  it('指定した数のテーブルを生成する', () => {
    const tables = createDefaultTables(6)
    expect(tables).toHaveLength(6)
  })

  it('全テーブルが OPEN ステータスで生成される', () => {
    const tables = createDefaultTables(3)
    for (const table of tables) {
      expect(table.status).toBe('OPEN')
      expect(table.guestCount).toBe(0)
      expect(table.totalAmount).toBe(0)
      expect(table.orderedAt).toBeNull()
    }
  })

  it('テーブル番号が1から連番で設定される', () => {
    const tables = createDefaultTables(4)
    expect(tables.map((t) => t.tableNumber)).toEqual(['1', '2', '3', '4'])
  })

  it('count=0 の場合は空配列を返す', () => {
    expect(createDefaultTables(0)).toEqual([])
  })
})
