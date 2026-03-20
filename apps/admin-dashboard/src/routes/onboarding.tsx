/**
 * テナント オンボーディングウィザード (#191, #440)
 * Steps: 組織情報 -> 店舗設定 -> 初期商品 -> スタッフ -> 確認
 * プレースホルダー完了をAPI経由の実プロビジョニングフローに置換。
 */

import { useState } from 'react'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { api } from '@/lib/api'
import { toast } from '@/hooks/use-toast'
import { OrganizationSchema, StoreSchema, StaffSchema } from '@shared-types/openpos'

type OnboardingStep = 'ORG_INFO' | 'STORE_SETUP' | 'PRODUCTS' | 'STAFF' | 'REVIEW'

const STEPS: { key: OnboardingStep; label: string }[] = [
  { key: 'ORG_INFO', label: '組織情報' },
  { key: 'STORE_SETUP', label: '店舗設定' },
  { key: 'PRODUCTS', label: '初期商品' },
  { key: 'STAFF', label: 'スタッフ' },
  { key: 'REVIEW', label: '確認' },
]

interface OnboardingData {
  orgName: string
  businessType: string
  invoiceNumber: string
  storeName: string
  storeAddress: string
  storePhone: string
  productNames: string[]
  staffName: string
  staffEmail: string
  staffPin: string
}

const initialData: OnboardingData = {
  orgName: '',
  businessType: 'RETAIL',
  invoiceNumber: '',
  storeName: '',
  storeAddress: '',
  storePhone: '',
  productNames: [''],
  staffName: '',
  staffEmail: '',
  staffPin: '',
}

/** プロビジョニング結果 */
interface ProvisioningResult {
  organizationId: string
  storeId: string
  staffId: string
  productCount: number
}

/**
 * API経由で組織・店舗・スタッフを一括プロビジョニングする。
 * NOTE: PII (email, PIN, phone, address) をログに出力しないこと。
 */
async function provisionTenant(data: OnboardingData): Promise<ProvisioningResult> {
  // Step 1: 組織作成
  const org = await api.post(
    '/api/organizations',
    {
      name: data.orgName,
      businessType: data.businessType,
      invoiceNumber: data.invoiceNumber || undefined,
    },
    OrganizationSchema,
  )

  // 組織IDを設定してテナントコンテキストを有効化
  api.setOrganizationId(org.id)

  // Step 2: 店舗作成
  const store = await api.post(
    '/api/stores',
    {
      name: data.storeName,
      address: data.storeAddress || undefined,
      phone: data.storePhone || undefined,
      timezone: 'Asia/Tokyo',
    },
    StoreSchema,
  )

  // Step 3: スタッフ作成
  const staff = await api.post(
    '/api/staff',
    {
      storeId: store.id,
      name: data.staffName,
      email: data.staffEmail,
      role: 'OWNER',
      pin: data.staffPin,
    },
    StaffSchema,
  )

  // Step 4: 初期商品登録（商品名のみの簡易登録）
  const validProducts = data.productNames.filter((n) => n.trim())
  const ProductResponseSchema = z.object({ id: z.string() }).passthrough()

  for (const productName of validProducts) {
    await api.post(
      '/api/products',
      {
        name: productName.trim(),
        price: 0, // オープンプライスとして作成（後で設定可能）
      },
      ProductResponseSchema,
    )
  }

  return {
    organizationId: org.id,
    storeId: store.id,
    staffId: staff.id,
    productCount: validProducts.length,
  }
}

export function OnboardingPage() {
  const [currentStep, setCurrentStep] = useState<OnboardingStep>('ORG_INFO')
  const [data, setData] = useState<OnboardingData>(initialData)
  const [completed, setCompleted] = useState(false)
  const [provisioning, setProvisioning] = useState(false)
  const [provisionError, setProvisionError] = useState<string | null>(null)

  const currentIndex = STEPS.findIndex((s) => s.key === currentStep)

  function goNext() {
    const next = STEPS[currentIndex + 1]
    if (currentIndex < STEPS.length - 1 && next) {
      setCurrentStep(next.key)
    }
  }

  function goBack() {
    const prev = STEPS[currentIndex - 1]
    if (currentIndex > 0 && prev) {
      setCurrentStep(prev.key)
    }
  }

  async function handleComplete() {
    setProvisioning(true)
    setProvisionError(null)

    try {
      await provisionTenant(data)
      setCompleted(true)
      toast({
        title: 'セットアップ完了',
        description: '組織・店舗・スタッフの初期設定が完了しました。',
      })
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'プロビジョニングに失敗しました'
      setProvisionError(message)
      toast({
        title: 'エラー',
        description: message,
        variant: 'destructive',
      })
    } finally {
      setProvisioning(false)
    }
  }

  /** 入力バリデーション: 必須フィールドが入力されているか */
  function isStepValid(): boolean {
    switch (currentStep) {
      case 'ORG_INFO':
        return data.orgName.trim().length > 0
      case 'STORE_SETUP':
        return data.storeName.trim().length > 0
      case 'PRODUCTS':
        return true // 任意入力
      case 'STAFF':
        return (
          data.staffName.trim().length > 0 &&
          data.staffEmail.trim().length > 0 &&
          /^\d{4,8}$/.test(data.staffPin)
        )
      case 'REVIEW':
        return true
      default:
        return false
    }
  }

  if (completed) {
    return (
      <div
        className="flex flex-col items-center justify-center min-h-[60vh] p-8"
        data-testid="onboarding-complete"
      >
        <h1 className="text-3xl font-bold mb-4">セットアップ完了</h1>
        <p className="text-muted-foreground mb-8">
          組織・店舗・スタッフの初期設定が完了しました。ダッシュボードから各種設定を管理できます。
        </p>
        <Button onClick={() => (window.location.href = '/')} data-testid="go-to-dashboard">
          ダッシュボードへ
        </Button>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto p-6" data-testid="onboarding-wizard">
      {/* ステッププログレス */}
      <div className="flex items-center justify-between mb-8" data-testid="step-progress">
        {STEPS.map((step, i) => (
          <div key={step.key} className="flex items-center">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                i <= currentIndex
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted text-muted-foreground'
              }`}
              data-testid={`step-indicator-${step.key}`}
            >
              {i + 1}
            </div>
            <span
              className={`ml-2 text-sm hidden sm:inline ${
                i === currentIndex ? 'font-medium' : 'text-muted-foreground'
              }`}
            >
              {step.label}
            </span>
            {i < STEPS.length - 1 && <div className="w-8 h-px bg-border mx-2 hidden sm:block" />}
          </div>
        ))}
      </div>

      {/* ステップコンテンツ */}
      <Card>
        <CardHeader>
          <CardTitle>{STEPS[currentIndex]?.label}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {currentStep === 'ORG_INFO' && (
            <>
              <div>
                <Label htmlFor="orgName">組織名 *</Label>
                <Input
                  id="orgName"
                  value={data.orgName}
                  onChange={(e) => setData({ ...data, orgName: e.target.value })}
                  placeholder="例: カフェ太郎"
                  data-testid="input-org-name"
                />
              </div>
              <div>
                <Label htmlFor="businessType">業態</Label>
                <select
                  id="businessType"
                  className="w-full rounded-md border px-3 py-2 text-sm"
                  value={data.businessType}
                  onChange={(e) => setData({ ...data, businessType: e.target.value })}
                  data-testid="select-business-type"
                >
                  <option value="RETAIL">小売</option>
                  <option value="RESTAURANT">飲食</option>
                  <option value="OTHER">その他</option>
                </select>
              </div>
              <div>
                <Label htmlFor="invoiceNumber">インボイス登録番号</Label>
                <Input
                  id="invoiceNumber"
                  value={data.invoiceNumber}
                  onChange={(e) => setData({ ...data, invoiceNumber: e.target.value })}
                  placeholder="例: T1234567890123"
                  data-testid="input-invoice-number"
                />
              </div>
            </>
          )}

          {currentStep === 'STORE_SETUP' && (
            <>
              <div>
                <Label htmlFor="storeName">店舗名 *</Label>
                <Input
                  id="storeName"
                  value={data.storeName}
                  onChange={(e) => setData({ ...data, storeName: e.target.value })}
                  placeholder="例: 本店"
                  data-testid="input-store-name"
                />
              </div>
              <div>
                <Label htmlFor="storeAddress">住所</Label>
                <Input
                  id="storeAddress"
                  value={data.storeAddress}
                  onChange={(e) => setData({ ...data, storeAddress: e.target.value })}
                  data-testid="input-store-address"
                />
              </div>
              <div>
                <Label htmlFor="storePhone">電話番号</Label>
                <Input
                  id="storePhone"
                  value={data.storePhone}
                  onChange={(e) => setData({ ...data, storePhone: e.target.value })}
                  data-testid="input-store-phone"
                />
              </div>
            </>
          )}

          {currentStep === 'PRODUCTS' && (
            <>
              <p className="text-sm text-muted-foreground">
                初期商品を登録します（後から追加・変更可能）。
              </p>
              {data.productNames.map((name, i) => (
                <div key={i} className="flex gap-2">
                  <Input
                    value={name}
                    onChange={(e) => {
                      const names = [...data.productNames]
                      names[i] = e.target.value
                      setData({ ...data, productNames: names })
                    }}
                    placeholder={`商品名 ${i + 1}`}
                    data-testid={`input-product-${i}`}
                  />
                  {i === data.productNames.length - 1 && (
                    <Button
                      variant="outline"
                      onClick={() => setData({ ...data, productNames: [...data.productNames, ''] })}
                      data-testid="add-product-button"
                    >
                      +
                    </Button>
                  )}
                </div>
              ))}
            </>
          )}

          {currentStep === 'STAFF' && (
            <>
              <div>
                <Label htmlFor="staffName">スタッフ名 *</Label>
                <Input
                  id="staffName"
                  value={data.staffName}
                  onChange={(e) => setData({ ...data, staffName: e.target.value })}
                  placeholder="例: 山田太郎"
                  data-testid="input-staff-name"
                />
              </div>
              <div>
                <Label htmlFor="staffEmail">メールアドレス *</Label>
                <Input
                  id="staffEmail"
                  type="email"
                  value={data.staffEmail}
                  onChange={(e) => setData({ ...data, staffEmail: e.target.value })}
                  data-testid="input-staff-email"
                />
              </div>
              <div>
                <Label htmlFor="staffPin">PIN（4-8桁） *</Label>
                <Input
                  id="staffPin"
                  type="password"
                  maxLength={8}
                  value={data.staffPin}
                  onChange={(e) => setData({ ...data, staffPin: e.target.value })}
                  data-testid="input-staff-pin"
                />
              </div>
            </>
          )}

          {currentStep === 'REVIEW' && (
            <div className="space-y-3" data-testid="review-summary">
              <div>
                <span className="text-sm text-muted-foreground">組織名:</span>{' '}
                <span className="font-medium">{data.orgName}</span>
              </div>
              <div>
                <span className="text-sm text-muted-foreground">業態:</span>{' '}
                <span className="font-medium">{data.businessType}</span>
              </div>
              <div>
                <span className="text-sm text-muted-foreground">店舗名:</span>{' '}
                <span className="font-medium">{data.storeName}</span>
              </div>
              <div>
                <span className="text-sm text-muted-foreground">商品数:</span>{' '}
                <span className="font-medium">
                  {data.productNames.filter((n) => n.trim()).length}件
                </span>
              </div>
              <div>
                <span className="text-sm text-muted-foreground">スタッフ:</span>{' '}
                <span className="font-medium">{data.staffName}</span>
              </div>
            </div>
          )}

          {provisionError && (
            <div
              className="rounded-md bg-destructive/10 p-3 text-sm text-destructive"
              data-testid="provision-error"
            >
              {provisionError}
            </div>
          )}
        </CardContent>
      </Card>

      {/* ナビゲーション */}
      <div className="flex justify-between mt-6">
        <Button
          variant="outline"
          onClick={goBack}
          disabled={currentIndex === 0 || provisioning}
          data-testid="step-back"
        >
          戻る
        </Button>
        {currentStep === 'REVIEW' ? (
          <Button onClick={handleComplete} disabled={provisioning} data-testid="complete-setup">
            {provisioning ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                プロビジョニング中...
              </>
            ) : (
              'セットアップ完了'
            )}
          </Button>
        ) : (
          <Button onClick={goNext} disabled={!isStepValid()} data-testid="step-next">
            次へ
          </Button>
        )}
      </div>
    </div>
  )
}
