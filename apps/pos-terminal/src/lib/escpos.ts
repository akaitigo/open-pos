/**
 * ESC/POS コマンドビルダー (#197)
 *
 * サーマルレシートプリンター向けの ESC/POS コマンドシーケンスを構築する。
 * Web Serial API (navigator.serial) を使用して USB/シリアル接続のプリンターに送信する。
 */

// ESC/POS 制御コード
const ESC = 0x1b
const GS = 0x1d
const LF = 0x0a

/**
 * ESC/POS コマンドビルダー
 */
export class EscPosBuilder {
  private commands: number[] = []

  /** プリンターを初期化する (ESC @) */
  initialize(): this {
    this.commands.push(ESC, 0x40)
    return this
  }

  /** テキストを追加する */
  text(content: string): this {
    const encoder = new TextEncoder()
    const bytes = encoder.encode(content)
    this.commands.push(...bytes)
    return this
  }

  /** 改行を追加する */
  newline(): this {
    this.commands.push(LF)
    return this
  }

  /** テキスト + 改行を追加する */
  line(content: string): this {
    return this.text(content).newline()
  }

  /** 太字を開始/終了する (ESC E n) */
  bold(on: boolean): this {
    this.commands.push(ESC, 0x45, on ? 0x01 : 0x00)
    return this
  }

  /** 文字サイズを設定する (GS ! n) */
  textSize(width: 1 | 2 | 3 | 4, height: 1 | 2 | 3 | 4): this {
    const n = ((width - 1) << 4) | (height - 1)
    this.commands.push(GS, 0x21, n)
    return this
  }

  /** テキスト配置を設定する (ESC a n) */
  align(alignment: 'left' | 'center' | 'right'): this {
    const n = alignment === 'left' ? 0 : alignment === 'center' ? 1 : 2
    this.commands.push(ESC, 0x61, n)
    return this
  }

  /** 紙をカットする (GS V m) */
  cut(partial: boolean = false): this {
    this.commands.push(GS, 0x56, partial ? 0x01 : 0x00)
    return this
  }

  /** フィードしてカットする */
  feedAndCut(lines: number = 3): this {
    for (let i = 0; i < lines; i++) {
      this.commands.push(LF)
    }
    return this.cut()
  }

  /** バーコードを印刷する (GS k m d1...dk NUL) */
  barcode(data: string, type: 'CODE39' | 'CODE128' | 'EAN13' = 'CODE128'): this {
    const typeCode = type === 'CODE39' ? 0x04 : type === 'EAN13' ? 0x02 : 0x49

    // バーコード高さ設定 (GS h n)
    this.commands.push(GS, 0x68, 80)
    // バーコード幅設定 (GS w n)
    this.commands.push(GS, 0x77, 2)
    // HRI 位置設定 (GS H n) - 下に印刷
    this.commands.push(GS, 0x48, 0x02)

    if (type === 'CODE128') {
      // CODE128: GS k 73 n d1...dn
      const encoder = new TextEncoder()
      const bytes = encoder.encode(data)
      this.commands.push(GS, 0x6b, typeCode, bytes.length + 2, 0x7b, 0x42, ...bytes)
    } else {
      // Other types: GS k m d1...dk NUL
      const encoder = new TextEncoder()
      const bytes = encoder.encode(data)
      this.commands.push(GS, 0x6b, typeCode, ...bytes, 0x00)
    }

    return this
  }

  /** QR コードを印刷する */
  qrcode(data: string, size: number = 6): this {
    const encoder = new TextEncoder()
    const bytes = encoder.encode(data)
    const storeLen = bytes.length + 3

    // QR モデル選択 (GS ( k)
    this.commands.push(GS, 0x28, 0x6b, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)
    // QR サイズ設定
    this.commands.push(GS, 0x28, 0x6b, 0x03, 0x00, 0x31, 0x43, size)
    // QR エラー訂正レベル (M)
    this.commands.push(GS, 0x28, 0x6b, 0x03, 0x00, 0x31, 0x45, 0x31)
    // QR データ格納
    this.commands.push(
      GS,
      0x28,
      0x6b,
      storeLen & 0xff,
      (storeLen >> 8) & 0xff,
      0x31,
      0x50,
      0x30,
      ...bytes,
    )
    // QR 印刷
    this.commands.push(GS, 0x28, 0x6b, 0x03, 0x00, 0x31, 0x51, 0x30)

    return this
  }

  /** コマンドバッファを Uint8Array として取得する */
  build(): Uint8Array {
    return new Uint8Array(this.commands)
  }
}

/**
 * Web Serial API を使用してレシートプリンターに接続する
 */
export interface PrinterConnection {
  port: SerialPort
  writer: WritableStreamDefaultWriter<Uint8Array>
}

/**
 * レシートプリンターに接続する (Web Serial API)
 * navigator.serial.requestPort() を呼び出すためユーザー操作が必要
 */
export async function connectPrinter(baudRate: number = 9600): Promise<PrinterConnection> {
  if (!('serial' in navigator)) {
    throw new Error('Web Serial API はこのブラウザでサポートされていません')
  }

  const port = await navigator.serial.requestPort()
  await port.open({ baudRate })

  const writer = port.writable?.getWriter()
  if (!writer) {
    throw new Error('プリンターへの書き込みストリームを取得できません')
  }

  return { port, writer }
}

/**
 * ESC/POS コマンドをプリンターに送信する
 */
export async function sendToPrinter(
  connection: PrinterConnection,
  data: Uint8Array,
): Promise<void> {
  await connection.writer.write(data)
}

/**
 * プリンター接続を閉じる
 */
export async function disconnectPrinter(connection: PrinterConnection): Promise<void> {
  connection.writer.releaseLock()
  await connection.port.close()
}
