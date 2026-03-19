/**
 * キャッシュドロワー連携インターフェース (#196)
 * Web Serial API を使用してドロワーを制御する。
 */

import { createLogger } from './logger'

const log = createLogger('ドロワー')
const dummyLog = createLogger('ダミードロワー')

export type DrawerStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN'

export interface CashDrawer {
  /** ドロワーを開く */
  open(): Promise<boolean>
  /** ドロワーの状態を取得する */
  getStatus(): Promise<DrawerStatus>
  /** 接続状態を確認する */
  isConnected(): boolean
}

/** Web Serial API の型定義 */
interface SerialNavigator {
  serial: {
    requestPort: () => Promise<SerialPort>
  }
}

/** Navigator が Web Serial API を持つか判定する型ガード */
function hasSerialApi(nav: Navigator): nav is Navigator & SerialNavigator {
  return 'serial' in nav
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
        if (!hasSerialApi(navigator)) {
          log.warn('Web Serial API は非対応です')
          return false
        }
        this.port = await navigator.serial.requestPort()
        await this.port.open({ baudRate: 9600 })
      }

      const writer = this.port.writable?.getWriter()
      if (!writer) return false

      // ESC/POS ドロワーキックコマンド
      const drawerKick = new Uint8Array([0x1b, 0x70, 0x00, 0x19, 0xfa])
      await writer.write(drawerKick)
      writer.releaseLock()

      log.info('開放コマンド送信')
      return true
    } catch (error) {
      log.error('エラー:', error)
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
    dummyLog.info('開放')
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
