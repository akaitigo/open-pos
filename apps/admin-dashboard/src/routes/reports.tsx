import { useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'
import { DailySalesSchema, SalesSummarySchema, formatMoney } from '@shared-types/openpos'
import type { DailySales, SalesSummary } from '@shared-types/openpos'
import { z } from 'zod'
import { Printer } from 'lucide-react'

function todayString(): string {
  return new Date().toISOString().slice(0, 10)
}

function firstOfMonth(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

export function ReportsPage() {
  const [startDate, setStartDate] = useState(firstOfMonth())
  const [endDate, setEndDate] = useState(todayString())
  const [dailySales, setDailySales] = useState<DailySales[] | null>(null)
  const [summary, setSummary] = useState<SalesSummary | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleGenerate() {
    setLoading(true)
    try {
      const [dailyData, summaryData] = await Promise.all([
        api.get('/api/analytics/daily', z.array(DailySalesSchema), {
          params: { startDate, endDate },
        }),
        api.get('/api/analytics/summary', SalesSummarySchema, {
          params: { startDate, endDate },
        }),
      ])
      setDailySales(dailyData)
      setSummary(summaryData)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  function handlePrint() {
    window.print()
  }

  const hasData = dailySales !== null && summary !== null

  return (
    <>
      <div className="print:hidden">
        <Header title="レポート" />
      </div>
      <div className="flex flex-1 flex-col gap-4 p-4 max-w-4xl">
        <div className="print:hidden">
          <Card>
            <CardHeader>
              <CardTitle>売上レポート生成</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-col gap-4">
                <div className="grid grid-cols-2 gap-4 max-w-md">
                  <div className="flex flex-col gap-2">
                    <Label htmlFor="report-start">開始日</Label>
                    <Input
                      id="report-start"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label htmlFor="report-end">終了日</Label>
                    <Input
                      id="report-end"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                    />
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button onClick={handleGenerate} disabled={loading}>
                    {loading ? '生成中...' : 'レポート生成'}
                  </Button>
                  {hasData && (
                    <Button variant="outline" onClick={handlePrint}>
                      <Printer className="h-4 w-4 mr-2" />
                      印刷
                    </Button>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {hasData && (
          <div className="print:p-0 space-y-6" id="report-content">
            {/* 印刷用ヘッダー */}
            <div className="hidden print:block text-center mb-6">
              <h1 className="text-xl font-bold">売上レポート</h1>
              <p className="text-sm text-gray-600">
                期間: {startDate} ~ {endDate}
              </p>
              <p className="text-xs text-gray-400">
                生成日時: {new Date().toLocaleString('ja-JP')}
              </p>
            </div>

            {/* サマリーカード */}
            <Card className="print:border print:shadow-none">
              <CardHeader>
                <CardTitle className="text-base">期間サマリー</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <p className="text-sm text-muted-foreground">売上合計</p>
                    <p className="text-xl font-bold">{formatMoney(summary.totalGross)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">取引数</p>
                    <p className="text-xl font-bold">
                      {summary.totalTransactions.toLocaleString()}件
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">客単価</p>
                    <p className="text-xl font-bold">{formatMoney(summary.averageTransaction)}</p>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-4 mt-4 pt-4 border-t">
                  <div>
                    <p className="text-sm text-muted-foreground">税抜合計</p>
                    <p className="text-base font-medium">{formatMoney(summary.totalNet)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">税額合計</p>
                    <p className="text-base font-medium">{formatMoney(summary.totalTax)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">割引合計</p>
                    <p className="text-base font-medium">{formatMoney(summary.totalDiscount)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 日次売上明細 */}
            <Card className="print:border print:shadow-none print:break-inside-avoid">
              <CardHeader>
                <CardTitle className="text-base">日次売上明細</CardTitle>
              </CardHeader>
              <CardContent>
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-2 px-2">日付</th>
                      <th className="text-right py-2 px-2">売上合計</th>
                      <th className="text-right py-2 px-2">税額</th>
                      <th className="text-right py-2 px-2">取引数</th>
                      <th className="text-right py-2 px-2">客単価</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dailySales.map((day) => (
                      <tr key={day.date} className="border-b">
                        <td className="py-2 px-2">{day.date}</td>
                        <td className="text-right py-2 px-2">{formatMoney(day.grossAmount)}</td>
                        <td className="text-right py-2 px-2">{formatMoney(day.taxAmount)}</td>
                        <td className="text-right py-2 px-2">{day.transactionCount}</td>
                        <td className="text-right py-2 px-2">
                          {formatMoney(
                            day.transactionCount > 0 ? day.grossAmount / day.transactionCount : 0,
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </CardContent>
            </Card>
          </div>
        )}
      </div>

      {/* 印刷スタイル */}
      <style>{`
        @media print {
          body * {
            visibility: hidden;
          }
          #report-content, #report-content * {
            visibility: visible;
          }
          #report-content {
            position: absolute;
            left: 0;
            top: 0;
            width: 100%;
          }
          .print\\:hidden {
            display: none !important;
          }
          .print\\:block {
            display: block !important;
          }
        }
      `}</style>
    </>
  )
}
