# Product Requirements Document (PRD)

## Product Name: (Working title) **ScanPro**

**Author:** Product Team
**Date:** 2026-07-17
**Version:** 1.0 (Draft)
**Status:** For Review

---

## 1. Overview

### 1.1 Summary
ScanPro is a free, cross-platform mobile utility app that lets users scan documents using their phone camera, organize them into folders, edit/manage them, and translate scanned text — with automatic sync to their cloud drive. It is positioned as a no-cost alternative to Adobe Scan, with no page limits and no paywalled features.

### 1.2 Problem Statement
Existing document scanning apps (Adobe Scan, CamScanner, etc.) lock core functionality — high page counts, OCR, translation, watermark-free exports — behind subscriptions. Users who scan documents occasionally or in bulk want a genuinely free tool that captures, organizes, translates, and backs up their documents without limits or hidden charges.

### 1.3 Goals
- Deliver a fully free scanning experience with **no page limits**.
- Auto-sync scanned documents to the user's cloud drive.
- Provide accurate OCR and text translation.
- Let users organize documents into custom folders.
- Keep the UX simple: scan should be reachable in one tap from the home screen.

### 1.4 Non-Goals (v1)
- Team/enterprise collaboration and shared workspaces.
- Advanced PDF editing (redaction, form filling, e-signatures).
- Desktop/web client (mobile-first for v1).
- Offline machine translation (v1 uses cloud translation).

---

## 2. Target Users & Personas

| Persona | Description | Key Need |
|---|---|---|
| **Student** | Scans notes, textbook pages, assignments | Unlimited scanning, folders per subject |
| **Small business owner / freelancer** | Scans receipts, invoices, contracts | Auto-backup to drive, searchable text |
| **Traveler / immigrant** | Scans foreign documents, forms | Accurate translation |
| **General consumer** | Scans IDs, letters, warranties | Free, simple, safe storage |

---

## 3. Confirmed Decisions & Assumptions

**Confirmed:**
- **Platform:** **Android only** for v1. iOS is deferred to a future release.
- **Funding model:** **Free core + paid power features.** All scanning, folders, editing, PDF export, and drive sync are free forever. Only heavy/premium features (e.g. bulk cloud translation beyond a daily cap, premium export options) may be paid later.
- **Cloud drives:** **Google Drive, Dropbox, and OneDrive.** (Google Drive is the launch integration; Dropbox and OneDrive follow.)

**Assumptions still to confirm:**
- **Auth:** Email + phone (OTP) sign-up. *Confirm SMS provider budget.*
- **iCloud was requested but is out of scope:** Apple provides no usable iCloud Drive API for Android apps — it only works on Apple platforms. iCloud is therefore deferred until/unless an iOS version ships.
- **On-device vs. cloud** for OCR and translation (see §11.1, §11.2).

---

## 4. User Stories

1. As a new user, I can sign up using my **phone number (OTP)** or **email**, so I can access my account securely.
2. As a user, I can tap **Scan** on the home screen and capture one or **many pages** in a single session, with no page limit.
3. As a user, I can have my scanned documents **automatically uploaded to my linked drive** so they're backed up.
4. As a user, I can **create and manage folders** to organize documents by project, subject, or type.
5. As a user, I can **translate** the text in a scanned document into another language, accurately.
6. As a user, I can **edit** a scan before saving — crop, rotate, adjust filters, **rearrange pages by dragging thumbnails, add new pages, and remove pages**.
7. As a user, I can **erase watermarks, stray marks, shadows, or other unwanted parts** of a scanned page so the final document is clean.
8. As a user, I can **export/share** a document as PDF or image.
9. As a user, I can **search** my documents by name or by text content (via OCR).

---

## 5. Functional Requirements

### 5.1 Authentication & Onboarding
- **FR-1.1** Sign up / log in via email (magic link or password) OR phone number (OTP via SMS).
- **FR-1.2** Account verification (email verification link / SMS OTP).
- **FR-1.3** Password reset / account recovery.
- **FR-1.4** First-run onboarding: explain scan, folders, drive linking; request camera permission.
- **FR-1.5** Link cloud drive account via OAuth (optional but prompted).

### 5.2 Home Screen
- **FR-2.1** Prominent **Scan** button (primary CTA), reachable in one tap.
- **FR-2.2** Recent documents list.
- **FR-2.3** Quick access to Folders and Search.
- **FR-2.4** Sync status indicator (synced / pending / failed).

### 5.3 Scanning
- **FR-3.1** Capture pages via camera; **unlimited pages per document**.
- **FR-3.2** Automatic edge detection and perspective correction (auto-crop).
- **FR-3.3** Image enhancement filters (auto, B&W, grayscale, color, magic color).
- **FR-3.4** Multi-page capture: continue capturing pages into one document.
- **FR-3.6** Import existing images from gallery / files.
- **FR-3.7** Save as multi-page PDF and/or image bundle.
- **FR-3.8** OCR text extraction on scanned pages (for search and translation).
- **FR-3.9** Create a PDF directly from photos already on the device (gallery/files) — multi-select, reorder, then save, without using the camera. [extends FR-3.6]

### 5.4 Page Editing
The document editor operates on a **thumbnail strip/grid** showing every page. Users can:
- **FR-E.1 Rearrange pages:** drag-and-drop page thumbnails to reorder pages within a document.
- **FR-E.2 Remove pages:** delete a page by removing its thumbnail (with undo).
- **FR-E.3 Add pages:** insert new pages at any position — via camera capture or import from gallery/files — placed inline in the thumbnail strip.
- **FR-E.4 Re-crop & rotate:** adjust crop boundaries and rotate any individual page after capture.
- **FR-E.5 Erase / clean-up tool:** manually select and erase **unwanted areas, watermarks, stray marks, shadows, or artifacts** from a page. A brush/rubber tool fills the selected region using the surrounding background (content-aware where possible) so the removed area blends in.
- **FR-E.6 Auto-enhance clean-up:** automatic removal of shadows, finger edges, and background noise on capture.
- **FR-E.7 Undo/redo:** all edit operations (reorder, add, remove, erase, crop) are reversible within the editing session.
- **FR-E.8 Non-destructive where feasible:** keep the original capture available so edits can be reverted before final save.
- **FR-E.9 Resize / scale:** change a page's output dimensions — fit to a standard size (A4 / Letter) or custom dimensions — preserving aspect ratio by default.
- **FR-E.10 Insert image:** add an image into the document, either as a **new page** or placed onto an existing page as a **movable, resizable overlay** (e.g., signature, stamp, photo). [extends FR-E.3]

> **Scope note (FR-E.5):** The erase tool is intended for cleaning up *the user's own documents* — removing scan artifacts, shadows, stray pen marks, or unwanted background. It is not positioned or marketed as a tool for removing ownership/copyright marks from third-party material. See §11.8.

### 5.5 Documentation / Document Management
- **FR-4.1** Rename documents.
- **FR-4.2** Move documents between folders.
- **FR-4.3** Delete documents (with confirmation; soft-delete/trash recommended).
- **FR-4.4** View document details (page count, size, created date, sync state).
- **FR-4.5** Export/share as PDF or image (email, messaging, other apps).
- **FR-4.6** Full-text search across documents (powered by OCR).
- **FR-4.7** Compress / optimize output: pick a quality-size level (e.g. High / Balanced / Small, or a target size) when saving or sharing, to shrink files for storage and faster drive upload. Because the original capture is retained (FR-E.8), the user can revert to full quality at any time before the original is discarded.

> **Note on "compress / decompress" (FR-4.7):** image compression to reduce size is lossy — data removed to shrink a file cannot be mathematically restored. "Decompress" here means **revert to the retained original**, not reconstruct lost detail. If originals are ever purged (e.g. to save space), that revert is no longer possible; the UI must make this clear.

### 5.6 Folders
- **FR-5.1** Create, rename, delete folders.
- **FR-5.2** Nested folders (subfolders) — *v1 optional; confirm.*
- **FR-5.3** Move/organize documents into folders.
- **FR-5.4** Folder structure optionally mirrors the linked drive folder structure.

### 5.7 Translation
- **FR-6.1** Extract text via OCR, then translate to a user-selected target language.
- **FR-6.2** Support a broad set of source and target languages (target: 50+).
- **FR-6.3** Display original and translated text side-by-side or toggle.
- **FR-6.4** Export translated text (copy, share, or save as new document).
- **FR-6.5** Auto-detect source language.

### 5.8 Cloud Drive Sync
- **FR-7.1** Link drive account via OAuth.
- **FR-7.2** Auto-upload new scans to a designated app folder in the drive.
- **FR-7.3** Configurable: auto-sync on Wi-Fi only vs. any network.
- **FR-7.4** Retry on failure; show sync status per document.
- **FR-7.5** Handle conflicts and offline queueing (upload when connection restored).

---

## 6. Non-Functional Requirements

- **NFR-1 Performance:** Edge detection & capture responsive (<300ms preview); PDF generation for a 20-page doc < 5s on mid-range device.
- **NFR-2 Reliability:** Sync must be resilient to network drops; no data loss on app kill.
- **NFR-3 Security & Privacy:** Documents may contain sensitive data (IDs, contracts). Encrypt at rest (device) and in transit (TLS). Clear privacy policy on what leaves the device (OCR/translation).
- **NFR-4 Scalability:** Backend must handle unlimited scans per user; storage strategy must account for this (see §11).
- **NFR-5 Accessibility:** Support screen readers, dynamic type, sufficient contrast.
- **NFR-6 Offline:** Scanning, editing, and folder management work offline; sync and translation queue until online.
- **NFR-7 Localization:** App UI localized for major markets.

---

## 7. Proposed Technical Approach (High Level)

| Layer | Recommendation | Notes |
|---|---|---|
| **Client** | Native Android (Kotlin + Jetpack Compose) or Flutter | Android-only v1; Flutter keeps a future iOS port cheaper |
| **Edge detection / scanning** | ML Kit Document Scanner (Android) / OpenCV | Keeps scanning free & private, on-device |
| **OCR** | On-device OCR (ML Kit Text Recognition / Tesseract) | Free, private, no per-call cost — fits "free core" |
| **Translation** | On-device ML Kit Translate for free tier; cloud API (Google/DeepL) reserved for paid power features | On-device = free core; cloud = paid tier per §11.1 |
| **Auth** | Firebase Auth (email + phone OTP) | SMS OTP has per-message cost |
| **Drive sync** | Google Drive API (launch), then Dropbox + OneDrive APIs — store in user's own drive | Offloads storage cost to user's drive |
| **Backend** | Firebase, or lightweight custom API | Minimal if storage lives in user's drive |

**Key architectural insight:** If documents are stored in the **user's own cloud drive** (not our servers), storage costs are largely offloaded — this is what makes "unlimited & free" feasible.

---

## 8. User Flow (Happy Path)

1. Open app → Sign up (email or phone OTP) → verify.
2. Onboarding → link Google Drive (optional) → grant camera permission.
3. Home → tap **Scan** → capture N pages → auto-crop/enhance → save.
4. Name document → choose folder → save.
5. Document auto-uploads to linked drive (status shown).
6. (Optional) Open document → **Translate** → pick target language → view/export.
7. Organize into folders; search by name or text later.

---

## 9. Success Metrics (KPIs)

- **Activation:** % of new users who complete ≥1 scan in first session.
- **Drive linking rate:** % of users who link a cloud drive.
- **Retention:** D1 / D7 / D30 retention.
- **Engagement:** avg. scans per active user per week.
- **Translation usage:** % of documents translated.
- **Sync reliability:** % of scans successfully synced without manual retry.
- **Crash-free sessions:** > 99.5%.

---

## 10. Release Plan (Phased)

**MVP (v1.0) — Android**
- Email + phone auth
- One-tap scan, unlimited pages, auto-crop, filters
- Save as PDF, folders, basic document management
- Google Drive auto-sync
- On-device OCR + search
- On-device translation (free core)

**v1.1**
- Nested folders, trash/restore
- Additional drives: Dropbox, OneDrive
- Cloud translation as **paid power feature** (higher accuracy, more languages, side-by-side view)

**v2.0**
- iOS version (+ iCloud drive integration becomes viable)
- Desktop/web companion
- Advanced editing, e-signatures (evaluate)

---

## 11. Review & Risk Assessment

> This section is the critical review of the requirements above.

### 11.1 The "Everything Free" Problem — RESOLVED via free-core / paid-power model
Funding model is confirmed: **free core + paid power features.** This resolves the cost tension as follows:

- **Translation:** Free tier uses **on-device ML translation** (ML Kit Translate) — zero marginal cost, works offline. **Accurate cloud translation** (Google Cloud Translation / DeepL, billed per character) becomes a **paid power feature** in v1.1, so per-character costs are covered by the users who use it heavily. This keeps the promise of "free scanning" honest while making accurate translation sustainable.
- **SMS OTP costs money** per message. *Mitigation:* prefer email verification; use phone OTP selectively, or use cheaper regional SMS providers.
- **Storage:** Solved by storing in the **user's own drive** rather than ours — a core design principle. Avoid hosting documents on our own servers for the free tier.
- **Note on messaging:** Be transparent in-app and in store listing that *scanning, folders, editing, PDF export, and drive sync are free forever*, and only advanced/bulk translation is paid. Avoid a bait-and-switch perception.

### 11.2 "Accurate Translation" Needs Definition
"Accurate" is subjective. Recommend: define a target (e.g., use DeepL/Google for top N languages, measure with sample documents), and set expectations that scanned-image translation accuracy depends heavily on OCR quality first. **Garbage OCR → garbage translation.** Invest in OCR quality.

### 11.3 Privacy & Compliance — Underspecified
Scanned documents often contain **highly sensitive PII** (IDs, financial, legal). Requirements must add:
- Explicit privacy policy: what text leaves the device (OCR/translation calls).
- Data handling for translation API calls (does the provider retain data?).
- GDPR / regional compliance if launching in EU/other regions.
- Encryption at rest on device.

### 11.4 Drive Dependency Risk
Making Google Drive the storage backbone means: OAuth complexity, dependency on Drive quotas (user's free 15GB fills up), and API rate limits. *Mitigation:* clear messaging when the user's drive is full; graceful local-only fallback.

### 11.5 Scope Realism
The MVP as written is **large**. Scanning + OCR + folders + drive sync + translation is a lot for a first release. Recommend cutting v1 to: **auth, scan, folders, PDF export, drive sync**. Add **translation and OCR search in v1.1** once the core is stable. (Kept in the plan above, but flagging that even the "MVP" is ambitious.)

### 11.6 Missing from Original Request (added above)
- Document editing (crop/rotate/reorder) — implied but must be explicit.
- Export/share formats — needed to make scans useful.
- Search — natural companion to OCR.
- Offline behavior — critical for a scanner.
- Trash/soft-delete — prevents accidental loss.

### 11.8 Erase / Watermark-Removal Tool — Positioning & Effort
The erase/clean-up tool (FR-E.5) carries two considerations:
- **Effort:** A convincing content-aware erase (inpainting) that removes a watermark and fills the gap naturally is technically non-trivial. A simpler v1 can offer a *fill-with-background-color* brush; true inpainting can be a later enhancement. Scope accordingly — don't over-promise "remove any watermark" in marketing.
- **Positioning:** Frame the feature as cleaning up **the user's own scans** (shadows, stray marks, scan artifacts, unwanted background). Avoid marketing it as a tool to strip copyright/ownership marks from third-party documents, which invites misuse and reputational/legal concern. Keep store copy focused on document clean-up quality, not watermark removal per se.

### 11.7 Open Decisions

**Resolved:**
1. ~~Platform~~ → **Android only** for v1.
2. ~~Funding model~~ → **Free core + paid power features.**
3. ~~Cloud drives~~ → **Google Drive (launch), Dropbox, OneDrive.** iCloud deferred (no Android API).

**Still open:**
4. Nested folders in v1 or v1.1?
5. On-device translation quality — is ML Kit Translate accurate enough for the free tier's target languages, or does the free tier need a capped cloud allowance? (Run sample-document benchmarks.)
6. SMS provider and budget for phone OTP.

---

## 12. Appendix

- **Competitors:** Adobe Scan, CamScanner, Microsoft Lens, Google Drive Scan, Genius Scan.
- **Differentiator:** Genuinely free, unlimited pages, integrated translation, user-owned storage.

### Parked ideas (revisit later)
- **AI images for PDFs** — generating images (text-to-image) or fetching images to insert into documents. Deferred: image generation carries a per-use API cost that conflicts with the free-core model (would likely be a Pro feature), and web image download raises copyright concerns. Manual image insertion (FR-E.10) covers the core need for now.
