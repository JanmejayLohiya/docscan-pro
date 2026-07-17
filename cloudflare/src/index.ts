import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { verifyFirebaseToken } from './auth'

type Bindings = { DB: D1Database; FIREBASE_PROJECT_ID: string }
type Vars = { uid: string }

const app = new Hono<{ Bindings: Bindings; Variables: Vars }>()

app.use('*', cors())

// Health check (unauthenticated).
app.get('/', (c) => c.json({ service: 'scanpro-api', ok: true }))

// ---- Auth: every /v1/* route requires a valid Firebase ID token ----
app.use('/v1/*', async (c, next) => {
  const authz = c.req.header('Authorization') ?? ''
  const token = authz.startsWith('Bearer ') ? authz.slice(7) : ''
  if (!token) return c.json({ error: 'missing_token' }, 401)
  let uid: string
  try {
    uid = await verifyFirebaseToken(token, c.env.FIREBASE_PROJECT_ID)
  } catch {
    return c.json({ error: 'invalid_token' }, 401)
  }
  c.set('uid', uid)
  // Upsert the user row + ensure a default entitlement exists.
  const now = Date.now()
  await c.env.DB.batch([
    c.env.DB.prepare(
      `INSERT INTO users (id, created_at, last_active_at) VALUES (?1, ?2, ?2)
       ON CONFLICT(id) DO UPDATE SET last_active_at = ?2`,
    ).bind(uid, now),
    c.env.DB.prepare(
      `INSERT INTO entitlements (user_id, tier, status, source, updated_at)
       VALUES (?1, 'FREE', 'ACTIVE', 'NONE', ?2)
       ON CONFLICT(user_id) DO NOTHING`,
    ).bind(uid, now),
  ])
  await next()
})

// ---------------------------- Folders (FR-5.*) ----------------------------
app.get('/v1/folders', async (c) => {
  const uid = c.get('uid')
  const { results } = await c.env.DB.prepare(
    `SELECT * FROM folders WHERE user_id = ?1 AND deleted_at IS NULL ORDER BY sort_order, name`,
  ).bind(uid).all()
  return c.json({ folders: results })
})

app.post('/v1/folders', async (c) => {
  const uid = c.get('uid')
  const b = await c.req.json<Record<string, unknown>>()
  if (typeof b.name !== 'string' || !b.name.trim()) return c.json({ error: 'name_required' }, 400)
  const id = typeof b.id === 'string' ? b.id : crypto.randomUUID()
  const now = Date.now()
  await c.env.DB.prepare(
    `INSERT INTO folders (id, user_id, parent_id, name, drive_folder_id, sort_order, created_at, updated_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?7)
     ON CONFLICT(id) DO UPDATE SET name = ?4, parent_id = ?3, drive_folder_id = ?5, updated_at = ?7`,
  ).bind(id, uid, b.parentId ?? null, b.name, b.driveFolderId ?? null, b.sortOrder ?? 0, now).run()
  return c.json({ id })
})

app.patch('/v1/folders/:id', async (c) => {
  const uid = c.get('uid')
  const id = c.req.param('id')
  const b = await c.req.json<Record<string, unknown>>()
  const now = Date.now()
  const res = await c.env.DB.prepare(
    `UPDATE folders SET name = COALESCE(?3, name), parent_id = ?4, updated_at = ?5
     WHERE id = ?1 AND user_id = ?2 AND deleted_at IS NULL`,
  ).bind(id, uid, b.name ?? null, b.parentId ?? null, now).run()
  if (res.meta.changes === 0) return c.json({ error: 'not_found' }, 404)
  return c.json({ ok: true })
})

app.delete('/v1/folders/:id', async (c) => {
  const uid = c.get('uid')
  const id = c.req.param('id')
  await c.env.DB.prepare(
    `UPDATE folders SET deleted_at = ?3, updated_at = ?3 WHERE id = ?1 AND user_id = ?2`,
  ).bind(id, uid, Date.now()).run()
  return c.json({ ok: true })
})

// --------------------------- Documents (FR-4.*) ---------------------------
app.get('/v1/documents', async (c) => {
  const uid = c.get('uid')
  const folderId = c.req.query('folderId')
  const stmt = folderId
    ? c.env.DB.prepare(
        `SELECT * FROM documents WHERE user_id = ?1 AND folder_id = ?2 AND deleted_at IS NULL ORDER BY updated_at DESC`,
      ).bind(uid, folderId)
    : c.env.DB.prepare(
        `SELECT * FROM documents WHERE user_id = ?1 AND deleted_at IS NULL ORDER BY updated_at DESC`,
      ).bind(uid)
  const { results } = await stmt.all()
  return c.json({ documents: results })
})

app.post('/v1/documents', async (c) => {
  const uid = c.get('uid')
  const b = await c.req.json<Record<string, unknown>>()
  if (typeof b.name !== 'string' || !b.name.trim()) return c.json({ error: 'name_required' }, 400)
  const id = typeof b.id === 'string' ? b.id : crypto.randomUUID()
  const now = Date.now()
  await c.env.DB.prepare(
    `INSERT INTO documents (id, user_id, folder_id, name, page_count, size_bytes, format, drive_file_id, sync_state, created_at, updated_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?10)
     ON CONFLICT(id) DO UPDATE SET
       folder_id = ?3, name = ?4, page_count = ?5, size_bytes = ?6, format = ?7,
       drive_file_id = ?8, sync_state = ?9, updated_at = ?10`,
  ).bind(
    id, uid, b.folderId ?? null, b.name, b.pageCount ?? 0, b.sizeBytes ?? 0,
    b.format ?? 'PDF', b.driveFileId ?? null, b.syncState ?? 'LOCAL_ONLY', now,
  ).run()
  return c.json({ id })
})

app.get('/v1/documents/:id', async (c) => {
  const uid = c.get('uid')
  const id = c.req.param('id')
  const doc = await c.env.DB.prepare(
    `SELECT * FROM documents WHERE id = ?1 AND user_id = ?2 AND deleted_at IS NULL`,
  ).bind(id, uid).first()
  if (!doc) return c.json({ error: 'not_found' }, 404)
  const { results: pages } = await c.env.DB.prepare(
    `SELECT * FROM pages WHERE document_id = ?1 ORDER BY order_index`,
  ).bind(id).all()
  return c.json({ document: doc, pages })
})

app.delete('/v1/documents/:id', async (c) => {
  const uid = c.get('uid')
  const id = c.req.param('id')
  await c.env.DB.prepare(
    `UPDATE documents SET deleted_at = ?3, updated_at = ?3 WHERE id = ?1 AND user_id = ?2`,
  ).bind(id, uid, Date.now()).run()
  return c.json({ ok: true })
})

// --------------------------- Entitlement ---------------------------
app.get('/v1/entitlement', async (c) => {
  const uid = c.get('uid')
  const row = await c.env.DB.prepare(`SELECT * FROM entitlements WHERE user_id = ?1`).bind(uid).first()
  return c.json({ entitlement: row ?? { user_id: uid, tier: 'FREE', status: 'ACTIVE' } })
})

// --------------------------- Delta sync (FR-7.*) ---------------------------
// GET  /v1/sync?since=<ms>  → all folders+documents changed since that time.
// POST /v1/sync             → client pushes changes; last-writer-wins by updatedAt.
app.get('/v1/sync', async (c) => {
  const uid = c.get('uid')
  const since = Number(c.req.query('since') ?? 0) || 0
  const folders = await c.env.DB.prepare(
    `SELECT * FROM folders WHERE user_id = ?1 AND updated_at > ?2`,
  ).bind(uid, since).all()
  const documents = await c.env.DB.prepare(
    `SELECT * FROM documents WHERE user_id = ?1 AND updated_at > ?2`,
  ).bind(uid, since).all()
  return c.json({ folders: folders.results, documents: documents.results, now: Date.now() })
})

app.post('/v1/sync', async (c) => {
  const uid = c.get('uid')
  const b = await c.req.json<{ folders?: any[]; documents?: any[] }>()
  const stmts: D1PreparedStatement[] = []
  for (const f of b.folders ?? []) {
    stmts.push(
      c.env.DB.prepare(
        `INSERT INTO folders (id, user_id, parent_id, name, drive_folder_id, sort_order, created_at, updated_at, deleted_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)
         ON CONFLICT(id) DO UPDATE SET
           parent_id=?3, name=?4, drive_folder_id=?5, sort_order=?6, updated_at=?8, deleted_at=?9
         WHERE excluded.updated_at > folders.updated_at`,
      ).bind(f.id, uid, f.parentId ?? null, f.name, f.driveFolderId ?? null, f.sortOrder ?? 0,
             f.createdAt ?? Date.now(), f.updatedAt ?? Date.now(), f.deletedAt ?? null),
    )
  }
  for (const d of b.documents ?? []) {
    stmts.push(
      c.env.DB.prepare(
        `INSERT INTO documents (id, user_id, folder_id, name, page_count, size_bytes, format, drive_file_id, sync_state, created_at, updated_at, deleted_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)
         ON CONFLICT(id) DO UPDATE SET
           folder_id=?3, name=?4, page_count=?5, size_bytes=?6, format=?7,
           drive_file_id=?8, sync_state=?9, updated_at=?11, deleted_at=?12
         WHERE excluded.updated_at > documents.updated_at`,
      ).bind(d.id, uid, d.folderId ?? null, d.name, d.pageCount ?? 0, d.sizeBytes ?? 0,
             d.format ?? 'PDF', d.driveFileId ?? null, d.syncState ?? 'LOCAL_ONLY',
             d.createdAt ?? Date.now(), d.updatedAt ?? Date.now(), d.deletedAt ?? null),
    )
  }
  if (stmts.length) await c.env.DB.batch(stmts)
  return c.json({ ok: true, applied: stmts.length, now: Date.now() })
})

export default app
