import { useCallback, useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { api } from '@/lib/api'
import type { Category } from '@shared-types/openpos'
import { CategorySchema } from '@shared-types/openpos'
import { z } from 'zod'

export function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)

  const fetchCategories = useCallback(async () => {
    const result = await api.get('/api/categories', z.array(CategorySchema))
    setCategories(result)
  }, [])

  useEffect(() => {
    fetchCategories()
  }, [fetchCategories])

  function handleCreate() {
    setEditingCategory(null)
    setDialogOpen(true)
  }

  function handleEdit(category: Category) {
    setEditingCategory(category)
    setDialogOpen(true)
  }

  async function handleDelete(id: string) {
    await api.delete(`/api/categories/${id}`)
    fetchCategories()
  }

  async function handleSubmit(data: Record<string, unknown>) {
    if (editingCategory) {
      await api.put(`/api/categories/${editingCategory.id}`, data, CategorySchema)
    } else {
      await api.post('/api/categories', data, CategorySchema)
    }
    setDialogOpen(false)
    fetchCategories()
  }

  function getCategoryName(id: string | null): string {
    if (!id) return '—'
    return categories.find((c) => c.id === id)?.name ?? '—'
  }

  return (
    <>
      <Header title="カテゴリ管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-end">
          <Button onClick={handleCreate}>カテゴリを追加</Button>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>カテゴリ名</TableHead>
                <TableHead>親カテゴリ</TableHead>
                <TableHead>カラー</TableHead>
                <TableHead className="text-right">表示順</TableHead>
                <TableHead className="w-[120px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {categories.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    カテゴリが登録されていません
                  </TableCell>
                </TableRow>
              ) : (
                categories.map((category) => (
                  <TableRow key={category.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        {category.icon && <span>{category.icon}</span>}
                        {category.name}
                      </div>
                    </TableCell>
                    <TableCell>{getCategoryName(category.parentId)}</TableCell>
                    <TableCell>
                      {category.color ? (
                        <div className="flex items-center gap-2">
                          <div
                            className="h-4 w-4 rounded-full border"
                            style={{ backgroundColor: category.color }}
                          />
                          <span className="font-mono text-xs">{category.color}</span>
                        </div>
                      ) : (
                        '—'
                      )}
                    </TableCell>
                    <TableCell className="text-right">{category.displayOrder}</TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" onClick={() => handleEdit(category)}>
                          編集
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => handleDelete(category.id)}>
                          削除
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        <CategoryFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          category={editingCategory}
          categories={categories}
          onSubmit={handleSubmit}
        />
      </div>
    </>
  )
}

function CategoryFormDialog({
  open,
  onOpenChange,
  category,
  categories,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  category: Category | null
  categories: Category[]
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')
  const [color, setColor] = useState('')
  const [icon, setIcon] = useState('')
  const [displayOrder, setDisplayOrder] = useState('0')

  useEffect(() => {
    if (category) {
      setName(category.name)
      setParentId(category.parentId ?? '')
      setColor(category.color ?? '')
      setIcon(category.icon ?? '')
      setDisplayOrder(String(category.displayOrder))
    } else {
      setName('')
      setParentId('')
      setColor('')
      setIcon('')
      setDisplayOrder('0')
    }
  }, [category, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      name,
      parentId: parentId || undefined,
      color: color || undefined,
      icon: icon || undefined,
      displayOrder: Number(displayOrder) || 0,
    })
  }

  const parentOptions = categories.filter((c) => c.id !== category?.id)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{category ? 'カテゴリを編集' : 'カテゴリを追加'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="cat-name">カテゴリ名 *</Label>
            <Input id="cat-name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="cat-parent">親カテゴリ</Label>
            <select
              id="cat-parent"
              value={parentId}
              onChange={(e) => setParentId(e.target.value)}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
            >
              <option value="">なし（ルート）</option>
              {parentOptions.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="cat-color">カラー</Label>
              <Input
                id="cat-color"
                type="color"
                value={color || '#000000'}
                onChange={(e) => setColor(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="cat-icon">アイコン</Label>
              <Input
                id="cat-icon"
                value={icon}
                onChange={(e) => setIcon(e.target.value)}
                placeholder="🍕"
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="cat-order">表示順</Label>
              <Input
                id="cat-order"
                type="number"
                value={displayOrder}
                onChange={(e) => setDisplayOrder(e.target.value)}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">{category ? '更新' : '追加'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
