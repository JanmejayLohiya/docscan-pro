-- ScanPro D1 schema (metadata + cross-device sync).
-- Files (page images / PDFs) are NOT stored here — they live in the user's own
-- cloud drive. This backend stores lightweight metadata so accounts and document
-- organization sync across a user's devices. Mirrors ERD.md (server subset).

CREATE TABLE IF NOT EXISTS users (
  id             TEXT PRIMARY KEY,          -- Firebase UID
  email          TEXT,
  phone          TEXT,
  display_name   TEXT,
  created_at     INTEGER NOT NULL,
  last_active_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS entitlements (
  user_id    TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  tier       TEXT NOT NULL DEFAULT 'FREE',   -- FREE | PRO
  status     TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | EXPIRED | GRACE | NONE
  source     TEXT,                           -- PLAY_BILLING | PROMO | NONE
  expires_at INTEGER,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS drive_connections (
  id            TEXT PRIMARY KEY,
  user_id       TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider      TEXT NOT NULL,               -- GOOGLE_DRIVE | DROPBOX | ONEDRIVE
  account_email TEXT,
  app_folder_id TEXT,
  access_scope  TEXT NOT NULL DEFAULT 'drive.file',
  linked_at     INTEGER NOT NULL,
  last_sync_at  INTEGER
);
CREATE INDEX IF NOT EXISTS idx_drive_conn_user ON drive_connections(user_id);

CREATE TABLE IF NOT EXISTS folders (
  id              TEXT PRIMARY KEY,
  user_id         TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  parent_id       TEXT,                      -- self-ref (nested folders, v1.1)
  name            TEXT NOT NULL,
  drive_folder_id TEXT,
  sort_order      INTEGER NOT NULL DEFAULT 0,
  created_at      INTEGER NOT NULL,
  updated_at      INTEGER NOT NULL,          -- used for delta sync
  deleted_at      INTEGER                    -- soft delete
);
CREATE INDEX IF NOT EXISTS idx_folders_user ON folders(user_id);
CREATE INDEX IF NOT EXISTS idx_folders_updated ON folders(user_id, updated_at);

CREATE TABLE IF NOT EXISTS documents (
  id            TEXT PRIMARY KEY,
  user_id       TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  folder_id     TEXT,                        -- NULL = root
  name          TEXT NOT NULL,
  page_count    INTEGER NOT NULL DEFAULT 0,
  size_bytes    INTEGER NOT NULL DEFAULT 0,
  format        TEXT NOT NULL DEFAULT 'PDF', -- PDF | IMAGE_BUNDLE
  drive_file_id TEXT,                        -- remote copy in user's drive
  sync_state    TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,            -- used for delta sync
  deleted_at    INTEGER
);
CREATE INDEX IF NOT EXISTS idx_documents_user ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_folder ON documents(folder_id);
CREATE INDEX IF NOT EXISTS idx_documents_updated ON documents(user_id, updated_at);

-- Page metadata (order + optional OCR text for cross-device search).
-- Image bytes stay on device / in the user's drive.
CREATE TABLE IF NOT EXISTS pages (
  id           TEXT PRIMARY KEY,
  document_id  TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  order_index  INTEGER NOT NULL,
  filter       TEXT,
  rotation     INTEGER NOT NULL DEFAULT 0,
  ocr_text     TEXT,
  ocr_lang     TEXT,
  updated_at   INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pages_doc ON pages(document_id, order_index);

CREATE TABLE IF NOT EXISTS translations (
  id          TEXT PRIMARY KEY,
  document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  source_lang TEXT NOT NULL,
  target_lang TEXT NOT NULL,
  engine      TEXT NOT NULL,                 -- ON_DEVICE | CLOUD
  status      TEXT NOT NULL,                 -- PENDING | READY | FAILED
  output_uri  TEXT,
  created_at  INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_translations_doc ON translations(document_id);
