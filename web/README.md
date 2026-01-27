# FaceMoji Web

Web-based re-implementation of the FaceMoji Android app using React, TypeScript, and ONNX Runtime Web.

## Features

- **Local AI**: Uses YOLOv8n-face via ONNX Runtime Web (WASM) running entirely in the browser.
- **Privacy First**: All processing happens on your device. No images are uploaded to any server.
- **PWA Support**: Installable on Android/iOS/Desktop.
- **Share Target**: On Android, you can share images directly from other apps to FaceMoji Web.
- **Masking**: Supports Emoji and Blur (Gaussian, Pixelate) masking.
- **Custom Fonts**: Infrastructure ready for custom font support.

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Download the required Model:
   The app requires `yolov8n-face.onnx`.
   See instructions in `public/models/README.md`.

3. Run Development Server:
   ```bash
   npm run dev
   ```

## Building for Production

1. Build the app:
   ```bash
   npm run build
   ```

2. Preview the build (to test PWA service worker):
   ```bash
   npm run preview
   ```

## Deployment

The `dist/` folder is ready to be deployed to Vercel, Netlify, or GitHub Pages.
Ensure proper headers for SharedArrayBuffer (COOP/COEP) are set if using multi-threaded WASM (though this project uses default WASM backend which should work broadly).
