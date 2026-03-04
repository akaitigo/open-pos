import { Header } from '@/components/header'

export function SettingsPage() {
  return (
    <>
      <Header title="設定" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <p className="text-sm text-muted-foreground">システム設定を管理します</p>
      </div>
    </>
  )
}
