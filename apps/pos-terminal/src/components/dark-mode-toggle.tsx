import { Button } from '@/components/ui/button'
import { Moon, Sun } from 'lucide-react'
import { useDarkMode } from '@/hooks/use-dark-mode'

export function DarkModeToggle() {
  const { isDark, toggle } = useDarkMode()

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={toggle}
      aria-label={isDark ? 'ライトモードに切替' : 'ダークモードに切替'}
    >
      {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </Button>
  )
}
