# WhatStash — Project Plan

WhatStash is a privacy-first, open-source WhatsApp status saver for Android.
It is a fork of **WhatSave** by Christians Martínez A. ([@mardous](https://github.com/mardous)), licensed GPL-3.0.

## Decisions so far

| Topic | Decision |
|---|---|
| Name | **WhatStash** (sorts right below WhatsApp / WhatsApp Business in alphabetical app drawers) |
| Base code | WhatSave **2.1.0-beta.3** (March 2025, from the Wayback Machine archive of the GitHub tag tarball), placed on top of the original 263-commit history (v1.2.5) |
| License | GPL-3.0, keep all original copyright headers, credit the original author in README and About |
| Distribution | Open-source channels only: GitHub Releases → IzzyOnDroid → F-Droid. **No Google Play.** |
| Stack | Keep the existing one: Kotlin, XML views + View Binding, Navigation, Room, Koin, Glide, Media3, coroutines |
| Deleted-message catcher | Keep, but **opt-in**, local-only, with retention limits |
| Network | Goal: **no `INTERNET` permission at all** |

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

## Phase 0 — Import (done locally, not yet committed)

- [x] Clone original history into this repo, push to `strider-ape/WhatStash` `main`
- [x] Verify the archived tarball (Gradle wrapper jar matches the official Gradle 8.7/8.8 checksum)
- [x] Lay 2.1.0-beta.3 over the history
- [ ] Commit: "Import WhatSave 2.1.0-beta.3 source by Christians Martínez A."
- [ ] Confirm it builds (`./gradlew assembleFdroidDebug`) before changing anything

## Phase 1 — Rebrand and de-Google → **v1.0.0 (feature parity)**

1. **Identity**
   - New `applicationId` (proposal: `io.github.striderape.whatstash`); keep the Kotlin package/namespace for now to keep the diff small (rename later if wanted)
   - App name "WhatStash" everywhere, new launcher icon + monochrome themed icon
   - Version: start at 1.0.0 with a fresh versionCode scheme
2. **Remove tracking and networking**
   - Delete the `normal` flavor, Firebase plugins/libraries, `google-services.json` CI steps, and the analytics stubs in `FirebaseApp.kt`
   - Delete the update checker (`update/`, `network/KtorClient.kt`, Ktor and versioncompare libraries)
   - Remove `INTERNET` and `ACCESS_NETWORK_STATE`; check that the "chat without saving" tool still works (it only fires an intent)
3. **Toolchain**
   - Bump AGP, Gradle, Kotlin, KSP, AndroidX, Media3, Room, Koin to current stable versions
   - targetSdk/compileSdk to the latest stable API level; keep minSdk 24 unless something forces a change
4. **Docs and credits**
   - New README (features, screenshots, "Based on WhatSave by Christians Martínez A.")
   - About screen: original author credit + link to this repo; keep `LICENSE.md`
   - Update `metadata/` (fastlane) texts for F-Droid
5. **CI**
   - One workflow: lint + unit tests + `assembleRelease` on every push/PR
   - Release workflow: signed APK from a tag, signing key stored in GitHub Secrets
6. **Verification**
   - Real device or emulator on Android 11+ and on Android 10 or older
   - WhatsApp and WhatsApp Business, images and videos, save/share/delete, catcher opt-in

## Phase 2 — Better than the original → **v1.1**

- **Expiry countdown** on each status ("gone in 3 h", from file modified time + 24 h)
- **Duplicate detection** by content hash: "already saved" badge, never save the same status twice
- **Repost**: share a status straight to WhatsApp / WhatsApp Business
- **Frame grab**: save the current video frame as an image
- **Saved library**: filter by type and date, favorites, storage used, bulk delete

## Phase 3 — Automation → **v1.2**

- **Auto-save** (opt-in): periodic WorkManager scan, filters (images / videos / both)
- Optional "new statuses" notification
- Home-screen widget and Quick Settings tile (new-status count, one tap to open)

## Phase 4 — Privacy and tools → **v1.3**

- **App lock** with biometrics / device credential (`androidx.biometric`)
- **Hide saved files from the gallery** toggle (`.nomedia` in the save folder)
- **Backup / export** saved statuses to any folder via SAF
- **Deleted-message catcher hardening**: clear opt-in onboarding, retention limit (e.g. 7/30 days), excluded from Android backups, one-tap wipe
- **Chat without saving**: number from clipboard, recent numbers list

## Phase 5 — Distribution

1. GitHub Releases (signed APK, changelog)
2. IzzyOnDroid repo (picks up GitHub releases)
3. F-Droid main repo: metadata merge request, aim for reproducible builds
4. Optional: own Weblate project for translations

## Open questions

- Final `applicationId`
- Keep OGWhatsApp support (a modded client) or drop it?
- Version scheme: 1.0.0 fresh, or continue from 2.x to signal the lineage?
