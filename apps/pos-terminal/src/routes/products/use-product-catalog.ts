import { useEffect, useState } from 'react'
import type { Category, Product, Stock } from '@shared-types/openpos'
import { getErrorMessage, loadProductCatalog } from './catalog'

interface ProductCatalogState {
  products: Product[]
  categories: Category[]
  stocksByProductId: Record<string, Stock>
  loading: boolean
  error: string | null
}

const INITIAL_STATE: ProductCatalogState = {
  products: [],
  categories: [],
  stocksByProductId: {},
  loading: true,
  error: null,
}

export function useProductCatalog(storeId: string | null, reloadKey: number): ProductCatalogState {
  const [state, setState] = useState<ProductCatalogState>(INITIAL_STATE)

  useEffect(() => {
    let cancelled = false

    async function fetchCatalog() {
      setState((current) => ({
        ...current,
        loading: true,
        error: null,
      }))

      try {
        const catalog = await loadProductCatalog(storeId)
        if (cancelled) return

        setState({
          ...catalog,
          loading: false,
          error: null,
        })
      } catch (error) {
        if (cancelled) return

        setState((current) => ({
          ...current,
          loading: false,
          error: getErrorMessage(error),
        }))
      }
    }

    void fetchCatalog()

    return () => {
      cancelled = true
    }
  }, [reloadKey, storeId])

  return state
}
