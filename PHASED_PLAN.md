# DocScan Pro — Phased Development Plan

**Package:** `com.docscan.pro` · **Repo:** GitHub · **CI:** GitHub Actions (debug APK)
**Companion:** [BUILD_SPEC.md](BUILD_SPEC.md) (milestones) · [ENGINEERING_SPEC.md](ENGINEERING_SPEC.md) (design)

Each phase is a shippable slice with a clear exit criterion. Build the APK on CI at the end of every phase.

---

## Phase 0 — Foundation & CI  ✅ (done, pending push)
- [x] Repo structure, `.gitignore`, `.gitattributes`, initial commit
- [x] Package `com.docscan.pro`; Gradle + version catalog; Hilt DI; Compose theme; nav host
- [x] Retrofit client + `Home` screen wired to `GET /v1/documents`
- [x] Cloudflare Workers + D1 API (metadata + delta sync), typechecked
- [x] GitHub Actions workflow builds a debug APK artifact
- [ ] **Push to GitHub** (needs your auth — see "What's needed" below)
- **Exit:** green CI run producing `docscan-pro-debug-apk`.

## Phase 1 — Capture core
- CameraX capture; ML Kit Document Scanner (edge detect, perspective, filters)
- Multi-page session (no limit); auto-enhance; save as multi-page PDF
- Room cache; local document list on Home (replaces network-only fetch)
- **Exit:** scan → save → view, fully offline. **Blocks on:** nothing external.

## Phase 2 — Editor
- Thumbnail strip: reorder / add / remove; crop, rotate, filter, **resize**, **erase** (mask+fill), **insert image**
- Undo/redo via `PAGE_EDIT` op-log; non-destructive (keep originals)
- **Photos → PDF** (create from device gallery)
- **Exit:** all FR-E.* + FR-3.9 pass on device.

## Phase 3 — Persistence & search
- Room schema complete; FTS5 index; OCR pipeline (ML Kit Text Recognition)
- Folders CRUD + move; **compression** levels on save (FR-4.7)
- **Exit:** full-text search finds OCR text; folders work; file-size options work.

## Phase 4 — Auth & cloud sync
- Firebase Auth (phone OTP + email); feed ID token into `TokenProvider`/`AuthInterceptor`
- Google Sign-In + Drive `drive.file`; `SyncWorker` (WorkManager) uploads to Drive
- Delta sync with the Cloudflare API (`/v1/sync`); sync-status badges
- **Exit:** round-trip sync survives offline + process death; multi-device metadata sync.
- **Blocks on:** Firebase project + `google-services.json`; deployed Cloudflare Worker.

## Phase 5 — Translate + polish
- On-device translation (ML Kit); side-by-side; export/share
- Accessibility (TalkBack, dynamic type), localization, Crashlytics + Analytics
- **Exit:** NFR targets met (see BUILD_SPEC §9).

## Phase 6 — Release
- Release signing (keystore in CI secrets), R8; Play data-safety + privacy policy
- Internal → closed → production rollout
- **Blocks on:** upload keystore; Play Console account.

---

## What's needed from you (to sync repo + build APK)

### 1. Create the GitHub repo and push
`gh` isn't installed here, so do this from your machine (repo is already committed locally at `D:\Scanning app`):

**Option A — GitHub CLI (easiest):**
```bash
winget install GitHub.cli          # if not installed; then restart shell
gh auth login                      # authenticate once
cd "D:\Scanning app"
gh repo create docscan-pro --private --source=. --remote=origin --push
```

**Option B — manual:** create an empty repo `docscan-pro` on github.com (no README), then:
```bash
cd "D:\Scanning app"
git remote add origin https://github.com/<your-username>/docscan-pro.git
git push -u origin main
```

### 2. APK builds automatically
On push to `main`, GitHub Actions runs **Android CI** → open the run → download the **`docscan-pro-debug-apk`** artifact. (Also runnable manually via "Run workflow".)

### 3. Local build (optional)
Install Android Studio, open the `android/` folder; it creates `local.properties` + the Gradle wrapper. Set `SCANPRO_API_BASE_URL` in `gradle.properties`.

### 4. Backend deploy (for Phase 4)
Cloudflare account → `cd cloudflare && npx wrangler login` → `npm run db:create` (paste id into `wrangler.toml`) → set `FIREBASE_PROJECT_ID` → `npm run db:migrate` → `npm run deploy`.

### 5. Firebase (for Phase 4 auth)
Create a Firebase project → add an Android app with package **`com.docscan.pro`** → download `google-services.json` into `android/app/` → add your signing SHA-1. (Then we wire the token into `AuthInterceptor`.)

### 6. Release signing (for Phase 6)
Generate an upload keystore; store it + passwords as GitHub Actions secrets; add a release build step. (Later.)
