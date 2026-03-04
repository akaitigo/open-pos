import { Badge } from '@/components/ui/badge'

export function Header() {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b px-4">
      <div className="flex items-center gap-3">
        <h1 className="text-lg font-semibold">OpenPOS Terminal</h1>
        <Badge variant="outline" className="text-xs">
          本店
        </Badge>
      </div>
      <div className="flex items-center gap-2">
        <Badge variant="secondary" className="text-xs">
          オンライン
        </Badge>
      </div>
    </header>
  )
}
