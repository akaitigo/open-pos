/**
 * Error reporter for centralized error monitoring.
 *
 * Captures unhandled errors and promise rejections via window event listeners.
 * Currently logs to console.error; replace with Sentry SDK initialization
 * when a DSN is configured.
 *
 * Usage:
 *   import { initErrorReporter, reportError } from '@/lib/error-reporter'
 *   initErrorReporter()  // Call once at app startup
 *   reportError(error)   // Manual error reporting
 */

import { createLogger } from '@/lib/logger'

const log = createLogger('ErrorReporter')

export interface ErrorReport {
  message: string
  stack?: string
  source?: string
  lineno?: number
  colno?: number
  timestamp: string
  userAgent: string
  url: string
}

type ErrorReportListener = (report: ErrorReport) => void

const listeners: Set<ErrorReportListener> = new Set()
let initialized = false

function buildReport(
  error: Error | string,
  extra?: { source?: string; lineno?: number; colno?: number },
): ErrorReport {
  const isError = error instanceof Error
  return {
    message: isError ? error.message : String(error),
    stack: isError ? error.stack : undefined,
    source: extra?.source,
    lineno: extra?.lineno,
    colno: extra?.colno,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    url: window.location.href,
  }
}

function notifyListeners(report: ErrorReport): void {
  for (const listener of listeners) {
    try {
      listener(report)
    } catch {
      // Prevent listener errors from causing infinite loops
    }
  }
}

/**
 * Subscribe to error reports. Returns an unsubscribe function.
 */
export function onErrorReport(listener: ErrorReportListener): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/**
 * Manually report an error.
 */
export function reportError(error: Error | string, context?: Record<string, unknown>): void {
  const report = buildReport(error)
  log.error(report.message, { ...report, context })
  notifyListeners(report)
}

/**
 * Initialize global error handlers. Call once at app startup.
 * Returns a cleanup function to remove event listeners.
 */
export function initErrorReporter(): () => void {
  if (initialized) {
    log.warn('ErrorReporter already initialized')
    return () => {}
  }

  initialized = true

  function handleError(event: ErrorEvent): void {
    const report = buildReport(event.error instanceof Error ? event.error : event.message, {
      source: event.filename,
      lineno: event.lineno,
      colno: event.colno,
    })
    log.error('Unhandled error', report)
    notifyListeners(report)
  }

  function handleUnhandledRejection(event: PromiseRejectionEvent): void {
    const error = event.reason instanceof Error ? event.reason : String(event.reason)
    const report = buildReport(error)
    log.error('Unhandled promise rejection', report)
    notifyListeners(report)
  }

  window.addEventListener('error', handleError)
  window.addEventListener('unhandledrejection', handleUnhandledRejection)

  log.info('Error reporter initialized')

  return () => {
    window.removeEventListener('error', handleError)
    window.removeEventListener('unhandledrejection', handleUnhandledRejection)
    initialized = false
  }
}
