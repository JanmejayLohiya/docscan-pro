# ScanPro API (Cloudflare Workers + D1)

Edge API that stores ScanPro **account + document metadata** and powers
**cross-device sync**. It does **not** store document files — those stay in the
user's own cloud drive (see [../BUILD_SPEC.md](../BUILD_SPEC.md) §1). Auth is
delegated to **Firebase**: the Worker validates the Firebase ID token the app
already holds, so there's no separate password system here.

## Stack
- **Hono** router on **Cloudflare Workers**
- **D1** (edge SQLite) for metadata — schema in `migrations/0001_init.sql` (mirrors [../ERD.md](../ERD.md))
- **jose** to verify Firebase ID tokens (RS256, Google JWKS)

## Setup

```bash
cd cloudflare
npm install

# 1) Create the D1 database, then paste the returned database_id into wrangler.toml
npm run db:create

# 2) Set your Firebase project id in wrangler.toml ([vars] FIREBASE_PROJECT_ID)

# 3) Apply migrations
npm run db:migrate:local     # local dev DB
npm run db:migrate           # remote (production) DB

# 4) Run locally / deploy
npm run dev                  # http://localhost:8787
npm run deploy               # deploys to *.workers.dev (or your route)
```

## Auth
Send the Firebase ID token as a bearer header on every `/v1/*` request:

```
Authorization: Bearer <firebase_id_token>
```

The Worker verifies issuer `https://securetoken.google.com/<FIREBASE_PROJECT_ID>`
and audience `<FIREBASE_PROJECT_ID>`, then upserts the user row.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Health check (no auth) |
| GET | `/v1/folders` | List folders |
| POST | `/v1/folders` | Create/upsert folder |
| PATCH | `/v1/folders/:id` | Rename / move folder |
| DELETE | `/v1/folders/:id` | Soft-delete folder |
| GET | `/v1/documents` | List documents (`?folderId=` optional) |
| POST | `/v1/documents` | Create/upsert document metadata |
| GET | `/v1/documents/:id` | Document + its pages |
| DELETE | `/v1/documents/:id` | Soft-delete document |
| GET | `/v1/entitlement` | Current tier (FREE/PRO) |
| GET | `/v1/sync?since=<ms>` | Changes since timestamp (pull) |
| POST | `/v1/sync` | Push changes (last-writer-wins by `updatedAt`) |

## Notes
- **Sync model:** last-writer-wins by `updatedAt`. Deletes are soft (`deleted_at`) so they propagate to other devices.
- **Least privilege:** the backend never sees Drive OAuth tokens or file bytes; only metadata.
- **Next steps:** rate limiting (Workers), request validation (zod), and a `/v1/drive/connections` resource if drive-link metadata should sync across devices.
