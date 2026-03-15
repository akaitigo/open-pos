import { describe, it, expect } from 'vitest'
import { StarPayTerminal, DummyPaymentTerminal, createPaymentTerminal } from './payment-terminal'

describe('PaymentTerminal', () => {
  describe('StarPayTerminal', () => {
    it('should connect successfully', async () => {
      const terminal = new StarPayTerminal()
      expect(terminal.isConnected()).toBe(false)

      const result = await terminal.connect()
      expect(result).toBe(true)
      expect(terminal.isConnected()).toBe(true)
    })

    it('should process payment when connected', async () => {
      const terminal = new StarPayTerminal()
      await terminal.connect()

      const result = await terminal.pay(10000)
      expect(result.success).toBe(true)
      expect(result.transactionId).toBeTruthy()
      expect(result.errorMessage).toBeNull()
    })

    it('should fail payment when not connected', async () => {
      const terminal = new StarPayTerminal()

      const result = await terminal.pay(10000)
      expect(result.success).toBe(false)
      expect(result.errorMessage).toBe('端末に接続されていません')
    })

    it('should cancel payment', async () => {
      const terminal = new StarPayTerminal()
      const result = await terminal.cancel()
      expect(result).toBe(true)
    })

    it('should return correct name', () => {
      const terminal = new StarPayTerminal()
      expect(terminal.getName()).toBe('StarPay')
    })
  })

  describe('DummyPaymentTerminal', () => {
    it('should always succeed', async () => {
      const terminal = new DummyPaymentTerminal()
      await terminal.connect()
      expect(terminal.isConnected()).toBe(true)

      const result = await terminal.pay(5000)
      expect(result.success).toBe(true)
    })

    it('should return correct name', () => {
      const terminal = new DummyPaymentTerminal()
      expect(terminal.getName()).toBe('ダミー端末')
    })
  })

  describe('createPaymentTerminal', () => {
    it('should create StarPayTerminal for starpay type', () => {
      const terminal = createPaymentTerminal('starpay')
      expect(terminal.getName()).toBe('StarPay')
    })

    it('should create DummyPaymentTerminal for unknown type', () => {
      const terminal = createPaymentTerminal('unknown')
      expect(terminal.getName()).toBe('ダミー端末')
    })
  })
})
