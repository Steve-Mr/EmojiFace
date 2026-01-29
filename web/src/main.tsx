import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { TranslationProvider } from './i18n/TranslationContext'
import { ThemeProvider } from './components/ThemeProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <TranslationProvider>
      <ThemeProvider>
        <App />
      </ThemeProvider>
    </TranslationProvider>
  </StrictMode>,
)
