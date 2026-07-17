# ScanPro — Entity-Relationship Diagram (ERD)

**Scope:** Data model for ScanPro Android v1.
**Companion docs:** [PRD.md](PRD.md) · [BUILD_SPEC.md](BUILD_SPEC.md)
**Status:** Draft v1.0 · **Date:** 2026-07-17

> The local **Room (SQLite)** database is the source of truth for metadata. Document **files** (page images, generated PDFs) are **not** stored in the DB — they live in app-private storage and are referenced by URI, then backed up to the **user's own cloud drive**. Fields like `driveFileId` link a local row to its remote copy. This is what keeps "unlimited + free" viable (PRD §11.1).

---

## 1. Diagram

```mermaid
erDiagram
  USER ||--o{ FOLDER : owns
  USER ||--o{ DOCUMENT : owns
  USER ||--o{ DRIVE_CONNECTION : links
  USER ||--|| ENTITLEMENT : has
  FOLDER ||--o{ DOCUMENT : contains
  FOLDER |o--o{ FOLDER : "parent of (v1.1)"
  DOCUMENT ||--o{ PAGE : "has (ordered)"
  DOCUMENT ||--o{ TRANSLATION : produces
  DOCUMENT ||--o{ SYNC_JOB : "queued as"
  DRIVE_CONNECTION ||--o{ SYNC_JOB : "target of"
  PAGE ||--o{ PAGE_EDIT : "modified by"

  USER {
    string id PK
    string email UK
    string phone UK
    string displayName
    long createdAt
    long lastActiveAt
  }
  ENTITLEMENT {
    string userId PK_FK
    string tier
    string status
    string source
    long expiresAt
    long updatedAt
  }
  DRIVE_CONNECTION {
    string id PK
    string userId FK
    string provider
    string accountEmail
    string appFolderId
    string accessScope
    long linkedAt
    long lastSyncAt
  }
  FOLDER {
    string id PK
    string userId FK
    string parentId FK
    string name
    string driveFolderId
    int sortOrder
    long createdAt
    long updatedAt
    long deletedAt
  }
  DOCUMENT {
    string id PK
    string userId FK
    string folderId FK
    string name
    int pageCount
    long sizeBytes
    string format
    string driveFileId
    string syncState
    string thumbnailUri
    long createdAt
    long updatedAt
    long deletedAt
  }
  PAGE {
    string id PK
    string documentId FK
    int orderIndex
    string originalUri
    string processedUri
    string filter
    int rotation
    string ocrText
    string ocrLang
    long updatedAt
  }
  PAGE_EDIT {
    string id PK
    string pageId FK
    string type
    string paramsJson
    int sequence
    long createdAt
  }
  TRANSLATION {
    string id PK
    string documentId FK
    string sourceLang
    string targetLang
    string engine
    string status
    string outputUri
    long createdAt
  }
  SYNC_JOB {
    string id PK
    string documentId FK
    string driveConnectionId FK
    string operation
    string state
    int attempts
    string lastError
    long nextAttemptAt
    long updatedAt
  }
```

**Cardinality legend:** `||` one-and-only-one · `|o` zero-or-one · `o{` zero-or-many · `|{` one-or-many.

---

## 2. Entities

### 2.1 USER
The account holder. Created on first successful auth (phone or email).

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | App-generated user id (mirrors Firebase UID). |
| `email` | TEXT | UNIQUE, nullable | Present if signed up / linked via email. |
| `phone` | TEXT | UNIQUE, nullable | E.164 format; present if phone auth. |
| `displayName` | TEXT | nullable | Optional. |
| `createdAt` | INTEGER | NOT NULL | Epoch millis. |
| `lastActiveAt` | INTEGER | NOT NULL | For engagement metrics (PRD §9). |

> At least one of `email` / `phone` is non-null (enforced in app logic). [FR-1.1]

### 2.2 ENTITLEMENT
Tracks the user's tier for the free-core / paid-power model. One row per user (1:1).

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `userId` | TEXT | **PK, FK → USER.id** | One entitlement per user. |
| `tier` | TEXT (enum) | NOT NULL | `FREE`, `PRO`. |
| `status` | TEXT (enum) | NOT NULL | `ACTIVE`, `EXPIRED`, `GRACE`, `NONE`. |
| `source` | TEXT (enum) | nullable | `PLAY_BILLING`, `PROMO`, `NONE`. |
| `expiresAt` | INTEGER | nullable | For time-bound Pro (v1.1). |
| `updatedAt` | INTEGER | NOT NULL | |

> v1 ships everyone as `FREE`. Gate cloud translation (v1.1) on `tier == PRO && status == ACTIVE`. [PRD §11.1]

### 2.3 DRIVE_CONNECTION
A linked cloud-drive account. Multiple allowed per user (Google now; Dropbox/OneDrive later).

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `userId` | TEXT | **FK → USER.id** | |
| `provider` | TEXT (enum) | NOT NULL | `GOOGLE_DRIVE`, `DROPBOX`, `ONEDRIVE`. |
| `accountEmail` | TEXT | nullable | The linked drive account. |
| `appFolderId` | TEXT | nullable | Remote id of the ScanPro app folder. |
| `accessScope` | TEXT | NOT NULL | e.g. `drive.file` (least privilege). |
| `linkedAt` | INTEGER | NOT NULL | |
| `lastSyncAt` | INTEGER | nullable | |

> OAuth tokens are **not** stored here — they live in `EncryptedSharedPreferences`, keyed by connection id. [BUILD_SPEC §8] · [FR-7.1]

### 2.4 FOLDER
User-created organizational container. Self-referential parent for nested folders (v1.1).

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `userId` | TEXT | **FK → USER.id** | |
| `parentId` | TEXT | **FK → FOLDER.id**, nullable | `NULL` = root. Nested folders are v1.1. |
| `name` | TEXT | NOT NULL | |
| `driveFolderId` | TEXT | nullable | Mirror folder in the drive. [FR-5.4] |
| `sortOrder` | INTEGER | NOT NULL, default 0 | Manual ordering. |
| `createdAt` / `updatedAt` | INTEGER | NOT NULL | |
| `deletedAt` | INTEGER | nullable | Soft delete. |

> Deleting a folder with documents: reassign children to root or cascade to trash — **decision open** (see §6). [FR-5.1]

### 2.5 DOCUMENT
A scanned document (1+ pages). The primary user artifact.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `userId` | TEXT | **FK → USER.id** | |
| `folderId` | TEXT | **FK → FOLDER.id**, nullable | `NULL` = unfiled/root. [FR-4.2] |
| `name` | TEXT | NOT NULL | User-editable. [FR-4.1] |
| `pageCount` | INTEGER | NOT NULL | Denormalized count of PAGE rows. |
| `sizeBytes` | INTEGER | NOT NULL | Generated file size. |
| `format` | TEXT (enum) | NOT NULL | `PDF`, `IMAGE_BUNDLE`. [FR-3.7] |
| `driveFileId` | TEXT | nullable | Remote copy id once synced. |
| `syncState` | TEXT (enum) | NOT NULL | `LOCAL_ONLY`, `PENDING`, `UPLOADING`, `SYNCED`, `FAILED`. Drives the UI badge. [FR-2.4/7.4] |
| `thumbnailUri` | TEXT | nullable | Cached first-page thumbnail. |
| `createdAt` / `updatedAt` | INTEGER | NOT NULL | |
| `deletedAt` | INTEGER | nullable | Soft delete → trash/restore. [PRD §11.6] |

### 2.6 PAGE
A single page within a document, in order. Holds image URIs and OCR output.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `documentId` | TEXT | **FK → DOCUMENT.id**, ON DELETE CASCADE | |
| `orderIndex` | INTEGER | NOT NULL | Position in document. [FR-E.1] |
| `originalUri` | TEXT | NOT NULL | Raw capture — retained for non-destructive edits. [FR-E.8] |
| `processedUri` | TEXT | NOT NULL | Cropped/enhanced/erased result shown & exported. |
| `filter` | TEXT (enum) | NOT NULL | `AUTO`, `COLOR`, `GRAYSCALE`, `BW`, `MAGIC`. [FR-3.3] |
| `rotation` | INTEGER | NOT NULL, default 0 | Degrees {0,90,180,270}. [FR-E.4] |
| `ocrText` | TEXT | nullable | Extracted text (feeds search + translation). [FR-3.8] |
| `ocrLang` | TEXT | nullable | Detected language code. |
| `updatedAt` | INTEGER | NOT NULL | |

> `(documentId, orderIndex)` is unique per document. Reordering rewrites `orderIndex`.

### 2.7 PAGE_EDIT
An ordered log of edit operations on a page — enables undo/redo and non-destructive history. [FR-E.7]

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `pageId` | TEXT | **FK → PAGE.id**, ON DELETE CASCADE | |
| `type` | TEXT (enum) | NOT NULL | `CROP`, `ROTATE`, `FILTER`, `ERASE`, `RESIZE`, `INSERT_IMAGE`. |
| `paramsJson` | TEXT | NOT NULL | Op parameters (crop rect, angle, erase mask/region). |
| `sequence` | INTEGER | NOT NULL | Order applied; undo pops highest. |
| `createdAt` | INTEGER | NOT NULL | |

### 2.8 TRANSLATION
A translation result for a document. Cached so repeat views are instant.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `documentId` | TEXT | **FK → DOCUMENT.id**, ON DELETE CASCADE | |
| `sourceLang` | TEXT | NOT NULL | `auto` or detected/selected code. [FR-6.5] |
| `targetLang` | TEXT | NOT NULL | Selected target. [FR-6.1] |
| `engine` | TEXT (enum) | NOT NULL | `ON_DEVICE` (free) or `CLOUD` (Pro, v1.1). |
| `status` | TEXT (enum) | NOT NULL | `PENDING`, `READY`, `FAILED`. |
| `outputUri` | TEXT | nullable | Stored translated text/doc. [FR-6.4] |
| `createdAt` | INTEGER | NOT NULL | |

> Unique on `(documentId, targetLang, engine)` to reuse cached results.

### 2.9 SYNC_JOB
A queued sync operation against a drive connection. Powers the offline-capable, retryable sync engine.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| `id` | TEXT (UUID) | **PK** | |
| `documentId` | TEXT | **FK → DOCUMENT.id**, ON DELETE CASCADE | |
| `driveConnectionId` | TEXT | **FK → DRIVE_CONNECTION.id** | |
| `operation` | TEXT (enum) | NOT NULL | `UPLOAD`, `UPDATE`, `DELETE`. |
| `state` | TEXT (enum) | NOT NULL | `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`. |
| `attempts` | INTEGER | NOT NULL, default 0 | Retry counter. |
| `lastError` | TEXT | nullable | Diagnostic. |
| `nextAttemptAt` | INTEGER | nullable | Backoff schedule. [FR-7.4] |
| `updatedAt` | INTEGER | NOT NULL | |

---

## 3. Relationships summary

| From | To | Cardinality | Delete rule |
|---|---|---|---|
| USER | FOLDER | 1 : 0..N | Restrict (account deletion = full purge) |
| USER | DOCUMENT | 1 : 0..N | Restrict |
| USER | DRIVE_CONNECTION | 1 : 0..N | Cascade |
| USER | ENTITLEMENT | 1 : 1 | Cascade |
| FOLDER | FOLDER (parent) | 0..1 : 0..N | See §6 (open) |
| FOLDER | DOCUMENT | 1 : 0..N | Set null (→ root) or trash — see §6 |
| DOCUMENT | PAGE | 1 : 1..N | **Cascade** |
| DOCUMENT | TRANSLATION | 1 : 0..N | **Cascade** |
| DOCUMENT | SYNC_JOB | 1 : 0..N | **Cascade** |
| DRIVE_CONNECTION | SYNC_JOB | 1 : 0..N | Restrict |
| PAGE | PAGE_EDIT | 1 : 0..N | **Cascade** |

---

## 4. Enumerations

| Enum | Values |
|---|---|
| `Document.format` | `PDF`, `IMAGE_BUNDLE` |
| `Document.syncState` | `LOCAL_ONLY`, `PENDING`, `UPLOADING`, `SYNCED`, `FAILED` |
| `Page.filter` | `AUTO`, `COLOR`, `GRAYSCALE`, `BW`, `MAGIC` |
| `PageEdit.type` | `CROP`, `ROTATE`, `FILTER`, `ERASE`, `RESIZE`, `INSERT_IMAGE` |
| `Translation.engine` | `ON_DEVICE`, `CLOUD` |
| `Translation.status` | `PENDING`, `READY`, `FAILED` |
| `DriveConnection.provider` | `GOOGLE_DRIVE`, `DROPBOX`, `ONEDRIVE` |
| `SyncJob.operation` | `UPLOAD`, `UPDATE`, `DELETE` |
| `SyncJob.state` | `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` |
| `Entitlement.tier` | `FREE`, `PRO` |
| `Entitlement.status` | `ACTIVE`, `EXPIRED`, `GRACE`, `NONE` |

---

## 5. Indexes & search

**Recommended indexes**
- `FOLDER(userId)`, `FOLDER(parentId)`
- `DOCUMENT(userId)`, `DOCUMENT(folderId)`, `DOCUMENT(syncState)`, `DOCUMENT(deletedAt)`
- `PAGE(documentId, orderIndex)` (unique), `PAGE(documentId)`
- `SYNC_JOB(state, nextAttemptAt)` (worker pickup), `SYNC_JOB(documentId)`
- `TRANSLATION(documentId, targetLang, engine)` (unique)

**Full-text search (FR-4.6)**
A separate **FTS4/5** virtual table `document_search` indexes `DOCUMENT.name` + concatenated `PAGE.ocrText`, kept in sync via triggers or repository writes. Query returns document ids ranked by relevance.

```
document_search(docId UNINDEXED, name, body)   -- FTS5
```

---

## 6. Open data decisions

1. **Folder deletion behavior** — reassign child documents to root, or cascade to trash? (Ties to nested-folder decision.) [PRD §11.7]
2. **Nested folders in v1?** — `FOLDER.parentId` is modeled but v1 may enforce single-level (all `parentId = NULL`).
3. **Multi-device** — v1 is single-device local DB; if multi-device sync is added, the drive copy (or a backend) becomes the reconciliation point and rows need `remoteUpdatedAt` for conflict resolution.
4. **OCR text of sensitive docs** — if Room is encrypted (SQLCipher), FTS performance must be validated. [BUILD_SPEC §8]
5. **Account deletion** — define full purge (local files + DB rows + optionally remote app folder) for Play data-safety compliance.

---

## 7. Mapping: local ↔ remote

| Concept | Local (Room) | Remote (user's Drive) |
|---|---|---|
| Folder | `FOLDER` row | Drive folder (`driveFolderId`) |
| Document file | file at `processedUri`s → generated PDF | Drive file (`driveFileId`) in app folder |
| Metadata | authoritative in Room | not stored remotely (v1) |
| Page images | app-private storage | not individually uploaded (bundled into the document file) |

> v1 uploads the **generated document** (PDF/image bundle), not individual page rows. The local DB remains the metadata authority; the drive is a backup/export target. [BUILD_SPEC §4, §5.1]
