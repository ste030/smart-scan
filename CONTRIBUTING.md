# Contributing to Smart Scan

Thanks for your interest in improving Smart Scan.

## Getting started

1. Fork the repository and clone your fork.
2. Open the project in Android Studio (or build from the command line with
   `./gradlew assembleDebug`).
3. Create a feature branch: `git checkout -b feature/my-change`.
4. Make your change, build it, and test on a device if you can.
5. Open a pull request describing what changed and why.

## Guidelines

- **Keep it on-device.** This app is intentionally offline and account-free. PRs that
  add network calls, analytics or cloud sync will be declined.
- **No proprietary code or assets.** Do not add code, images, fonts or other assets
  copied from a commercial app.
- **Match the existing style.** Kotlin, Jetpack Compose, Material 3, dark UI.
- **One logical change per PR.** Smaller PRs are easier to review.
- **Describe testing.** Note the device/Android version you tested on, or state that
  you only verified the build.

## Reporting bugs

Open an issue with:

- What you expected vs what happened.
- Steps to reproduce.
- Device model and Android version.
- A screenshot or logcat snippet if relevant.

## Code of conduct

Be respectful and constructive. Harassment or abusive behaviour is not tolerated.
