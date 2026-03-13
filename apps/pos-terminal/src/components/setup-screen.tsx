import { Card } from '@/components/ui/card'

export function SetupScreen() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-lg space-y-3 p-6">
        <h1 className="text-2xl font-bold">OpenPOS Terminal</h1>
        <p className="text-sm text-muted-foreground">
          organization、store、terminal のデモ設定が未構成です。
        </p>
        <p className="text-sm text-muted-foreground">
          `make local-demo` か `bash scripts/seed.sh` を実行してから、ブラウザを再読み込みしてください。
        </p>
        <p className="text-sm text-muted-foreground">
          seed 後は `apps/pos-terminal/public/demo-config.json` が生成され、reload だけで反映されます。
        </p>
      </Card>
    </div>
  )
}
