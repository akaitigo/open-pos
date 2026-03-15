/**
 * テーブルオーダー / 飲食店モード (#200)
 * テーブルレイアウトUI。テーブル状態: OPEN/OCCUPIED/BILL_REQUESTED
 */

import { useState } from 'react'
import { formatMoney } from '@shared-types/openpos'

export type TableStatus = 'OPEN' | 'OCCUPIED' | 'BILL_REQUESTED'

export interface TableInfo {
  tableNumber: string
  status: TableStatus
  guestCount: number
  totalAmount: number
  orderedAt: string | null
}

interface TableLayoutProps {
  tables: TableInfo[]
  onSelectTable: (tableNumber: string) => void
  onRequestBill: (tableNumber: string) => void
  onCloseTable: (tableNumber: string) => void
}

const statusColors: Record<TableStatus, string> = {
  OPEN: 'bg-green-100 border-green-500 text-green-800',
  OCCUPIED: 'bg-blue-100 border-blue-500 text-blue-800',
  BILL_REQUESTED: 'bg-yellow-100 border-yellow-500 text-yellow-800',
}

const statusLabels: Record<TableStatus, string> = {
  OPEN: '空席',
  OCCUPIED: '使用中',
  BILL_REQUESTED: '会計待ち',
}

export function TableLayout({
  tables,
  onSelectTable,
  onRequestBill,
  onCloseTable,
}: TableLayoutProps) {
  const [selectedTable, setSelectedTable] = useState<string | null>(null)

  return (
    <div className="p-4">
      <h2 className="text-xl font-bold mb-4">テーブル一覧</h2>

      {/* テーブルグリッド */}
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3">
        {tables.map((table) => (
          <button
            key={table.tableNumber}
            className={`flex flex-col items-center justify-center p-4 rounded-lg border-2 transition-colors min-h-[100px] ${
              statusColors[table.status]
            } ${selectedTable === table.tableNumber ? 'ring-2 ring-primary' : ''}`}
            onClick={() => {
              setSelectedTable(table.tableNumber)
              onSelectTable(table.tableNumber)
            }}
            data-testid={`table-${table.tableNumber}`}
          >
            <span className="text-lg font-bold">{table.tableNumber}</span>
            <span className="text-xs mt-1">{statusLabels[table.status]}</span>
            {table.status !== 'OPEN' && (
              <>
                <span className="text-xs mt-1">{table.guestCount}名</span>
                <span className="text-sm font-medium mt-1">{formatMoney(table.totalAmount)}</span>
              </>
            )}
          </button>
        ))}
      </div>

      {/* 選択されたテーブルのアクション */}
      {selectedTable && (
        <div className="mt-4 p-4 rounded-lg border">
          <h3 className="font-medium mb-3">テーブル {selectedTable}</h3>
          <div className="flex gap-2">
            <button
              className="px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm"
              onClick={() => onSelectTable(selectedTable)}
              data-testid="table-order"
            >
              注文する
            </button>
            <button
              className="px-4 py-2 rounded-md border text-sm"
              onClick={() => onRequestBill(selectedTable)}
              data-testid="table-bill"
            >
              会計
            </button>
            <button
              className="px-4 py-2 rounded-md border text-sm text-red-600"
              onClick={() => {
                onCloseTable(selectedTable)
                setSelectedTable(null)
              }}
              data-testid="table-close"
            >
              退席
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

/**
 * デフォルトのテーブル一覧を生成する（デモ用）。
 */
export function createDefaultTables(count: number): TableInfo[] {
  return Array.from({ length: count }, (_, i) => ({
    tableNumber: `${i + 1}`,
    status: 'OPEN' as TableStatus,
    guestCount: 0,
    totalAmount: 0,
    orderedAt: null,
  }))
}
