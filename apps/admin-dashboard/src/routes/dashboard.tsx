import { useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'
import {
  PaginatedProductsSchema,
  PaginatedStoresSchema,
  PaginatedStaffSchema,
  PaginatedTransactionsSchema,
} from '@shared-types/openpos'
import { Package, Store, Users, Receipt } from 'lucide-react'

export function DashboardPage() {
  const [productCount, setProductCount] = useState<number | null>(null)
  const [storeCount, setStoreCount] = useState<number | null>(null)
  const [staffCount, setStaffCount] = useState<number | null>(null)
  const [transactionCount, setTransactionCount] = useState<number | null>(null)

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
  }, [])

  const cards = [
    { title: '商品数', value: productCount, icon: Package },
    { title: '店舗数', value: storeCount, icon: Store },
    { title: 'スタッフ数', value: staffCount, icon: Users },
    { title: '取引数', value: transactionCount, icon: Receipt },
  ]

  return (
    <>
      <Header title="ダッシュボード" />
      <div className="p-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {cards.map((card) => (
            <Card key={card.title}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">{card.title}</CardTitle>
                <card.icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
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
