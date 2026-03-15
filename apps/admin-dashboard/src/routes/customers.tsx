import { useEffect, useState } from 'react'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { api } from '@/lib/api'
import type { Customer } from '@shared-types/openpos'
import { CustomerSchema } from '@shared-types/openpos'
import { Plus, Search, Users } from 'lucide-react'

const CustomersResponseSchema = z.object({
  data: z.array(CustomerSchema),
  pagination: z.object({
    page: z.number(),
    pageSize: z.number(),
    totalCount: z.number(),
    totalPages: z.number(),
  }),
})

export function CustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      try {
        const response = await api.get('/api/customers', CustomersResponseSchema, {
          params: { page: 1, pageSize: 50, ...(search ? { search } : {}) },
        })
        if (\!cancelled) setCustomers(response.data)
      } catch {
        // ignore
      } finally {
        if (\!cancelled) setLoading(false)
      }
    }
    void load()
    return () => { cancelled = true }
  }, [search])

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Users className="h-6 w-6" />
          <h1 className="text-2xl font-bold">顧客管理</h1>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          顧客を追加
        </Button>
      </div>

      <div className="flex items-center gap-2">
        <Search className="h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="顧客名で検索..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-sm"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>顧客一覧</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="py-8 text-center text-muted-foreground">読み込み中...</p>
          ) : customers.length === 0 ? (
            <p className="py-8 text-center text-muted-foreground">顧客が登録されていません</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>氏名</TableHead>
                  <TableHead>メールアドレス</TableHead>
                  <TableHead>電話番号</TableHead>
                  <TableHead className="text-right">ポイント</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {customers.map((customer) => (
                  <TableRow key={customer.id}>
                    <TableCell className="font-medium">{customer.name}</TableCell>
                    <TableCell>{customer.email ?? '-'}</TableCell>
                    <TableCell>{customer.phone ?? '-'}</TableCell>
                    <TableCell className="text-right">
                      <Badge variant="secondary">{customer.points.toLocaleString()} pt</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
