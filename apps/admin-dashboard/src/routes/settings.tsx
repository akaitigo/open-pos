import { Header } from '@/components/header'
import { NavLink } from 'react-router'
import { Building2, Monitor, Tag, FileDown, FileText } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

const settingsLinks = [
  {
    title: '組織設定',
    description: '組織名・インボイス番号の設定',
    url: '/organization',
    icon: Building2,
  },
  {
    title: '端末管理',
    description: '店舗ごとの端末の登録・管理',
    url: '/terminals',
    icon: Monitor,
  },
  {
    title: '割引・クーポン',
    description: '割引やクーポンの作成・管理',
    url: '/discounts',
    icon: Tag,
  },
  {
    title: 'データエクスポート',
    description: '売上データの CSV ダウンロード',
    url: '/export',
    icon: FileDown,
  },
  {
    title: 'レポート',
    description: '日次・月次レポートの生成・印刷',
    url: '/reports',
    icon: FileText,
  },
]

export function SettingsPage() {
  return (
    <>
      <Header title="設定" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <p className="text-sm text-muted-foreground">システム設定を管理します</p>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {settingsLinks.map((link) => (
            <NavLink key={link.url} to={link.url}>
              <Card className="hover:bg-accent transition-colors cursor-pointer h-full">
                <CardHeader className="flex flex-row items-center gap-3 pb-2">
                  <link.icon className="h-5 w-5 text-muted-foreground" />
                  <CardTitle className="text-base">{link.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground">{link.description}</p>
                </CardContent>
              </Card>
            </NavLink>
          ))}
        </div>
      </div>
    </>
  )
}
