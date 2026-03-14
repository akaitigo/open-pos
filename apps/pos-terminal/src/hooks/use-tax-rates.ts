import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import { TaxRateSchema } from '@shared-types/openpos'
import type { TaxRate } from '@shared-types/openpos'
import { z } from 'zod'

export function useTaxRates() {
  const [taxRates, setTaxRates] = useState<TaxRate[]>([])

  useEffect(() => {
    let cancelled = false

    api
      .get('/api/tax-rates', z.array(TaxRateSchema))
      .then((result) => {
        if (!cancelled) setTaxRates(result)
      })
      .catch(() => {
        if (!cancelled) setTaxRates([])
      })

    return () => {
      cancelled = true
    }
  }, [])

  return taxRates
}
