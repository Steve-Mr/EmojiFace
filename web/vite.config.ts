import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { viteStaticCopy } from 'vite-plugin-static-copy'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    viteStaticCopy({
      targets: [
        {
          src: 'node_modules/onnxruntime-web/dist/*.wasm',
          dest: '.'
        }
      ]
    }),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon.png'],
      manifest: {
        name: 'FaceMoji Web',
        short_name: 'FaceMoji',
        description: 'Face privacy protection with Emojis and Blur',
        theme_color: '#ffffff',
        display: 'standalone',
        icons: [
          {
            src: 'pwa-192x192.webp',
            sizes: '192x192',
            type: 'image/webp',
            purpose: 'any'
          },
          {
            src: 'pwa-192x192.webp',
            sizes: '192x192',
            type: 'image/webp',
            purpose: 'maskable'
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any'
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable'
          }
        ],
        screenshots: [
          {
            src: 'screenshot-desktop.png',
            sizes: '1280x800',
            type: 'image/png',
            form_factor: 'wide',
            label: 'Desktop Editor'
          },
          {
            src: 'screenshot-desktop.png',
            sizes: '1280x800',
            type: 'image/png',
            label: 'Editor View'
          }
        ],
        share_target: {
          action: "/",
          method: "POST",
          enctype: "multipart/form-data",
          params: {
            title: "title",
            text: "text",
            url: "url",
            files: [
              {
                name: "image",
                accept: ["image/*", ".png", ".jpg", ".jpeg", ".webp"]
              }
            ]
          }
        }
      },
      injectManifest: {
          maximumFileSizeToCacheInBytes: 30 * 1024 * 1024,
          globPatterns: ['**/*.{js,css,html,ico,png,svg,wasm}', '**/*.onnx']
      }
    })
  ],
  optimizeDeps: {
    exclude: ['onnxruntime-web']
  }
})
