import { useEffect, useMemo, useState } from 'react'
import { Header } from '@/components/header'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'
import {
  PaginatedProductsSchema,
  PaginatedStoresSchema,
  PaginatedStaffSchema,
  PaginatedTransactionsSchema,
  DailySalesResponseSchema,
  SalesSummarySchema,
  formatMoney,
} from '@shared-types/openpos'
import type { DailySales, SalesSummary } from '@shared-types/openpos'
import {
  Package,
  Store,
  Users,
  Receipt,
  TrendingUp,
  TrendingDown,
  DollarSign,
  ShoppingCart,
  BarChart3,
  UserCheck,
} from 'lucide-react'

function todayString(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoString(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

export function DashboardPage() {
  const [productCount, setProductCount] = useState<number | null>(null)
  const [storeCount, setStoreCount] = useState<number | null>(null)
  const [staffCount, setStaffCount] = useState<number | null>(null)
  const [transactionCount, setTransactionCount] = useState<number | null>(null)
  const [dailySales, setDailySales] = useState<DailySales[]>([])
  const [todaySummary, setTodaySummary] = useState<SalesSummary | null>(null)
  const [yesterdaySummary, setYesterdaySummary] = useState<SalesSummary | null>(null)

  useEffect(() => {
    api
      .get('/api/products', PaginatedProductsSchema, {
        params: { page: 1, pageSize: 1 },
      })
      .then((r) => setProductCount(r.pagination.totalCount))
      .catch(() => {})

    api
      .get('/api/stores', PaginatedStoresSchema, {
        params: { page: 1, pageSize: 1 },
      })
      .then((r) => {
        setStoreCount(r.pagination.totalCount)
        const firstStore = r.data[0]
        if (firstStore) {
          api
            .get('/api/staff', PaginatedStaffSchema, {
              params: { storeId: firstStore.id, page: 1, pageSize: 1 },
            })
            .then((sr) => setStaffCount(sr.pagination.totalCount))
            .catch(() => {})
        }
      })
      .catch(() => {})

    api
      .get('/api/transactions', PaginatedTransactionsSchema, {
        params: { page: 1, pageSize: 1 },
      })
      .then((r) => setTransactionCount(r.pagination.totalCount))
      .catch(() => {})

    // 過去7日間の日次売上
    api
      .get('/api/analytics/daily-sales', DailySalesResponseSchema, {
        params: { startDate: daysAgoString(6), endDate: todayString() },
      })
      .then((res) => setDailySales(res.data))
      .catch(() => {})

    // 今日のサマリー
    api
      .get('/api/analytics/summary', SalesSummarySchema, {
        params: { startDate: todayString(), endDate: todayString() },
      })
      .then(setTodaySummary)
      .catch(() => {})

    // 昨日のサマリー（前日比用）
    api
      .get('/api/analytics/summary', SalesSummarySchema, {
        params: { startDate: daysAgoString(1), endDate: daysAgoString(1) },
      })
      .then(setYesterdaySummary)
      .catch(() => {})
  }, [])

  const salesDiff = useMemo(() => {
    if (!todaySummary || !yesterdaySummary) return null
    if (yesterdaySummary.totalGross === 0) return null
    return (
      ((todaySummary.totalGross - yesterdaySummary.totalGross) / yesterdaySummary.totalGross) * 100
    )
  }, [todaySummary, yesterdaySummary])

  const basicCards = [
    { title: '商品数', value: productCount, icon: Package, testId: 'summary-card-products' },
    { title: '店舗数', value: storeCount, icon: Store, testId: 'summary-card-stores' },
    { title: 'スタッフ数', value: staffCount, icon: Users, testId: 'summary-card-staff' },
    {
      title: '取引数',
      value: transactionCount,
      icon: Receipt,
      testId: 'summary-card-transactions',
    },
  ]

  const maxSales = useMemo(() => {
    if (dailySales.length === 0) return 1
    return Math.max(...dailySales.map((d) => d.grossAmount), 1)
  }, [dailySales])

  return (
    <>
      <Header title="ダッシュボード" />
      <div className="p-6 space-y-6">
        {/* KPI カード: 今日の売上 */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">本日の売上</CardTitle>
              <DollarSign className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold" data-testid="summary-value">
                {todaySummary ? formatMoney(todaySummary.totalGross) : '...'}
              </div>
              {salesDiff !== null && (
                <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
                  {salesDiff >= 0 ? (
                    <TrendingUp className="h-3 w-3 text-green-500" />
                  ) : (
                    <TrendingDown className="h-3 w-3 text-red-500" />
                  )}
                  <span className={salesDiff >= 0 ? 'text-green-500' : 'text-red-500'}>
                    {salesDiff >= 0 ? '+' : ''}
                    {salesDiff.toFixed(1)}%
                  </span>
                  <span>前日比</span>
                </p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">本日の取引数</CardTitle>
              <ShoppingCart className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold" data-testid="summary-value">
                {todaySummary ? todaySummary.totalTransactions.toLocaleString() : '...'}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">客単価</CardTitle>
              <UserCheck className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold" data-testid="summary-value">
                {todaySummary ? formatMoney(todaySummary.averageTransaction) : '...'}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">昨日の売上</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold" data-testid="summary-value">
                {yesterdaySummary ? formatMoney(yesterdaySummary.totalGross) : '...'}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* 日次売上チャート */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">過去7日間の売上推移</CardTitle>
          </CardHeader>
          <CardContent>
            {dailySales.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-8">
                データを読み込み中...
              </p>
            ) : (
              <div className="flex items-end gap-2 h-48">
                {dailySales.map((day) => {
                  const heightPercent = (day.grossAmount / maxSales) * 100
                  return (
                    <div key={day.date} className="flex-1 flex flex-col items-center gap-1">
                      <span className="text-xs text-muted-foreground">
                        {formatMoney(day.grossAmount)}
                      </span>
                      <div className="w-full flex items-end" style={{ height: '160px' }}>
                        <div
                          className="w-full bg-primary rounded-t-sm"
                          style={{ height: `${Math.max(heightPercent, 2)}%` }}
                        />
                      </div>
                      <span className="text-xs text-muted-foreground">{day.date.slice(5)}</span>
                    </div>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 基本集計カード */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {basicCards.map((card) => (
            <Card key={card.title} data-testid={card.testId}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">{card.title}</CardTitle>
                <card.icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold" data-testid="summary-value">
                  {card.value !== null ? card.value.toLocaleString() : '...'}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </>
  )
}
