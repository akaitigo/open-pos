import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TrainingModeBanner } from './training-mode-banner'

describe('TrainingModeBanner', () => {
  it('isTraining=true のとき警告バナーを表示する', () => {
    render(<TrainingModeBanner isTraining={true} />)
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('TRAINING')).toBeInTheDocument()
    expect(screen.getByText('トレーニングモード - この取引は記録されません')).toBeInTheDocument()
  })

  it('isTraining=false のとき何も表示しない', () => {
    const { container } = render(<TrainingModeBanner isTraining={false} />)
    expect(container.firstChild).toBeNull()
  })

  it('aria-live="polite" が設定されている', () => {
    render(<TrainingModeBanner isTraining={true} />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveAttribute('aria-live', 'polite')
  })
})
