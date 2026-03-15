/**
 * スタッフ活動ログ（操作履歴）管理ページ (#187)
 */

import { useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { api } from '@/lib/api'
import { z } from 'zod'

const AuditLogSchema = z.object({
  id: z.string(),
  staffId: z.string().nullable().optional(),
  action: z.string(),
  entityType: z.string(),
  entityId: z.string().nullable().optional(),
  details: z.string().optional(),
  ipAddress: z.string().nullable().optional(),
  createdAt: z.string(),
})

type AuditLog = z.infer<typeof AuditLogSchema>

const AuditLogResponseSchema = z.object({
  data: z.array(AuditLogSchema),
  pagination: z.object({
    page: z.number(),
    pageSize: z.number(),
    totalCount: z.number(),
    totalPages: z.number(),
  }),
})

const actionColors: Record<string, string> = {
  CREATE: 'bg-green-100 text-green-800',
  UPDATE: 'bg-blue-100 text-blue-800',
  DELETE: 'bg-red-100 text-red-800',
  LOGIN_SUCCESS: 'bg-emerald-100 text-emerald-800',
  LOGIN_FAILURE: 'bg-orange-100 text-orange-800',
}

export function ActivityLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [staffIdFilter, setStaffIdFilter] = useState('')
  const [actionFilter, setActionFilter] = useState('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    const params: Record<string, string | number> = { page, pageSize: 20 }
    if (staffIdFilter) params.staffId = staffIdFilter
    if (actionFilter) params.action = actionFilter

    api
      .get('/api/audit-logs', AuditLogResponseSchema, { params })
      .then((res) => {
        setLogs(res.data)
        setTotalPages(res.pagination.totalPages)
      })
      .catch(() => {})
  }, [page, staffIdFilter, actionFilter])

  return (
    <>
      <Header title="操作履歴" />
      <div className="p-6 space-y-4">
        {/* フィルター */}
        <Card>
          <CardContent className="pt-4">
            <div className="flex gap-4">
              <div className="flex-1">
                <Input
                  placeholder="スタッフIDでフィルタ"
                  value={staffIdFilter}
                  onChange={(e) => {
                    setStaffIdFilter(e.target.value)
                    setPage(1)
                  }}
                />
              </div>
              <div className="w-48">
                <select
                  className="w-full rounded-md border px-3 py-2 text-sm"
                  value={actionFilter}
                  onChange={(e) => {
                    setActionFilter(e.target.value)
                    setPage(1)
                  }}
                >
                  <option value="">全てのアクション</option>
                  <option value="CREATE">作成</option>
                  <option value="UPDATE">更新</option>
                  <option value="DELETE">削除</option>
                  <option value="LOGIN_SUCCESS">ログイン成功</option>
                  <option value="LOGIN_FAILURE">ログイン失敗</option>
                </select>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* ログ一覧 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">操作ログ</CardTitle>
          </CardHeader>
          <CardContent>
            {logs.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-8">操作ログがありません</p>
            ) : (
              <div className="space-y-2">
                {logs.map((log) => (
                  <div
                    key={log.id}
                    className="flex items-center gap-3 p-3 rounded-lg border text-sm"
                  >
                    <Badge className={actionColors[log.action] ?? 'bg-gray-100 text-gray-800'}>
                      {log.action}
                    </Badge>
                    <span className="font-medium">{log.entityType}</span>
                    <span className="text-muted-foreground flex-1">{log.entityId ?? '-'}</span>
                    <span className="text-xs text-muted-foreground">
                      {new Date(log.createdAt).toLocaleString('ja-JP')}
                    </span>
                  </div>
                ))}
              </div>
            )}

            {/* ページネーション */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-4">
                <button
                  className="px-3 py-1 text-sm rounded border disabled:opacity-50"
                  disabled={page <= 1}
                  onClick={() => setPage(page - 1)}
                >
                  前へ
                </button>
                <span className="px-3 py-1 text-sm">
                  {page} / {totalPages}
                </span>
                <button
                  className="px-3 py-1 text-sm rounded border disabled:opacity-50"
                  disabled={page >= totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  次へ
                </button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  )
}
