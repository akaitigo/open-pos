import { Badge } from '@/components/ui/badge'

interface TrainingModeBannerProps {
  isTraining: boolean
}

export function TrainingModeBanner({ isTraining }: TrainingModeBannerProps) {
  if (!isTraining) return null

  return (
    <div
      className="flex items-center justify-center gap-2 bg-amber-500 px-4 py-1.5 text-sm font-semibold text-white"
      role="alert"
      aria-live="polite"
    >
      <Badge variant="outline" className="border-white text-white">
        TRAINING
      </Badge>
      トレーニングモード - この取引は記録されません
    </div>
  )
}
