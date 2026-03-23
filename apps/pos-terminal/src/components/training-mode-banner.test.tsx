import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TrainingModeBanner } from './training-mode-banner'

describe('TrainingModeBanner', () => {
  it('isTraining=true の場合にバナーを表示する', () => {
    render(<TrainingModeBanner isTraining={true} />)
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('TRAINING')).toBeInTheDocument()
    expect(screen.getByText(/トレーニングモード/)).toBeInTheDocument()
  })

  it('isTraining=false の場合に何も表示しない', () => {
    const { container } = render(<TrainingModeBanner isTraining={false} />)
    expect(container.innerHTML).toBe('')
  })
})
