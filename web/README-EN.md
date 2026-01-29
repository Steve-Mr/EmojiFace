# FaceMoji Web

Web-based re-implementation of the FaceMoji Android app using React, TypeScript, and ONNX Runtime Web.

> **Note**: This Web application was entirely generated using Jules (Vibe Coding). While functional, the code quality and stability are not guaranteed.

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

2. Run Development Server:
   ```bash
   npm run dev
   ```

   *Note*: The model file `yolov8n-face.onnx` is included in `public/models/`. It is a 5-channel output version (bbox + score, no keypoints).

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

The easiest way to deploy your own instance of FaceMoji Web is to use Vercel.

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FSteve-Mr%2FEmojiFace&root-directory=web&project-name=facemoji-web)

**Note:** When deploying, Vercel should automatically detect the settings. If you are configuring it manually, ensure the **Root Directory** is set to `web`.
