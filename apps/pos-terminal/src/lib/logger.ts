/**
 * Structured logger utility.
 * Replaces direct console.* calls with a centralized, configurable logger.
 * In production, this can be wired to a remote logging service.
 */

export type LogLevel = 'debug' | 'info' | 'warn' | 'error'

interface LogEntry {
  level: LogLevel
  tag: string
  message: string
  data?: unknown
  timestamp: string
}

type LogTransport = (entry: LogEntry) => void

const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
}

let minLevel: LogLevel = import.meta.env.DEV ? 'debug' : 'info'
const transports: LogTransport[] = []

/** Set the minimum log level */
export function setLogLevel(level: LogLevel): void {
  minLevel = level
}

/** Register an additional transport (e.g., remote logging) */
export function addLogTransport(transport: LogTransport): void {
  transports.push(transport)
}

function shouldLog(level: LogLevel): boolean {
  return LOG_LEVEL_PRIORITY[level] >= LOG_LEVEL_PRIORITY[minLevel]
}

function emit(entry: LogEntry): void {
  for (const transport of transports) {
    transport(entry)
  }

  // Default transport: structured console output in dev
  if (import.meta.env.DEV) {
    const prefix = `[${entry.tag}]`
    switch (entry.level) {
      case 'debug':
        // eslint-disable-next-line no-console
        console.debug(prefix, entry.message, entry.data ?? '')
        break
      case 'info':
        // eslint-disable-next-line no-console
        console.info(prefix, entry.message, entry.data ?? '')
        break
      case 'warn':
        // eslint-disable-next-line no-console
        console.warn(prefix, entry.message, entry.data ?? '')
        break
      case 'error':
        // eslint-disable-next-line no-console
        console.error(prefix, entry.message, entry.data ?? '')
        break
    }
  }
}

export interface Logger {
  debug(message: string, data?: unknown): void
  info(message: string, data?: unknown): void
  warn(message: string, data?: unknown): void
  error(message: string, data?: unknown): void
}

/** Create a tagged logger instance */
export function createLogger(tag: string): Logger {
  function log(level: LogLevel, message: string, data?: unknown): void {
    if (!shouldLog(level)) return
    const entry: LogEntry = {
      level,
      tag,
      message,
      data,
      timestamp: new Date().toISOString(),
    }
    emit(entry)
  }

  return {
    debug: (message: string, data?: unknown) => log('debug', message, data),
    info: (message: string, data?: unknown) => log('info', message, data),
    warn: (message: string, data?: unknown) => log('warn', message, data),
    error: (message: string, data?: unknown) => log('error', message, data),
  }
}
