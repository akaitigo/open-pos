import { useEffect, useState } from 'react'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { api } from '@/lib/api'
import type { Notification } from '@shared-types/openpos'
import { NotificationSchema } from '@shared-types/openpos'
import { Bell, CheckCheck } from 'lucide-react'

const NotificationsResponseSchema = z.object({
  data: z.array(NotificationSchema),
  pagination: z.object({
    page: z.number(),
    pageSize: z.number(),
    totalCount: z.number(),
    totalPages: z.number(),
  }),
})

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      try {
        const response = await api.get('/api/notifications', NotificationsResponseSchema, {
          params: { page: 1, pageSize: 50 },
        })
        if (!cancelled) setNotifications(response.data)
      } catch {
        // ignore
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Bell className="h-6 w-6" />
          <h1 className="text-2xl font-bold">通知</h1>
        </div>
        <Button variant="outline">
          <CheckCheck className="mr-2 h-4 w-4" />
          すべて既読
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>通知一覧</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="py-8 text-center text-muted-foreground">読み込み中...</p>
          ) : notifications.length === 0 ? (
            <p className="py-8 text-center text-muted-foreground">通知はありません</p>
          ) : (
            <div className="space-y-3">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`rounded-lg border p-4 ${notification.isRead ? 'opacity-60' : 'bg-accent/50'}`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <p className="font-medium">{notification.title}</p>
                        {!notification.isRead && (
                          <Badge variant="default" className="text-xs">
                            未読
                          </Badge>
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground">{notification.message}</p>
                      <p className="text-xs text-muted-foreground">{notification.createdAt}</p>
                    </div>
                    <Badge variant="outline">{notification.type}</Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
