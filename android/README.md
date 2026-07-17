# ScanPro — Android app (scaffold)

Kotlin + Jetpack Compose + Hilt + Retrofit scaffold, wired to the Cloudflare API
([../cloudflare](../cloudflare)). This is a **starting skeleton**, not the full app:
it stands up the build, DI graph, networking, and one screen (**Home**) that
loads documents from `GET /v1/documents`.

## What's here
- Gradle (Kotlin DSL) + version catalog (`gradle/libs.versions.toml`)
- Single `:app` module — packages mirror the target module split in
  [../BUILD_SPEC.md](../BUILD_SPEC.md) §3 (`feature/`, `data/`, `network/`, `di/`,
  `navigation/`). Split into real Gradle modules as the app grows.
- Hilt DI (`di/AppModule.kt`), Retrofit + kotlinx.serialization
- `network/ScanProApi.kt` (API contract + DTOs), `AuthInterceptor` (Firebase bearer token)
- `feature/home` — `HomeViewModel` (StateFlow UiState) + `HomeScreen` (Compose)

## Open it
1. Open the `android/` folder in **Android Studio** (Ladybug or newer). It will
   generate the Gradle wrapper and sync.
   - CLI alternative: `gradle wrapper` then `./gradlew assembleDebug`.
2. Set the API URL in `gradle.properties` → `SCANPRO_API_BASE_URL`:
   - Emulator → local Worker: `http://10.0.2.2:8787/` (default; `10.0.2.2` = host loopback)
   - Deployed Worker: `https://scanpro-api.<subdomain>.workers.dev/`
3. Run on an emulator/device.

## CI (APK build)
`.github/workflows/android.yml` builds a **debug APK** on every push/PR to
`android/**` and uploads it as the `docscan-pro-debug-apk` artifact. It generates
a pinned Gradle wrapper on the runner (the `gradle-wrapper.jar` is intentionally
not committed). Release signing (keystore secrets) is a later step.

## Auth wiring (next step)
`network/TokenProvider` holds the Firebase ID token the `AuthInterceptor` attaches.
Wire it to real auth: add Firebase Auth, and on sign-in set
`tokenProvider.token = user.getIdToken(false).await().token`. Until then, `/v1/*`
calls return 401 (expected) and Home shows the error state.

## Notes / TODO
- `usesCleartextTraffic="true"` is enabled for local `http://10.0.2.2` dev. Set it
  `false` (or scope a network-security-config) before release.
- Feature modules to build next per [../ENGINEERING_SPEC.md](../ENGINEERING_SPEC.md):
  scan (CameraX + ML Kit), editor, folders, translate, sync (WorkManager + Drive), Room cache.
