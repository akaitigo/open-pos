import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { initializeRuntimeConfig } from '@/lib/runtime-config'
import './index.css'

const root = document.getElementById('root')
if (!root) throw new Error('Root element not found')

async function bootstrap() {
  await initializeRuntimeConfig()

  createRoot(root).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

void bootstrap()
