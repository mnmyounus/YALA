# YALA — Your App Lock and Analyzer

Developed by **MNM YOUNUS**. 100% offline, zero ads, no `INTERNET` permission.

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- minSdk 21 (Android 5.0) · targetSdk/compileSdk 34
- Android TV supported (leanback launcher entry, D-pad focusable controls, TV banner)

## Project layout

```
YALA/
├─ .github/workflows/build.yml        CI: unsigned debug APK on every push to main / tag
├─ settings.gradle.kts
├─ build.gradle.kts                   Root plugins (AGP, Kotlin, Hilt, KSP)
├─ gradle/wrapper/                    Wrapper properties (run `gradle wrapper` once locally)
└─ app/
   ├─ build.gradle.kts
   ├─ proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ res/{values,xml,drawable}/
      └─ java/com/mnmyounus/yala/
         ├─ YalaApp.kt                Hilt application
         ├─ MainActivity.kt           Compose NavHost: onboarding / home / settings / gallery
         ├─ core/theme/Theme.kt       Light, Dark, System themes + TV typography
         ├─ core/util/CryptoUtils.kt  Salted SHA-256 hashing, 12-char recovery key
         ├─ di/AppModule.kt           Hilt bindings
         ├─ domain/model/             LockType, LockedApp, IntruderShot, …
         ├─ domain/repository/        LockRepository, IntruderRepository interfaces
         ├─ data/local/               EncryptedSharedPreferences + Room (intruder shots)
         ├─ data/repository/          Repository implementations
         ├─ data/service/             Accessibility service, device admin, foreground service
         └─ ui/                       lock / home / gallery / settings / onboarding
```

## Architecture

Clean Architecture with MVVM and unidirectional data flow:

```
UI (Compose)  →  ViewModel (StateFlow<UiState>)  →  Repository (domain interface)
                                                        ↓
                                        EncryptedSharedPreferences · Room · CameraX
```

Every screen exposes a single immutable `UiState`; events flow up as plain function
calls, state flows down through `StateFlow`. Hilt provides repositories as singletons
so the accessibility service and the UI observe the same locked-app table.

## Feature map

| Requirement | Where |
|---|---|
| 4-digit PIN | `ui/lock/LockScreen.kt` → `PinPad` |
| 8-char password | `ui/lock/LockScreen.kt` → `PasswordEntry` |
| Pattern lock | `ui/lock/LockScreen.kt` → `PatternLock` |
| Image-sequence lock (5 uploads, 3–4 secret order) | `LockScreen.ImageSequenceLock` + `onboarding/LockSetupScreen.kt` |
| 12-char recovery key, copy / save to file | `CryptoUtils.generateRecoveryKey`, `SecurePrefs`, settings + onboarding |
| Lock user **and** system apps | `LockRepositoryImpl.installedApps(includeSystem = true)` |
| Global vs per-app lock type and hint | `SecurePrefs.lockMode`, `SettingsScreen` |
| Accessibility foreground detection | `data/service/YalaAccessibilityService.kt` |
| Device admin anti-uninstall | `data/service/YalaDeviceAdminReceiver.kt` + `res/xml/device_admin.xml` |
| Silent intruder capture, split galleries | `data/repository/IntruderRepositoryImpl.kt`, `ui/gallery/` |
| Light / Dark / System themes | `core/theme/Theme.kt` |
| TV layout and D-pad | `LocalIsTv`, leanback intent filter, focusable pad keys |
| Onboarding walkthrough + disclaimer | `ui/onboarding/OnboardingScreen.kt` |

## Security notes

- Credentials are never stored in plain text: salted SHA-256 with 20,000 iterations,
  written to `EncryptedSharedPreferences` (AES-256-GCM).
- The recovery key is hashed the same way; the plaintext is held only until the user
  finishes onboarding, then wiped.
- The lock activity sets `FLAG_SECURE`, blocks the Back button and sends the intruder
  to the home screen instead of the protected app.
- Intruder photos are written to app-private internal storage — not the system gallery.

## Building

```bash
gradle wrapper --gradle-version 8.7   # first time only, generates gradlew
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## CI/CD

`.github/workflows/build.yml` runs on every push to `main` and on `v*` tags. It builds
an **unsigned debug APK** — no keystore or secrets required — uploads it as the
`YALA-debug-apk` workflow artifact, and attaches it to the GitHub Release when the
trigger is a tag.

## Before first run

Add a launcher icon at `app/src/main/res/mipmap-*/ic_launcher.png` (or generate one via
Android Studio's Image Asset wizard) — the manifest references `@mipmap/ic_launcher`.
