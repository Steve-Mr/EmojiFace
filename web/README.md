# FaceMoji Web

基于 React、TypeScript 和 ONNX Runtime Web 对 FaceMoji Android 应用的 Web 版重写。

> **注意**：本 Web 应用完全由 Jules (Vibe Coding) 生成。目前整体可用，但代码质量及稳定性不作保证。

## 功能特点

- **本地 AI**：通过 ONNX Runtime Web (WASM) 在浏览器中完全运行 YOLOv8n-face 模型。
- **隐私优先**：所有处理均在您的设备上进行。没有任何图片会上传到服务器。
- **支持 PWA**：可安装在 Android/iOS/桌面端。
- **分享目标**：在 Android 上，您可以直接从其他应用将图片分享到 FaceMoji Web。
- **遮罩效果**：支持 Emoji 和模糊（高斯模糊、像素化）遮罩。
- **自定义字体**：已准备好自定义字体支持的基础架构。

## 设置与运行

1. 安装依赖：
   ```bash
   npm install
   ```

2. 运行开发服务器：
   ```bash
   npm run dev
   ```

   *注意*：模型文件 `yolov8n-face.onnx` 包含在 `public/models/` 中。这是一个 5 通道输出版本（边界框 + 分数，无关键点）。

## 构建生产版本

1. 构建应用：
   ```bash
   npm run build
   ```

2. 预览构建（用于测试 PWA Service Worker）：
   ```bash
   npm run preview
   ```

## 部署

部署您自己的 FaceMoji Web 实例的最简单方法是使用 Vercel。

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FSteve-Mr%2FEmojiFace&root-directory=web&project-name=facemoji-web)

**注意**：部署时，Vercel 应会自动检测设置。如果您手动配置，请确保 **Root Directory（根目录）** 设置为 `web`。
