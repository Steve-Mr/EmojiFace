from PIL import Image
import os

def update_icons():
    source_path = 'web/public/maskable_icon.png'

    if not os.path.exists(source_path):
        print(f"Error: {source_path} not found.")
        return

    try:
        img = Image.open(source_path)

        # Define targets
        targets = [
            {'path': 'web/public/pwa-192x192.webp', 'size': (192, 192), 'format': 'WEBP'},
            {'path': 'web/public/pwa-512x512.png', 'size': (512, 512), 'format': 'PNG'},
            {'path': 'web/public/pwa-192x192-v2.webp', 'size': (192, 192), 'format': 'WEBP'},
            {'path': 'web/public/pwa-512x512-v2.png', 'size': (512, 512), 'format': 'PNG'},
        ]

        for target in targets:
            print(f"Generating {target['path']}...")
            resized = img.resize(target['size'], Image.Resampling.LANCZOS)
            resized.save(target['path'], format=target['format'])
            print(f"Saved {target['path']}")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    update_icons()
