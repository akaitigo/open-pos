import { Header } from '@/components/header'

export function DashboardPage() {
  return (
    <>
      <Header title="ダッシュボード" />
      <div className="flex flex-1 flex-col items-center justify-center gap-4 p-4">
        <h2 className="text-2xl font-bold text-muted-foreground">Dashboard - Coming Soon</h2>
        <p className="text-sm text-muted-foreground">売上・在庫・注文の概要がここに表示されます</p>
      </div>
    </>
  )
}
