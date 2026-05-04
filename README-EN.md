# FaceMoji Privacy Edition

An on-device image privacy redaction edition based on [Steve-Mr/EmojiFace](https://github.com/Steve-Mr/EmojiFace). Phase 1 focuses on the Web/PWA app: stronger face masking, local-only processing, and metadata-stripping exports.

[<img src="assets/README/rec1_.png"
    alt="Featured on sspai"
    height="70">](https://sspai.com/post/97708)

## Get

### Web Version

No installation needed, ready to use. The privacy edition defaults to stronger irreversible face redaction and re-encodes exported images to strip EXIF/GPS metadata. Supports PWA installation.

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FSteve-Mr%2FEmojiFace&root-directory=web&project-name=facemoji-web)

> Deployment Note: Click the button above to deploy to Vercel. If deploying manually, ensure the **Root Directory** is set to `web`.
> [See Web Version Documentation](./web/README-EN.md)

### Android App

[<img src="assets/README/get-it-on-github.png"
    alt="Get it on GitHub"
    height="80">](https://github.com/Steve-Mr/EmojiFace/releases/latest)
[<img src="assets/README/get-it-on-obtainium.png"
    alt="Get it on Obtainium"
    height="80">](http://obtainium-redirect.maary.top/?r=obtainium://add/https://github.com/Steve-Mr/EmojiFace)

We offer two APK variants. Please choose the one that best fits your usage style:  

- `default`: The app icon is visible by default after installation. You can hide the icon from the in-app settings, but this might not work perfectly on all Android systems.  
- `icon-disabled`: The app icon is hidden by default after installation. This is the recommended choice if you primarily plan to use the app via the share menu from other applications. You can always unhide the icon later in the app's settings.  

|![Screenshot 1](assets/README/Screenshot_20250322-151002_FaceMoji.png)|![Screenshot 2](assets/README/Screenshot_20250322-150958_FaceMoji.png)|
|:-:|:-:|

## Features

1. Automatically detects faces in images and applies a privacy preset.
    - Privacy Edition defaults to solid redaction.
    - Face coverage is expanded by default to include edge areas around the detected face.
    - Emoji, Gaussian blur, and pixelation are still available.
2. Add, modify, or delete emoji or blur regions on images.
3. Export as PNG / WebP / JPEG through canvas re-encoding, stripping original image metadata by default.
4. Option to hide the app icon (In this state, the only way to launch the app is by sharing a picture to it from another application.).
5. Import custom emoji font files.

## Notes

1. This application is "**provided AS IS**", without warranty of any kind.
2. The face recognition model has limitations and may occasionally misidentify or fail to detect faces.
3. All image processing is done **offline**.
4. This modified edition keeps the upstream GPL-3.0 license and attribution while adding privacy-focused changes.

## Acknowledgements

This project utilizes the following resources and projects:
1. Icons are modified from [Carnival Masks Pack](https://www.freepik.com/free-vector/carnival-masks-pack_832490.htm#fromView=search&page=1&position=25&uuid=19121ed9-3676-4304-a9af-fdd72fe1528c&query=Masquerade+mask+icon) by freepik.
2. Uses the pre-trained face detection model from [derronqi/yolov8-face](https://github.com/derronqi/yolov8-face).
