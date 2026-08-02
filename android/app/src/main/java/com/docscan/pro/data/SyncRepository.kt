package com.docscan.pro.data

import android.content.Context
import com.docscan.pro.data.local.DocumentDao
import com.docscan.pro.data.local.DocumentEntity
import com.docscan.pro.data.local.FolderEntity
import com.docscan.pro.network.ScanProApi
import com.docscan.pro.network.SyncDocument
import com.docscan.pro.network.SyncFolder
import com.docscan.pro.network.SyncPush
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way cloud backup against the Cloudflare backend (requires a signed-in
 * user; the Firebase token is attached by AuthInterceptor).
 *
 *  - [backUp]  pushes document + folder metadata, then uploads each PDF to R2.
 *  - [restore] pulls metadata and downloads any PDFs missing on this device.
 *
 * Per-page edit images stay local, so a restored document is viewable/shareable
 * but re-opens as a flat PDF (not re-editable) until it is re-scanned.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val dao: DocumentDao,
    private val api: ScanProApi,
    @ApplicationContext private val context: Context,
) {
    private val pdfMediaType = "application/pdf".toMediaType()

    /** Pushes metadata and uploads every local PDF; returns the number of files backed up. */
    suspend fun backUp(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val docs = dao.getAllDocuments()
            val folders = dao.getAllFolders()

            api.push(
                SyncPush(
                    documents = docs.map {
                        SyncDocument(
                            id = it.id, folderId = it.folderId, name = it.name, pageCount = it.pageCount,
                            sizeBytes = it.sizeBytes, format = it.format, syncState = it.syncState,
                            createdAt = it.createdAt, updatedAt = it.updatedAt, deletedAt = it.deletedAt,
                        )
                    },
                    folders = folders.map {
                        SyncFolder(it.id, it.name, it.createdAt, it.updatedAt, it.deletedAt)
                    },
                ),
            )

            var uploaded = 0
            for (doc in docs) {
                if (doc.deletedAt != null) continue
                val file = File(doc.filePath)
                if (!file.exists() || file.length() == 0L) continue
                api.uploadFile(doc.id, file.readBytes().toRequestBody(pdfMediaType))
                dao.setSyncState(doc.id, "SYNCED")
                uploaded++
            }
            uploaded
        }
    }

    /** Pulls metadata and downloads any documents missing locally; returns the number restored. */
    suspend fun restore(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val remote = api.pull(0)
            val localFolderIds = dao.getAllFolders().map { it.id }.toSet()
            val localDocIds = dao.getAllDocuments().map { it.id }.toSet()

            for (f in remote.folders) {
                if (f.deletedAt != null || f.id in localFolderIds) continue
                dao.insertFolder(FolderEntity(f.id, f.name, f.createdAt, f.updatedAt, f.deletedAt))
            }

            var restored = 0
            for (d in remote.documents) {
                if (d.deletedAt != null || d.id in localDocIds) continue
                val bytes = runCatching { api.downloadFile(d.id).use { it.bytes() } }.getOrNull() ?: continue
                if (bytes.isEmpty()) continue
                val dir = File(context.filesDir, "documents/${d.id}").apply { mkdirs() }
                val pdf = File(dir, "${d.id}.pdf")
                pdf.writeBytes(bytes)
                dao.insertDocument(
                    DocumentEntity(
                        id = d.id, name = d.name, pageCount = d.pageCount, sizeBytes = pdf.length(),
                        format = d.format, filePath = pdf.absolutePath, syncState = "SYNCED",
                        folderId = d.folderId, createdAt = d.createdAt, updatedAt = d.updatedAt,
                        deletedAt = null, ocrText = null,
                    ),
                )
                restored++
            }
            restored
        }
    }
}
