import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useRef } from 'react'
import { useTouchGestures } from './use-touch-gestures'

function createTouchEvent(type: string, x: number, y: number): Event {
  const event = new Event(type, { bubbles: true })
  const touchLike = { clientX: x, clientY: y }
  if (type === 'touchend') {
    Object.defineProperty(event, 'changedTouches', { value: [touchLike] })
  } else {
    Object.defineProperty(event, 'touches', { value: [touchLike] })
  }
  return event
}

function setupHook(options: Parameters<typeof useTouchGestures>[1]) {
  const div = document.createElement('div')
  document.body.appendChild(div)

  const { unmount } = renderHook(() => {
    const ref = useRef<HTMLDivElement>(div)
    useTouchGestures(ref, options)
  })

  return { element: div, unmount }
}

describe('useTouchGestures', () => {
  it('左スワイプを検出する', () => {
    const onSwipeLeft = vi.fn()
    const onSwipe = vi.fn()
    const { element, unmount } = setupHook({ onSwipeLeft, onSwipe })

    element.dispatchEvent(createTouchEvent('touchstart', 200, 100))
    element.dispatchEvent(createTouchEvent('touchend', 100, 100))

    expect(onSwipeLeft).toHaveBeenCalledOnce()
    expect(onSwipe).toHaveBeenCalledWith('left')
    unmount()
  })

  it('右スワイプを検出する', () => {
    const onSwipeRight = vi.fn()
    const { element, unmount } = setupHook({ onSwipeRight })

    element.dispatchEvent(createTouchEvent('touchstart', 100, 100))
    element.dispatchEvent(createTouchEvent('touchend', 200, 100))

    expect(onSwipeRight).toHaveBeenCalledOnce()
    unmount()
  })

  it('上スワイプを検出する', () => {
    const onSwipeUp = vi.fn()
    const { element, unmount } = setupHook({ onSwipeUp })

    element.dispatchEvent(createTouchEvent('touchstart', 100, 200))
    element.dispatchEvent(createTouchEvent('touchend', 100, 100))

    expect(onSwipeUp).toHaveBeenCalledOnce()
    unmount()
  })

  it('下スワイプを検出する', () => {
    const onSwipeDown = vi.fn()
    const { element, unmount } = setupHook({ onSwipeDown })

    element.dispatchEvent(createTouchEvent('touchstart', 100, 100))
    element.dispatchEvent(createTouchEvent('touchend', 100, 200))

    expect(onSwipeDown).toHaveBeenCalledOnce()
    unmount()
  })

  it('threshold未満の移動では発火しない', () => {
    const onSwipe = vi.fn()
    const { element, unmount } = setupHook({ onSwipe, threshold: 50 })

    element.dispatchEvent(createTouchEvent('touchstart', 100, 100))
    element.dispatchEvent(createTouchEvent('touchend', 130, 100))

    expect(onSwipe).not.toHaveBeenCalled()
    unmount()
  })

  it('maxTime超過のスワイプは無視する', () => {
    const onSwipe = vi.fn()
    const { element, unmount } = setupHook({ onSwipe, maxTime: 100 })

    vi.useFakeTimers()
    element.dispatchEvent(createTouchEvent('touchstart', 200, 100))
    vi.advanceTimersByTime(200)
    element.dispatchEvent(createTouchEvent('touchend', 100, 100))

    expect(onSwipe).not.toHaveBeenCalled()
    vi.useRealTimers()
    unmount()
  })

  it('カスタムthresholdで動作する', () => {
    const onSwipeLeft = vi.fn()
    const { element, unmount } = setupHook({ onSwipeLeft, threshold: 20 })

    element.dispatchEvent(createTouchEvent('touchstart', 100, 100))
    element.dispatchEvent(createTouchEvent('touchend', 70, 100))

    expect(onSwipeLeft).toHaveBeenCalledOnce()
    unmount()
  })
})
