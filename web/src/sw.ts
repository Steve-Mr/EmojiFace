/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching';
import { clientsClaim } from 'workbox-core';
import { registerRoute } from 'workbox-routing';
import { CacheFirst } from 'workbox-strategies';
import { CacheableResponsePlugin } from 'workbox-cacheable-response';
import { ExpirationPlugin } from 'workbox-expiration';
import { openDB } from 'idb';

declare const self: ServiceWorkerGlobalScope;

cleanupOutdatedCaches();
precacheAndRoute(self.__WB_MANIFEST);

self.skipWaiting();
clientsClaim();

// Cache ONNX models and WASM files
registerRoute(
  ({ url }) => url.pathname.endsWith('.onnx') || url.pathname.endsWith('.wasm'),
  new CacheFirst({
    cacheName: 'models-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200],
      }),
      new ExpirationPlugin({
        maxEntries: 10,
        maxAgeSeconds: 60 * 60 * 24 * 365, // 1 year
      }),
    ],
  })
);

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (event.request.method === 'POST' && (url.pathname === '/' || url.pathname === '/index.html')) {
    event.respondWith(
      (async () => {
        try {
            const formData = await event.request.formData();
            const file = formData.get('image');

            if (file && file instanceof File) {
              const db = await openDB('facemoji-share', 1, {
                 upgrade(db) {
                     db.createObjectStore('shared-files');
                 }
              });
              await db.put('shared-files', file, 'latest');
              return Response.redirect('/?shared=true', 303);
            }
        } catch (e) {
            console.error('Share Target Error', e);
        }

        return Response.redirect('/', 303);
      })()
    );
  }
});
