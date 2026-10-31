# Smart Scan

[![Build](https://github.com/ste030/smart-scan/actions/workflows/build.yml/badge.svg)](https://github.com/ste030/smart-scan/actions/workflows/build.yml)

A fast, privacy-first document scanner for Android. Point, shoot, and get clean,
cropped, enhanced scans — entirely on-device, with no cloud, no account, and no ads.

Smart Scan implements the classic "pocket scanner" workflow — batch capture →
automatic edge detection → corner adjustment → enhancement → PDF/JPEG export —
using free and open Android libraries.

> **Not affiliated with any commercial scanner app.** Smart Scan is an independent
> project inspired by the document-scanner category. It contains no proprietary
> code, assets or trademarks from any commercial product.

## Features

- **Camera capture** — custom CameraX screen with batch capture, an animated
  shutter, a page counter and a last-shot thumbnail.
- **Automatic edge detection** — OpenCV finds the document quad and
  perspective-corrects it. The detected quad is expanded slightly outward so the
  document border is never clipped.
- **Manual crop** — draggable corner handles with a magnifier loupe for precise
  adjustment.
- **Enhancement filters** — Color, Enhance (contrast/saturation), Grayscale, and a
  clean Black & White via adaptive threshold.
- **Multi-page documents** — batch capture, reorder, rotate, merge and split pages.
- **Organise** — folders, search, sorting, multi-select and bulk delete.
- **Export** — PNG/JPEG images and PDF, optionally password-protected.
- **Barcode scanning** — ML Kit barcode detection.
- **On-device OCR & smart naming** — ML Kit text recognition derives document names.
- **App lock** — biometric / device-credential lock backed by encrypted preferences.
- **Theming** — 16 named colour themes over a dark UI.
- **Fully offline** — nothing ever leaves the device.

## Build

Requirements:

- JDK 17
- Android SDK Platform 35 (compileSdk 35)
- Android 7.0 (API 24) or newer device

```bash
git clone https://github.com/<your-username>/smart-scan.git
cd smart-scan
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Release builds use R8 (code + resource shrinking). Because the Gradle file declares
an ABI split for `arm64-v8a`, a release build produces an `arm64-v8a` APK. Remove
the `splits { abi { ... } }` block in `app/build.gradle.kts` to build a universal APK.

### Note on OpenCV

This project pins **OpenCV 4.11.0**. Earlier versions (4.9.x / 4.10.x) fail to load
their native library on Android 15 with `dlopen failed: empty/missing DT_HASH`, which
silently breaks edge detection. 4.11.0 includes the upstream fix — do not downgrade.

## Tech stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose (BOM 2024.09.02), Material 3 |
| Camera | CameraX 1.3.4 |
| Image processing | OpenCV 4.11.0 |
| On-device ML | ML Kit document scanner, text recognition, barcode scanning |
| Persistence | Room 2.6.1 |
| PDF | PDFBox-Android 2.0.27.0 |
| Images | Coil 2.7.0 |
| Security | AndroidX Biometric + Security-Crypto |
| Build | Gradle 8.10.2, AGP 8.6.1, Kotlin 2.0.20 |

## Project structure

```
app/src/main/java/com/ste03/smartscan/
├── MainActivity.kt          # Nav host + theme + app lock
├── core/                    # File storage helpers
├── data/                    # Room entities, DAOs, database
├── detect/                  # OpenCV edge detection + perspective warp
├── enhance/                 # Post-crop image filters
├── export/                  # PDF and image/zip exporters
├── lock/                    # Biometric / passcode app lock
├── naming/                  # OCR-driven smart document naming
├── ocr/                     # ML Kit OCR + barcode scanning
├── ui/                      # Compose screens (camera, crop, document, home, …)
├── ui/components/           # Reusable cards and dialogs
├── ui/theme/                # Colours and the 16 named themes
└── vm/                      # AppViewModel
```

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE) © 2026 Steven Hill.

## Third-party licenses

All bundled dependencies are permissively licensed (Apache-2.0 unless noted):
AndroidX / Jetpack Compose, CameraX, Material Components, Room, Coil, PDFBox-Android,
ML Kit, and OpenCV (Apache-2.0). Their respective licenses apply.
