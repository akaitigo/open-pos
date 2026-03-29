import { useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'
import { DailySalesResponseSchema, PaginatedStoresSchema, formatMoney } from '@shared-types/openpos'
import type { DailySales } from '@shared-types/openpos'
import { Download } from 'lucide-react'

function todayString(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoString(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

function generateCsv(data: DailySales[]): string {
  const header = '日付,売上合計（円）,取引数,客単価（円）'
  const rows = data.map((row) => {
    const avg = row.transactionCount > 0 ? row.grossAmount / row.transactionCount : 0
    return [
      row.date,
      (row.grossAmount / 100).toFixed(0),
      row.transactionCount,
      (avg / 100).toFixed(0),
    ].join(',')
  })
  return [header, ...rows].join('\n')
}

function downloadCsv(csv: string, filename: string): void {
  const bom = '\uFEFF'
  const blob = new Blob([bom + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export function ExportPage() {
  const [startDate, setStartDate] = useState(daysAgoString(30))
  const [endDate, setEndDate] = useState(todayString())
  const [previewData, setPreviewData] = useState<DailySales[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [storeId, setStoreId] = useState<string | null>(null)

  useEffect(() => {
    api
      .get('/api/stores', PaginatedStoresSchema, { params: { page: 1, pageSize: 1 } })
      .then((r) => {
        if (r.data[0]) setStoreId(r.data[0].id)
      })
      .catch(() => {})
  }, [])

  async function handlePreview() {
    if (!storeId) return
    setLoading(true)
    try {
      const res = await api.get('/api/analytics/daily-sales', DailySalesResponseSchema, {
        params: { storeId, startDate, endDate },
      })
      setPreviewData(res.data)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  function handleDownload() {
    if (!previewData) return
    const csv = generateCsv(previewData)
    downloadCsv(csv, `売上データ_${startDate}_${endDate}.csv`)
  }

  return (
    <>
      <Header title="データエクスポート" />
      <div className="flex flex-1 flex-col gap-4 p-4 max-w-4xl">
        <Card>
          <CardHeader>
            <CardTitle>日次売上データ CSV エクスポート</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-4 max-w-md">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="export-start">開始日</Label>
                  <Input
                    id="export-start"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="export-end">終了日</Label>
                  <Input
                    id="export-end"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                  />
                </div>
              </div>
              <div className="flex gap-2">
                <Button onClick={handlePreview} disabled={loading}>
                  {loading ? '読み込み中...' : 'プレビュー'}
                </Button>
                {previewData && previewData.length > 0 && (
                  <Button variant="outline" onClick={handleDownload}>
                    <Download className="h-4 w-4 mr-2" />
                    CSV ダウンロード
                  </Button>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {previewData && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">プレビュー（{previewData.length}件）</CardTitle>
            </CardHeader>
            <CardContent>
              {previewData.length === 0 ? (
                <p className="text-sm text-muted-foreground">データがありません</p>
              ) : (
                <div className="overflow-auto max-h-96">
                  <table className="w-full text-sm">
                    <thead className="sticky top-0 bg-background">
                      <tr className="border-b">
                        <th className="text-left py-2 px-3">日付</th>
                        <th className="text-right py-2 px-3">売上合計</th>
                        <th className="text-right py-2 px-3">取引数</th>
                        <th className="text-right py-2 px-3">客単価</th>
                      </tr>
                    </thead>
                    <tbody>
                      {previewData.map((row) => (
                        <tr key={row.date} className="border-b">
                          <td className="py-2 px-3">{row.date}</td>
                          <td className="text-right py-2 px-3">{formatMoney(row.grossAmount)}</td>
                          <td className="text-right py-2 px-3">{row.transactionCount}</td>
                          <td className="text-right py-2 px-3">
                            {formatMoney(
                              row.transactionCount > 0 ? row.grossAmount / row.transactionCount : 0,
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </>
  )
}
