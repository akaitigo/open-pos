import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { reportError } from '@/lib/error-reporter'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, _errorInfo: ErrorInfo) {
    reportError(error, { componentStack: _errorInfo.componentStack ?? undefined })
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <div className="flex min-h-[400px] flex-col items-center justify-center gap-4 p-8">
          <div className="flex flex-col items-center gap-2 text-center">
            <h2 className="text-xl font-semibold">エラーが発生しました</h2>
            <p className="max-w-md text-sm text-muted-foreground">
              予期しないエラーが発生しました。ページを再読み込みするか、しばらくしてからもう一度お試しください。
            </p>
            {this.state.error && (
              <pre className="mt-2 max-w-lg overflow-auto rounded-md bg-muted p-3 text-left text-xs">
                {this.state.error.message}
              </pre>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={this.handleReset}>
              再試行
            </Button>
            <Button onClick={() => window.location.reload()}>ページを再読み込み</Button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
