import { Header } from '@/components/header'

export function ProductsPage() {
  return (
    <>
      <Header title="商品管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <p className="text-sm text-muted-foreground">商品の一覧・登録・編集を行います</p>
      </div>
    </>
  )
}
