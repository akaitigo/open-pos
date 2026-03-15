/**
 * 外部決済端末連携インターフェース (#186)
 * StarPay 等の外部決済端末との接続を抽象化する。
 */

export interface PaymentTerminalResult {
  success: boolean
  transactionId: string | null
  errorMessage: string | null
}

export interface PaymentTerminal {
  /** 決済端末に接続する */
  connect(): Promise<boolean>
  /** 決済を実行する（金額は銭単位） */
  pay(amount: number): Promise<PaymentTerminalResult>
  /** 決済をキャンセルする */
  cancel(): Promise<boolean>
  /** 接続状態を取得する */
  isConnected(): boolean
  /** 端末名を取得する */
  getName(): string
}

/**
 * StarPay 風プレースホルダー実装。
 * 本番では StarPay SDK を使用する。
 */
export class StarPayTerminal implements PaymentTerminal {
  private connected = false

  async connect(): Promise<boolean> {
    console.log('[StarPay] 接続中...')
    // プレースホルダー: 常に成功
    this.connected = true
    console.log('[StarPay] 接続完了')
    return true
  }

  async pay(amount: number): Promise<PaymentTerminalResult> {
    if (!this.connected) {
      return {
        success: false,
        transactionId: null,
        errorMessage: '端末に接続されていません',
      }
    }

    console.log(`[StarPay] 決済処理中: ${amount} 銭`)
    // プレースホルダー: 常に成功
    return {
      success: true,
      transactionId: `SP-${Date.now()}`,
      errorMessage: null,
    }
  }

  async cancel(): Promise<boolean> {
    console.log('[StarPay] 決済キャンセル')
    return true
  }

  isConnected(): boolean {
    return this.connected
  }

  getName(): string {
    return 'StarPay'
  }
}

/**
 * シミュレーション用ダミー端末。
 * テスト・デモ用に使用する。
 */
export class DummyPaymentTerminal implements PaymentTerminal {
  private connected = false

  async connect(): Promise<boolean> {
    this.connected = true
    return true
  }

  async pay(_amount: number): Promise<PaymentTerminalResult> {
    return {
      success: true,
      transactionId: `DUMMY-${Date.now()}`,
      errorMessage: null,
    }
  }

  async cancel(): Promise<boolean> {
    return true
  }

  isConnected(): boolean {
    return this.connected
  }

  getName(): string {
    return 'ダミー端末'
  }
}

/**
 * 決済端末ファクトリ。
 * 設定に基づいて適切な端末実装を返す。
 */
export function createPaymentTerminal(type: string): PaymentTerminal {
  switch (type) {
    case 'starpay':
      return new StarPayTerminal()
    default:
      return new DummyPaymentTerminal()
  }
}
