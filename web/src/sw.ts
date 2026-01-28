/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching';
import { clientsClaim } from 'workbox-core';
import { openDB } from 'idb';

declare const self: ServiceWorkerGlobalScope;

cleanupOutdatedCaches();
precacheAndRoute(self.__WB_MANIFEST);

self.skipWaiting();
clientsClaim();

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
