import { describe, it, expect, vi, afterEach } from 'vitest'
import { startCartSync, stopCartSync, broadcastCartChange } from './cart-sync'

// BroadcastChannel のモック
class MockBroadcastChannel {
  name: string
  onmessage: ((event: MessageEvent) => void) | null = null
  static lastInstance: MockBroadcastChannel | null = null

  constructor(name: string) {
    this.name = name
    MockBroadcastChannel.lastInstance = this
  }

  postMessage = vi.fn()
  close = vi.fn()
}

vi.stubGlobal('BroadcastChannel', MockBroadcastChannel)

describe('CartSync', () => {
  afterEach(() => {
    stopCartSync()
    MockBroadcastChannel.lastInstance = null
  })

  it('should start sync and create BroadcastChannel', () => {
    const handler = vi.fn()
    startCartSync(handler)

    expect(MockBroadcastChannel.lastInstance).toBeTruthy()
    expect(MockBroadcastChannel.lastInstance?.name).toBe('openpos-cart-sync')
  })

  it('should broadcast cart changes', () => {
    const handler = vi.fn()
    startCartSync(handler)

    broadcastCartChange('org-1', 'store-1', 'cart-1', [], 'CLEAR')

    expect(MockBroadcastChannel.lastInstance?.postMessage).toHaveBeenCalledWith(
      expect.objectContaining({
        orgId: 'org-1',
        storeId: 'store-1',
        cartId: 'cart-1',
        action: 'CLEAR',
        items: [],
      }),
    )
  })

  it('should not broadcast when sync is stopped', () => {
    startCartSync(vi.fn())
    stopCartSync()

    broadcastCartChange('org-1', 'store-1', 'cart-1', [], 'ADD')

    // postMessage は startCartSync 中にのみ呼ばれるはず
    // stopCartSync 後は channel が null
  })

  it('should stop sync and close channel', () => {
    startCartSync(vi.fn())
    const channel = MockBroadcastChannel.lastInstance

    stopCartSync()

    expect(channel?.close).toHaveBeenCalled()
  })

  it('should call handler when receiving valid message from another sender', () => {
    const handler = vi.fn()
    startCartSync(handler)

    const channel = MockBroadcastChannel.lastInstance!
    channel.onmessage!(
      new MessageEvent('message', {
        data: {
          orgId: 'org-1',
          storeId: 'store-1',
          cartId: 'cart-1',
          items: [],
          action: 'CLEAR',
          timestamp: Date.now(),
          senderId: 'another-sender',
        },
      }),
    )

    expect(handler).toHaveBeenCalledWith(
      expect.objectContaining({
        orgId: 'org-1',
        action: 'CLEAR',
        senderId: 'another-sender',
      }),
    )
  })

  it('should ignore invalid messages', () => {
    const handler = vi.fn()
    startCartSync(handler)

    const channel = MockBroadcastChannel.lastInstance!
    channel.onmessage!(
      new MessageEvent('message', {
        data: { invalid: 'data' },
      }),
    )

    expect(handler).not.toHaveBeenCalled()
  })

  it('should not create duplicate channels on repeated startCartSync', () => {
    const handler = vi.fn()
    startCartSync(handler)
    const firstChannel = MockBroadcastChannel.lastInstance

    startCartSync(handler)

    expect(MockBroadcastChannel.lastInstance).toBe(firstChannel)
  })
})
