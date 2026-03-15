import { describe, it, expect } from 'vitest'
import { DummyCashDrawer, createCashDrawer } from './cash-drawer'

describe('CashDrawer', () => {
  describe('DummyCashDrawer', () => {
    it('should report as connected', () => {
      const drawer = new DummyCashDrawer()
      expect(drawer.isConnected()).toBe(true)
    })

    it('should open successfully', async () => {
      const drawer = new DummyCashDrawer()
      const result = await drawer.open()
      expect(result).toBe(true)
    })

    it('should report OPEN status after opening', async () => {
      const drawer = new DummyCashDrawer()
      await drawer.open()
      const status = await drawer.getStatus()
      expect(status).toBe('OPEN')
    })

    it('should report CLOSED status initially', async () => {
      const drawer = new DummyCashDrawer()
      const status = await drawer.getStatus()
      expect(status).toBe('CLOSED')
    })
  })

  describe('createCashDrawer', () => {
    it('should create DummyCashDrawer when serial is not available', () => {
      const drawer = createCashDrawer(false)
      expect(drawer.isConnected()).toBe(true)
    })
  })
})
