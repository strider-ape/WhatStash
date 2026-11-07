# WhatStash Project Plan

WhatStash is a privacy-first, open-source WhatsApp status saver for Android.
It is a fork of **WhatSave** by Christians Martínez A. ([@mardous](https://github.com/mardous)), licensed GPL-3.0.

## Decisions so far

| Topic | Decision |
|---|---|
| Name | **WhatStash** (sorts right below WhatsApp / WhatsApp Business in alphabetical app drawers) |
| Base code | WhatSave **2.1.0-beta.3** (March 2025, from the Wayback Machine archive of the GitHub tag tarball), placed on top of the original 263-commit history (v1.2.5) |
| License | GPL-3.0, keep all original copyright headers, credit the original author in README and About |
| Distribution | Open-source channels only: GitHub Releases → IzzyOnDroid → F-Droid. **No Google Play.** |
| Stack | Keep the existing one: Kotlin, XML views + View Binding, Navigation, Room, Koin, Coil, Media3, coroutines |
| Deleted-message catcher | Keep, but **opt-in**, local-only, with retention limits |
| Network | **No `INTERNET` permission** (done in Phase 1) |

## What the base already has (2.1.0-beta.3)

- WhatsApp, WhatsApp Business (and OGWhatsApp) statuses through the Storage Access Framework
- Image/video tabs, saved tab, multi-selection
- Playback viewer with a Media3 video player and playback speed
- Edge-to-edge insets, Material You, night/black themes, 17 languages
- "Chat without saving the number" tool, deleted-message catcher (NotificationListenerService)
- `normal` (Firebase) and `fdroid` (no analytics) product flavors; GitHub Actions workflows
- Builds without private keys (falls back to debug signing when `keystore.properties` is missing)

Things to remove or fix: Firebase Analytics/Crashlytics (`normal` flavor), the Ktor-based GitHub update checker,
`INTERNET`/`ACCESS_NETWORK_STATE` permissions, WhatSave branding, F-Droid/Weblate links pointing at the dead project.

## Hard limits (things no status saver can do)

- **No contact attribution.** Status files have random names; we cannot tell who posted a status, so no per-contact filters or auto-save rules.
- **Depends on WhatsApp's storage layout.** Statuses live in `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`. If WhatsApp moves them to `Android/data`, all status savers break. Keep the path logic isolated (`WaClient`, `WaDirectory`) so a change is a small fix.
- **Dual/cloned WhatsApp** (Xiaomi Second Space, Samsung Dual Messenger) lives in another Android user profile and is generally not reachable through SAF.

---

## Phase 0: Import

- [x] Clone original history into this repo, push to `strider-ape/WhatStash` `main`
- [x] Verify the archived tarball (Gradle wrapper jar matches the official Gradle 8.7/8.8 checksum)
- [x] Lay 2.1.0-beta.3 over the history
- [x] Commit the import and push it
- [x] Rewrite the README and credit the original author
- [x] Confirm it builds (`./gradlew assembleFdroidDebug`, now `assembleDebug`) before changing anything
  (2026-11-07: builds cleanly with JDK 21, AGP 8.8.2, Gradle 8.10.2; 14 MB APK, no Firebase classes;
  still has `INTERNET` and `ACCESS_NETWORK_STATE`, as expected before Phase 1)
- [ ] Install on a phone and check that statuses load (no device connected during the build check)

## Phase 1: Rebrand and de-Google (v1.0.0, feature parity)

Done on 2026-11-07 unless marked open.

1. **Identity**
   - [x] `applicationId` `io.github.striderape.whatstash`; Kotlin package/namespace kept as `com.simplified.wsstatussaver` to keep the diff small
   - [x] App name "WhatStash" in the label, APK name and user-facing strings (all locales)
   - [x] New vector launcher icon with a monochrome (themed) layer; vector legacy icons for Android 7.x; new F-Droid icon
   - [x] Version 1.0.0 (versionCode 1000)
2. **Remove tracking and networking**
   - [x] Firebase Analytics/Crashlytics, both product flavors, the analytics setting and its strings
   - [x] Update checker (`update/`, `network/KtorClient.kt`, update dialog, Ktor, versioncompare) and its settings
   - [x] `INTERNET` and `ACCESS_NETWORK_STATE` removed (the latter is stripped from Media3's manifest with `tools:node="remove"`)
   - [ ] Check on a device that "chat without saving" and video playback still work without network permissions
3. **Toolchain**
   - [x] Gradle 9.9.0, AGP 9.5.1 (built-in Kotlin), Kotlin 2.4.21, KSP 2.3.13, current AndroidX/Media3/Room/Koin/Coil
   - [x] compileSdk 37, targetSdk 36, minSdk 24
4. **Docs and credits**
   - [x] README (English and Spanish) with a Credits section
   - [x] About screen credits Christians Martínez A. (mardous), adds the maintainer, links to this repo
   - [x] PRIVACY.md rewritten, CONTRIBUTING.md and Code of Conduct point at this project
   - [x] F-Droid metadata: title, descriptions, 1.0.0 changelog
5. **CI**
   - [x] `ci.yml`: wrapper validation, lint, debug and release builds on every push/PR to `main`
   - [x] `release.yml`: signed APK and GitHub release from a `v*` tag, keystore from repository secrets
   - [ ] Add the signing secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) before the first release
   - [ ] Unit tests (there are none yet)
6. **Verification**
   - [ ] Real device or emulator on Android 11+ and on Android 10 or older
   - [ ] WhatsApp and WhatsApp Business, images and videos, save/share/delete, catcher opt-in
   - [ ] Release build on a device (R8 now runs with full optimization)

## Phase 2: Better than the original (v1.1)

- **Expiry countdown** on each status ("gone in 3 h", from file modified time + 24 h)
- **Duplicate detection** by content hash: "already saved" badge, never save the same status twice
- **Repost**: share a status straight to WhatsApp / WhatsApp Business
- **Frame grab**: save the current video frame as an image
- **Saved library**: filter by type and date, favorites, storage used, bulk delete

## Phase 3: Automation (v1.2)

- **Auto-save** (opt-in): periodic WorkManager scan, filters (images / videos / both)
- Optional "new statuses" notification
- Home-screen widget and Quick Settings tile (new-status count, one tap to open)

## Phase 4: Privacy and tools (v1.3)

- **App lock** with biometrics / device credential (`androidx.biometric`)
- **Hide saved files from the gallery** toggle (`.nomedia` in the save folder)
- **Backup / export** saved statuses to any folder via SAF
- **Deleted-message catcher hardening**: clear opt-in onboarding, retention limit (e.g. 7/30 days), excluded from Android backups, one-tap wipe
- **Chat without saving**: number from clipboard, recent numbers list

## Phase 5: Distribution

1. GitHub Releases (signed APK, changelog)
2. IzzyOnDroid repo (picks up GitHub releases)
3. F-Droid main repo: metadata merge request, aim for reproducible builds
4. Optional: own Weblate project for translations

## Settled questions

- `applicationId`: `io.github.striderape.whatstash`
- OGWhatsApp support: kept
- Version scheme: fresh start at 1.0.0
