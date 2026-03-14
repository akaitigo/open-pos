import { useCallback, useEffect, useMemo, useState } from 'react'
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
import { ConfirmDialog } from '@/components/confirm-dialog'
import { api } from '@/lib/api'
import type { Category } from '@shared-types/openpos'
import { CategorySchema } from '@shared-types/openpos'
import { z } from 'zod'
import { ChevronDown, ChevronRight, List, TreesIcon } from 'lucide-react'

type ViewMode = 'table' | 'tree'

interface CategoryTreeNode {
  category: Category
  children: CategoryTreeNode[]
}

function buildTree(categories: Category[]): CategoryTreeNode[] {
  const map = new Map<string, CategoryTreeNode>()
  const roots: CategoryTreeNode[] = []

  for (const cat of categories) {
    map.set(cat.id, { category: cat, children: [] })
  }

  for (const cat of categories) {
    const node = map.get(cat.id)
    if (!node) continue
    if (cat.parentId) {
      const parent = map.get(cat.parentId)
      if (parent) {
        parent.children.push(node)
      } else {
        roots.push(node)
      }
    } else {
      roots.push(node)
    }
  }

  function sortNodes(nodes: CategoryTreeNode[]) {
    nodes.sort((a, b) => a.category.displayOrder - b.category.displayOrder)
    for (const node of nodes) {
      sortNodes(node.children)
    }
  }

  sortNodes(roots)
  return roots
}

export function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>('table')

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
    if (!id) return '\u2014'
    return categories.find((c) => c.id === id)?.name ?? '\u2014'
  }

  const tree = useMemo(() => buildTree(categories), [categories])

  return (
    <>
      <Header title="カテゴリ管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1 rounded-md border p-1">
            <Button
              variant={viewMode === 'table' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('table')}
              aria-label="テーブル表示"
            >
              <List className="mr-1 h-4 w-4" />
              テーブル
            </Button>
            <Button
              variant={viewMode === 'tree' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('tree')}
              aria-label="ツリー表示"
            >
              <TreesIcon className="mr-1 h-4 w-4" />
              ツリー
            </Button>
          </div>
          <Button onClick={handleCreate}>カテゴリを追加</Button>
        </div>

        {viewMode === 'table' ? (
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
                          '\u2014'
                        )}
                      </TableCell>
                      <TableCell className="text-right">{category.displayOrder}</TableCell>
                      <TableCell>
                        <div className="flex gap-1">
                          <Button variant="ghost" size="sm" onClick={() => handleEdit(category)}>
                            編集
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteTarget(category.id)}
                          >
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
        ) : (
          <div className="rounded-md border p-4">
            {tree.length === 0 ? (
              <p className="text-center text-sm text-muted-foreground">
                カテゴリが登録されていません
              </p>
            ) : (
              <div className="flex flex-col gap-1">
                {tree.map((node) => (
                  <CategoryTreeItem
                    key={node.category.id}
                    node={node}
                    depth={0}
                    onEdit={handleEdit}
                    onDelete={(id) => setDeleteTarget(id)}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        <CategoryFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          category={editingCategory}
          categories={categories}
          onSubmit={handleSubmit}
        />

        <ConfirmDialog
          open={deleteTarget !== null}
          onOpenChange={(open) => {
            if (!open) setDeleteTarget(null)
          }}
          title="カテゴリを削除"
          description="本当に削除しますか？この操作は取り消せません。"
          onConfirm={() => {
            if (deleteTarget) handleDelete(deleteTarget)
            setDeleteTarget(null)
          }}
        />
      </div>
    </>
  )
}

function CategoryTreeItem({
  node,
  depth,
  onEdit,
  onDelete,
}: {
  node: CategoryTreeNode
  depth: number
  onEdit: (category: Category) => void
  onDelete: (id: string) => void
}) {
  const [expanded, setExpanded] = useState(true)
  const hasChildren = node.children.length > 0
  const cat = node.category

  return (
    <div>
      <div
        className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-muted/50"
        style={{ paddingLeft: `${depth * 24 + 8}px` }}
      >
        {hasChildren ? (
          <button
            onClick={() => setExpanded(!expanded)}
            className="flex h-5 w-5 items-center justify-center rounded-sm hover:bg-muted"
            aria-label={expanded ? '折りたたむ' : '展開する'}
          >
            {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          </button>
        ) : (
          <span className="h-5 w-5" />
        )}

        {cat.color && (
          <div className="h-3 w-3 rounded-full border" style={{ backgroundColor: cat.color }} />
        )}

        {cat.icon && <span className="text-sm">{cat.icon}</span>}

        <span className="flex-1 text-sm font-medium">{cat.name}</span>

        <span className="text-xs text-muted-foreground">順序: {cat.displayOrder}</span>

        <div className="flex gap-1">
          <Button variant="ghost" size="sm" onClick={() => onEdit(cat)}>
            編集
          </Button>
          <Button variant="ghost" size="sm" onClick={() => onDelete(cat.id)}>
            削除
          </Button>
        </div>
      </div>

      {hasChildren && expanded && (
        <div>
          {node.children.map((child) => (
            <CategoryTreeItem
              key={child.category.id}
              node={child}
              depth={depth + 1}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
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
                placeholder="\uD83C\uDF55"
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
