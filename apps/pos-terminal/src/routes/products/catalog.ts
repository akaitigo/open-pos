import { z } from 'zod'
import { api } from '@/lib/api'
import {
  CategorySchema,
  PaginatedProductsSchema,
  PaginatedStocksSchema,
} from '@shared-types/openpos'
import type { Category, PaginatedResponse, Product, Stock } from '@shared-types/openpos'

const CATALOG_FETCH_PAGE_SIZE = 100

export interface ProductCatalog {
  products: Product[]
  categories: Category[]
  stocksByProductId: Record<string, Stock>
}

export async function loadProductCatalog(storeId: string | null): Promise<ProductCatalog> {
  const [categories, products, stocks] = await Promise.all([
    api.get('/api/categories', z.array(CategorySchema)),
    fetchAllPages<Product>('/api/products', PaginatedProductsSchema, {
      activeOnly: true,
      pageSize: CATALOG_FETCH_PAGE_SIZE,
    }),
    storeId
      ? fetchAllPages<Stock>('/api/inventory/stocks', PaginatedStocksSchema, {
          storeId,
          pageSize: CATALOG_FETCH_PAGE_SIZE,
        })
      : Promise.resolve([]),
  ])

  return {
    categories: sortCategories(categories),
    products: sortProducts(products),
    stocksByProductId: indexStocksByProductId(stocks),
  }
}

async function fetchAllPages<T>(
  path: string,
  schema: z.ZodType<PaginatedResponse<T>>,
  params: Record<string, string | number | boolean>,
): Promise<T[]> {
  const items: T[] = []
  let nextPage = 1
  let totalPages = 1

  while (nextPage <= totalPages) {
    const response = await api.get(path, schema, {
      params: {
        ...params,
        page: nextPage,
      },
    })

    items.push(...response.data)
    totalPages = response.pagination.totalPages
    nextPage += 1
  }

  return items
}

export function filterProducts(
  products: Product[],
  categories: Category[],
  selectedParentCategory: string,
  selectedChildCategory: string,
  search: string,
): Product[] {
  const categoryIds = getSelectedCategoryIds(
    categories,
    selectedParentCategory,
    selectedChildCategory,
  )
  const normalizedSearch = search.trim().toLowerCase()

  return sortProducts(
    products.filter((product) => {
      if (categoryIds && (!product.categoryId || !categoryIds.has(product.categoryId))) {
        return false
      }

      if (!normalizedSearch) {
        return true
      }

      return [product.name, product.barcode ?? '', product.sku ?? ''].some((value) =>
        value.toLowerCase().includes(normalizedSearch),
      )
    }),
  )
}

function getSelectedCategoryIds(
  categories: Category[],
  selectedParentCategory: string,
  selectedChildCategory: string,
): Set<string> | null {
  if (selectedChildCategory !== 'all') {
    return new Set([selectedChildCategory])
  }

  if (selectedParentCategory === 'all') {
    return null
  }

  return collectDescendantCategoryIds(categories, selectedParentCategory)
}

function collectDescendantCategoryIds(categories: Category[], rootId: string): Set<string> {
  const ids = new Set<string>([rootId])
  const queue = [rootId]

  while (queue.length > 0) {
    const currentId = queue.shift()
    if (!currentId) continue

    for (const category of categories) {
      if (category.parentId === currentId && !ids.has(category.id)) {
        ids.add(category.id)
        queue.push(category.id)
      }
    }
  }

  return ids
}

export function getTopLevelCategories(categories: Category[]): Category[] {
  return categories.filter((category) => category.parentId === null)
}

export function getChildCategories(categories: Category[], parentId: string): Category[] {
  if (parentId === 'all') {
    return []
  }

  return categories.filter((category) => category.parentId === parentId)
}

export function indexCategoriesById(categories: Category[]): Record<string, Category> {
  return categories.reduce<Record<string, Category>>((result, category) => {
    result[category.id] = category
    return result
  }, {})
}

function indexStocksByProductId(stocks: Stock[]): Record<string, Stock> {
  return stocks.reduce<Record<string, Stock>>((result, stock) => {
    result[stock.productId] = stock
    return result
  }, {})
}

function sortCategories(categories: Category[]): Category[] {
  return [...categories].sort((left, right) => {
    if (left.displayOrder !== right.displayOrder) {
      return left.displayOrder - right.displayOrder
    }
    return left.name.localeCompare(right.name, 'ja')
  })
}

function sortProducts(products: Product[]): Product[] {
  return [...products].sort((left, right) => {
    if (left.displayOrder !== right.displayOrder) {
      return left.displayOrder - right.displayOrder
    }
    return left.name.localeCompare(right.name, 'ja')
  })
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return 'ネットワークまたは API の状態を確認してください。'
}
