/**
 * キャッシュドロワー連携インターフェース (#196)
 * Web Serial API を使用してドロワーを制御する。
 */

export type DrawerStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN'

export interface CashDrawer {
  /** ドロワーを開く */
  open(): Promise<boolean>
  /** ドロワーの状態を取得する */
  getStatus(): Promise<DrawerStatus>
  /** 接続状態を確認する */
  isConnected(): boolean
}

/**
 * Web Serial API を使用したキャッシュドロワー実装。
 * ESC/POS コマンドでドロワーキックを送信する。
 */
export class SerialCashDrawer implements CashDrawer {
  private port: SerialPort | null = null

  async open(): Promise<boolean> {
    try {
      if (!this.port) {
        if (!('serial' in navigator)) {
          console.warn('[ドロワー] Web Serial API は非対応です')
          return false
        }
        this.port = await (
          navigator as unknown as { serial: { requestPort: () => Promise<SerialPort> } }
        ).serial.requestPort()
        await this.port.open({ baudRate: 9600 })
      }

      const writer = this.port.writable?.getWriter()
      if (!writer) return false

      // ESC/POS ドロワーキックコマンド
      const drawerKick = new Uint8Array([0x1b, 0x70, 0x00, 0x19, 0xfa])
      await writer.write(drawerKick)
      writer.releaseLock()

      console.log('[ドロワー] 開放コマンド送信')
      return true
    } catch (error) {
      console.error('[ドロワー] エラー:', error)
      return false
    }
  }

  async getStatus(): Promise<DrawerStatus> {
    if (!this.port) return 'UNKNOWN'
    // プレースホルダー: 実際はステータスポーリングが必要
    return 'UNKNOWN'
  }

  isConnected(): boolean {
    return this.port !== null
  }
}

/**
 * ダミーキャッシュドロワー（テスト用）。
 */
export class DummyCashDrawer implements CashDrawer {
  private status: DrawerStatus = 'CLOSED'

  async open(): Promise<boolean> {
    this.status = 'OPEN'
    console.log('[ダミードロワー] 開放')
    return true
  }

  async getStatus(): Promise<DrawerStatus> {
    return this.status
  }

  isConnected(): boolean {
    return true
  }
}

export function createCashDrawer(useSerial: boolean): CashDrawer {
  if (useSerial && 'serial' in navigator) {
    return new SerialCashDrawer()
  }
  return new DummyCashDrawer()
}
