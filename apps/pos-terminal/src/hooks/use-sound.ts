import { useCallback, useRef } from 'react'

const STORAGE_KEY = 'openpos-sound-enabled'

type SoundType = 'scan' | 'add-to-cart' | 'payment-complete' | 'error'

const SOUND_CONFIG: Record<
  SoundType,
  { frequency: number; duration: number; type: OscillatorType }
> = {
  scan: { frequency: 1200, duration: 100, type: 'square' },
  'add-to-cart': { frequency: 800, duration: 80, type: 'sine' },
  'payment-complete': { frequency: 600, duration: 200, type: 'sine' },
  error: { frequency: 300, duration: 250, type: 'sawtooth' },
}

function isSoundEnabled(): boolean {
  return localStorage.getItem(STORAGE_KEY) !== 'false'
}

export function useSound() {
  const audioContextRef = useRef<AudioContext | null>(null)

  const play = useCallback((sound: SoundType) => {
    if (!isSoundEnabled()) return

    try {
      if (!audioContextRef.current) {
        audioContextRef.current = new AudioContext()
      }
      const ctx = audioContextRef.current
      const config = SOUND_CONFIG[sound]
      const oscillator = ctx.createOscillator()
      const gainNode = ctx.createGain()

      oscillator.type = config.type
      oscillator.frequency.setValueAtTime(config.frequency, ctx.currentTime)
      gainNode.gain.setValueAtTime(0.1, ctx.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + config.duration / 1000)

      oscillator.connect(gainNode)
      gainNode.connect(ctx.destination)
      oscillator.start(ctx.currentTime)
      oscillator.stop(ctx.currentTime + config.duration / 1000)

      if (sound === 'payment-complete') {
        const osc2 = ctx.createOscillator()
        const gain2 = ctx.createGain()
        osc2.type = 'sine'
        osc2.frequency.setValueAtTime(900, ctx.currentTime + 0.15)
        gain2.gain.setValueAtTime(0.1, ctx.currentTime + 0.15)
        gain2.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.35)
        osc2.connect(gain2)
        gain2.connect(ctx.destination)
        osc2.start(ctx.currentTime + 0.15)
        osc2.stop(ctx.currentTime + 0.35)
      }
    } catch {
      // Web Audio API not available - silently ignore
    }
  }, [])

  const toggleSound = useCallback(() => {
    const current = isSoundEnabled()
    localStorage.setItem(STORAGE_KEY, String(!current))
  }, [])

  return { play, toggleSound, isSoundEnabled: isSoundEnabled() }
}
