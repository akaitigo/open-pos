import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export function SetupScreen() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <CardTitle>デモ設定が未構成です</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-muted-foreground">
          <p>管理画面は organization context がないとデータを表示できません。</p>
          <p>
            `make local-demo` か `bash scripts/seed.sh` を実行してから、ブラウザを再読み込みしてください。
          </p>
          <p>seed 後は `apps/admin-dashboard/public/demo-config.json` が生成され、reload だけで反映されます。</p>
        </CardContent>
      </Card>
    </div>
  )
}
