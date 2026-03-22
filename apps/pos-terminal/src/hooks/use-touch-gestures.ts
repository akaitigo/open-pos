import { useEffect, useRef, type RefObject } from 'react'

export type SwipeDirection = 'left' | 'right' | 'up' | 'down'

interface TouchGestureOptions {
  onSwipe?: (direction: SwipeDirection) => void
  onSwipeLeft?: () => void
  onSwipeRight?: () => void
  onSwipeUp?: () => void
  onSwipeDown?: () => void
  /** Minimum distance in px to trigger a swipe (default: 50) */
  threshold?: number
  /** Maximum time in ms for a swipe gesture (default: 300) */
  maxTime?: number
}

interface TouchState {
  startX: number
  startY: number
  startTime: number
}

export function useTouchGestures<T extends HTMLElement>(
  ref: RefObject<T | null>,
  options: TouchGestureOptions,
): void {
  const optionsRef = useRef(options)
  optionsRef.current = options

  useEffect(() => {
    const element = ref.current
    if (!element) return

    let touchState: TouchState | null = null

    function handleTouchStart(e: TouchEvent) {
      const touch = e.touches[0]
      if (!touch) return
      touchState = {
        startX: touch.clientX,
        startY: touch.clientY,
        startTime: Date.now(),
      }
    }

    function handleTouchEnd(e: TouchEvent) {
      if (!touchState) return

      const touch = e.changedTouches[0]
      if (!touch) return

      const { threshold = 50, maxTime = 300 } = optionsRef.current
      const elapsed = Date.now() - touchState.startTime

      if (elapsed > maxTime) {
        touchState = null
        return
      }

      const dx = touch.clientX - touchState.startX
      const dy = touch.clientY - touchState.startY
      const absDx = Math.abs(dx)
      const absDy = Math.abs(dy)

      touchState = null

      if (absDx < threshold && absDy < threshold) return

      let direction: SwipeDirection
      if (absDx > absDy) {
        direction = dx > 0 ? 'right' : 'left'
      } else {
        direction = dy > 0 ? 'down' : 'up'
      }

      optionsRef.current.onSwipe?.(direction)

      switch (direction) {
        case 'left':
          optionsRef.current.onSwipeLeft?.()
          break
        case 'right':
          optionsRef.current.onSwipeRight?.()
          break
        case 'up':
          optionsRef.current.onSwipeUp?.()
          break
        case 'down':
          optionsRef.current.onSwipeDown?.()
          break
      }
    }

    element.addEventListener('touchstart', handleTouchStart, { passive: true })
    element.addEventListener('touchend', handleTouchEnd, { passive: true })

    return () => {
      element.removeEventListener('touchstart', handleTouchStart)
      element.removeEventListener('touchend', handleTouchEnd)
    }
  }, [ref])
}
