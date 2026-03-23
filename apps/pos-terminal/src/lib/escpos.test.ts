import { describe, expect, it } from 'vitest'
import { EscPosBuilder } from './escpos'

describe('EscPosBuilder', () => {
  it('generates initialize command (ESC @)', () => {
    const result = new EscPosBuilder().initialize().build()
    expect(result[0]).toBe(0x1b)
    expect(result[1]).toBe(0x40)
  })

  it('generates text with newline', () => {
    const result = new EscPosBuilder().line('test').build()
    // 't', 'e', 's', 't' then LF
    expect(result[result.length - 1]).toBe(0x0a)
  })

  it('generates bold on/off commands (ESC E)', () => {
    const result = new EscPosBuilder().bold(true).text('bold').bold(false).build()
    // ESC E 1
    expect(result[0]).toBe(0x1b)
    expect(result[1]).toBe(0x45)
    expect(result[2]).toBe(0x01)
  })

  it('generates text size command (GS !)', () => {
    const result = new EscPosBuilder().textSize(2, 2).build()
    expect(result[0]).toBe(0x1d) // GS
    expect(result[1]).toBe(0x21) // !
    expect(result[2]).toBe(0x11) // (2-1)<<4 | (2-1) = 0x11
  })

  it('generates alignment command (ESC a)', () => {
    const center = new EscPosBuilder().align('center').build()
    expect(center[0]).toBe(0x1b) // ESC
    expect(center[1]).toBe(0x61) // a
    expect(center[2]).toBe(1) // center

    const right = new EscPosBuilder().align('right').build()
    expect(right[2]).toBe(2) // right
  })

  it('generates cut command (GS V)', () => {
    const result = new EscPosBuilder().cut().build()
    expect(result[0]).toBe(0x1d) // GS
    expect(result[1]).toBe(0x56) // V
    expect(result[2]).toBe(0x00) // full cut
  })

  it('generates partial cut', () => {
    const result = new EscPosBuilder().cut(true).build()
    expect(result[2]).toBe(0x01)
  })

  it('chains multiple commands', () => {
    const result = new EscPosBuilder()
      .initialize()
      .align('center')
      .bold(true)
      .line('Title')
      .bold(false)
      .cut()
      .build()
    expect(result.length).toBeGreaterThan(10)
    // Starts with ESC @
    expect(result[0]).toBe(0x1b)
    expect(result[1]).toBe(0x40)
  })

  it('generates barcode command', () => {
    const result = new EscPosBuilder().barcode('4901234567890', 'EAN13').build()
    // Should contain GS commands for height, width, HRI, then barcode data
    expect(result.length).toBeGreaterThan(10)
  })

  it('generates QR code command', () => {
    const result = new EscPosBuilder().qrcode('https://example.com').build()
    // Should contain multiple GS ( k sequences
    expect(result.length).toBeGreaterThan(20)
  })

  it('generates CODE39 barcode', () => {
    const result = new EscPosBuilder().barcode('12345', 'CODE39').build()
    expect(result.length).toBeGreaterThan(10)
    // CODE39 type code is 0x04
    expect(result).toContain(0x04)
  })

  it('generates CODE128 barcode', () => {
    const result = new EscPosBuilder().barcode('ABC123', 'CODE128').build()
    expect(result.length).toBeGreaterThan(10)
  })

  it('feedAndCut generates newlines then cut', () => {
    const result = new EscPosBuilder().feedAndCut(2).build()
    // 2 LFs then GS V 0x00
    expect(result[0]).toBe(0x0a)
    expect(result[1]).toBe(0x0a)
    expect(result[2]).toBe(0x1d)
    expect(result[3]).toBe(0x56)
  })

  it('align left generates correct value', () => {
    const result = new EscPosBuilder().align('left').build()
    expect(result[2]).toBe(0)
  })

  it('qrcode with custom size', () => {
    const result = new EscPosBuilder().qrcode('test', 3).build()
    expect(result.length).toBeGreaterThan(10)
  })
})

describe('connectPrinter', () => {
  it('throws when Web Serial API is not available', async () => {
    const { connectPrinter } = await import('./escpos')
    await expect(connectPrinter()).rejects.toThrow('Web Serial API')
  })
})
