import { Button } from '@/components/ui/button'
import { Moon, Sun } from 'lucide-react'
import { useDarkMode } from '@/hooks/use-dark-mode'
import { t } from '@/i18n'

export function DarkModeToggle() {
  const { isDark, toggle } = useDarkMode()

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={toggle}
      aria-label={isDark ? t('accessibility.lightModeToggle') : t('accessibility.darkModeToggle')}
    >
      {isDark ? (
        <Sun className="h-4 w-4" aria-hidden="true" />
      ) : (
        <Moon className="h-4 w-4" aria-hidden="true" />
      )}
    </Button>
  )
}
