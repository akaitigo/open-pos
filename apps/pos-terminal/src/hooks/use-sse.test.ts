import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useSSE } from './use-sse'

interface MockEventSource {
  onopen: (() => void) | null
  onmessage: ((event: MessageEvent) => void) | null
  onerror: ((event: Event) => void) | null
  close: ReturnType<typeof vi.fn>
  url: string
}

let mockEventSourceInstance: MockEventSource | null = null

class MockEventSourceClass {
  onopen: (() => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  close = vi.fn()
  url: string

  constructor(url: string) {
    this.url = url
    mockEventSourceInstance = this
  }
}

describe('useSSE', () => {
  beforeEach(() => {
    mockEventSourceInstance = null
    vi.stubGlobal('EventSource', MockEventSourceClass)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('enabled=true のとき EventSource を作成する', () => {
    renderHook(() => useSSE({ url: '/api/events', enabled: true }))
    expect(mockEventSourceInstance).not.toBeNull()
    expect(mockEventSourceInstance!.url).toBe('/api/events')
  })

  it('enabled=false のとき EventSource を作成しない', () => {
    renderHook(() => useSSE({ url: '/api/events', enabled: false }))
    expect(mockEventSourceInstance).toBeNull()
  })

  it('接続が開くと connected が true になる', () => {
    const { result } = renderHook(() => useSSE({ url: '/api/events' }))
    expect(result.current.connected).toBe(false)

    act(() => {
      mockEventSourceInstance!.onopen?.()
    })
    expect(result.current.connected).toBe(true)
  })

  it('メッセージ受信時に onMessage コールバックを呼ぶ', () => {
    const onMessage = vi.fn()
    renderHook(() => useSSE({ url: '/api/events', onMessage }))

    const event = new MessageEvent('message', { data: 'test-data' })
    act(() => {
      mockEventSourceInstance!.onmessage?.(event)
    })
    expect(onMessage).toHaveBeenCalledWith(event)
  })

  it('エラー発生時に connected が false になり onError が呼ばれる', () => {
    const onError = vi.fn()
    const { result } = renderHook(() => useSSE({ url: '/api/events', onError }))

    act(() => {
      mockEventSourceInstance!.onopen?.()
    })
    expect(result.current.connected).toBe(true)

    const errorEvent = new Event('error')
    act(() => {
      mockEventSourceInstance!.onerror?.(errorEvent)
    })
    expect(result.current.connected).toBe(false)
    expect(onError).toHaveBeenCalledWith(errorEvent)
  })

  it('disconnect を呼ぶと接続が閉じられる', () => {
    const { result } = renderHook(() => useSSE({ url: '/api/events' }))
    const instance = mockEventSourceInstance!

    act(() => {
      instance.onopen?.()
    })
    expect(result.current.connected).toBe(true)

    act(() => {
      result.current.disconnect()
    })
    expect(result.current.connected).toBe(false)
  })

  it('アンマウント時に EventSource が閉じられる', () => {
    const { unmount } = renderHook(() => useSSE({ url: '/api/events' }))
    const instance = mockEventSourceInstance!

    unmount()
    expect(instance.close).toHaveBeenCalled()
  })

  it('デフォルトで enabled=true として動作する', () => {
    renderHook(() => useSSE({ url: '/api/events' }))
    expect(mockEventSourceInstance).not.toBeNull()
  })
})
