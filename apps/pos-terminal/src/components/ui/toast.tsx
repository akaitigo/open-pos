import { cn } from '@/lib/utils'
import { useToast, type ToasterToast } from '@/hooks/use-toast'
import { X } from 'lucide-react'

function ToastClose({ onClose }: { onClose: () => void }) {
  return (
    <button
      aria-label="Close notification"
      className="absolute right-1 top-1 rounded-md p-1 text-foreground/50 opacity-0 transition-opacity hover:text-foreground focus:opacity-100 focus:outline-hidden group-hover:opacity-100"
      onClick={onClose}
    >
      <X className="h-4 w-4" />
    </button>
  )
}

function ToastItem({ toast, onDismiss }: { toast: ToasterToast; onDismiss: (id: string) => void }) {
  return (
    <div
      className={cn(
        'group pointer-events-auto relative flex w-full items-center justify-between space-x-2 overflow-hidden rounded-md border p-4 pr-6 shadow-lg transition-all',
        toast.variant === 'destructive'
          ? 'border-destructive bg-destructive text-destructive-foreground'
          : 'border bg-background text-foreground',
      )}
    >
      <div className="grid gap-1">
        {toast.title && <div className="text-sm font-semibold">{toast.title}</div>}
        {toast.description && <div className="text-sm opacity-90">{toast.description}</div>}
      </div>
      {toast.action}
      <ToastClose onClose={() => onDismiss(toast.id)} />
    </div>
  )
}

export function Toaster() {
  const { toasts, dismiss } = useToast()

  return (
    <div className="pointer-events-none fixed right-0 top-0 z-100 flex max-h-screen w-full flex-col p-4 md:max-w-[420px]">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={dismiss} />
      ))}
    </div>
  )
}
