import { useEffect, useRef, useCallback, useState } from 'react'

interface UseSSEOptions {
  url: string
  enabled?: boolean
  onMessage?: (event: MessageEvent) => void
  onError?: (event: Event) => void
}

export function useSSE({ url, enabled = true, onMessage, onError }: UseSSEOptions) {
  const [connected, setConnected] = useState(false)
  const sourceRef = useRef<EventSource | null>(null)
  const onMessageRef = useRef(onMessage)
  const onErrorRef = useRef(onError)

  useEffect(() => {
    onMessageRef.current = onMessage
    onErrorRef.current = onError
  })

  const disconnect = useCallback(() => {
    if (sourceRef.current) {
      sourceRef.current.close()
      sourceRef.current = null
      setConnected(false)
    }
  }, [])

  useEffect(() => {
    if (!enabled) {
      disconnect()
      return
    }

    const source = new EventSource(url)
    sourceRef.current = source

    source.onopen = () => setConnected(true)

    source.onmessage = (event) => {
      onMessageRef.current?.(event)
    }

    source.onerror = (event) => {
      setConnected(false)
      onErrorRef.current?.(event)
    }

    return () => {
      source.close()
      sourceRef.current = null
    }
  }, [url, enabled, disconnect])

  return { connected, disconnect }
}
