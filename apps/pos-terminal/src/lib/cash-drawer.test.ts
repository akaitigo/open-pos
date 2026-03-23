import { describe, it, expect, vi } from 'vitest'
import { DummyCashDrawer, SerialCashDrawer, createCashDrawer } from './cash-drawer'

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

  describe('SerialCashDrawer', () => {
    it('should report as not connected initially', () => {
      const drawer = new SerialCashDrawer()
      expect(drawer.isConnected()).toBe(false)
    })

    it('should return UNKNOWN status when not connected', async () => {
      const drawer = new SerialCashDrawer()
      const status = await drawer.getStatus()
      expect(status).toBe('UNKNOWN')
    })

    it('should return false when Web Serial API is not available', async () => {
      const drawer = new SerialCashDrawer()
      // navigator.serial is not defined in jsdom
      const result = await drawer.open()
      expect(result).toBe(false)
    })

    it('should open port and send drawer kick command when Serial API is available', async () => {
      const mockWrite = vi.fn()
      const mockReleaseLock = vi.fn()
      const mockPort = {
        open: vi.fn().mockResolvedValue(undefined),
        writable: {
          getWriter: () => ({
            write: mockWrite,
            releaseLock: mockReleaseLock,
          }),
        },
      }

      Object.defineProperty(navigator, 'serial', {
        value: { requestPort: vi.fn().mockResolvedValue(mockPort) },
        configurable: true,
      })

      const drawer = new SerialCashDrawer()
      const result = await drawer.open()
      expect(result).toBe(true)
      expect(mockPort.open).toHaveBeenCalledWith({ baudRate: 9600 })
      expect(mockWrite).toHaveBeenCalled()
      expect(mockReleaseLock).toHaveBeenCalled()
      expect(drawer.isConnected()).toBe(true)

      // Second call reuses existing port
      const result2 = await drawer.open()
      expect(result2).toBe(true)

      // Cleanup
      delete (navigator as unknown as Record<string, unknown>).serial
    })

    it('should return false when writable is null', async () => {
      const mockPort = {
        open: vi.fn().mockResolvedValue(undefined),
        writable: null,
      }

      Object.defineProperty(navigator, 'serial', {
        value: { requestPort: vi.fn().mockResolvedValue(mockPort) },
        configurable: true,
      })

      const drawer = new SerialCashDrawer()
      const result = await drawer.open()
      expect(result).toBe(false)

      delete (navigator as unknown as Record<string, unknown>).serial
    })

    it('should return false on error', async () => {
      Object.defineProperty(navigator, 'serial', {
        value: { requestPort: vi.fn().mockRejectedValue(new Error('User cancelled')) },
        configurable: true,
      })

      const drawer = new SerialCashDrawer()
      const result = await drawer.open()
      expect(result).toBe(false)

      delete (navigator as unknown as Record<string, unknown>).serial
    })
  })

  describe('createCashDrawer', () => {
    it('should create DummyCashDrawer when serial is not available', () => {
      const drawer = createCashDrawer(false)
      expect(drawer.isConnected()).toBe(true)
    })

    it('should create DummyCashDrawer when useSerial true but Serial API not available', () => {
      const drawer = createCashDrawer(true)
      expect(drawer.isConnected()).toBe(true)
    })

    it('should create SerialCashDrawer when useSerial true and Serial API available', () => {
      Object.defineProperty(navigator, 'serial', {
        value: { requestPort: vi.fn() },
        configurable: true,
      })

      const drawer = createCashDrawer(true)
      expect(drawer.isConnected()).toBe(false) // SerialCashDrawer starts unconnected

      delete (navigator as unknown as Record<string, unknown>).serial
    })
  })
})
