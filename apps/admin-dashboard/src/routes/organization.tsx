import { useCallback, useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'
import { OrganizationSchema } from '@shared-types/openpos'
import type { Organization } from '@shared-types/openpos'

export function OrganizationPage() {
  const [org, setOrg] = useState<Organization | null>(null)
  const [name, setName] = useState('')
  const [businessType, setBusinessType] = useState('')
  const [invoiceNumber, setInvoiceNumber] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const fetchOrg = useCallback(async () => {
    try {
      const data = await api.get('/api/organization', OrganizationSchema)
      setOrg(data)
      setName(data.name)
      setBusinessType(data.businessType)
      setInvoiceNumber(data.invoiceNumber ?? '')
    } catch {
      // 組織情報がまだない場合
    }
  }, [])

  useEffect(() => {
    fetchOrg()
  }, [fetchOrg])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setMessage(null)
    try {
      const updated = await api.put(
        '/api/organization',
        {
          name,
          businessType,
          invoiceNumber: invoiceNumber || null,
        },
        OrganizationSchema,
      )
      setOrg(updated)
      setMessage('設定を保存しました')
    } catch {
      setMessage('保存に失敗しました')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Header title="組織設定" />
      <div className="flex flex-1 flex-col gap-4 p-4 max-w-2xl">
        <Card>
          <CardHeader>
            <CardTitle>基本情報</CardTitle>
          </CardHeader>
          <CardContent>
            {org === null ? (
              <p className="text-sm text-muted-foreground">読み込み中...</p>
            ) : (
              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="org-name">組織名 *</Label>
                  <Input
                    id="org-name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="org-business-type">業種</Label>
                  <Input
                    id="org-business-type"
                    value={businessType}
                    onChange={(e) => setBusinessType(e.target.value)}
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="org-invoice-number">
                    適格請求書発行事業者登録番号（インボイス番号）
                  </Label>
                  <Input
                    id="org-invoice-number"
                    value={invoiceNumber}
                    onChange={(e) => setInvoiceNumber(e.target.value)}
                    placeholder="T1234567890123"
                  />
                  <p className="text-xs text-muted-foreground">
                    インボイス制度に対応する場合、「T」で始まる13桁の番号を入力してください
                  </p>
                </div>

                <div className="flex items-center gap-4">
                  <Button type="submit" disabled={saving}>
                    {saving ? '保存中...' : '設定を保存'}
                  </Button>
                  {message && <span className="text-sm text-muted-foreground">{message}</span>}
                </div>
              </form>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>組織ID</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              この値はシステム内部で使用されます。変更できません。
            </p>
            <code className="mt-2 block text-sm font-mono bg-muted p-2 rounded">
              {org?.id ?? '...'}
            </code>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
