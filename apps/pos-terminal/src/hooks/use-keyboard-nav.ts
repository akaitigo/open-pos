import { useEffect } from 'react'

interface KeyboardNavOptions {
  onSearch?: () => void
  onCheckout?: () => void
  onCancel?: () => void
}

export function useKeyboardNav({ onSearch, onCheckout, onCancel }: KeyboardNavOptions) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      // Ignore if in input/textarea
      const target = event.target as HTMLElement
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
        if (event.key === 'Escape') {
          target.blur()
          onCancel?.()
        }
        return
      }

      switch (event.key) {
        case 'F1':
          event.preventDefault()
          onSearch?.()
          break
        case 'F2':
          event.preventDefault()
          onCheckout?.()
          break
        case 'Escape':
          onCancel?.()
          break
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onSearch, onCheckout, onCancel])
}
