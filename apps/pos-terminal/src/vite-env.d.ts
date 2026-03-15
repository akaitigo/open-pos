/// <reference types="vite/client" />

/**
 * Web Serial API type declarations.
 * Subset required by cash-drawer.ts and escpos.ts.
 */
interface SerialPort {
  readonly readable: ReadableStream<Uint8Array> | null
  readonly writable: WritableStream<Uint8Array> | null
  open(options: SerialOptions): Promise<void>
  close(): Promise<void>
}

interface SerialOptions {
  baudRate: number
}

interface Serial {
  requestPort(options?: SerialPortRequestOptions): Promise<SerialPort>
}

interface SerialPortRequestOptions {
  filters?: SerialPortFilter[]
}

interface SerialPortFilter {
  usbVendorId?: number
  usbProductId?: number
}

interface Navigator {
  readonly serial: Serial
}
