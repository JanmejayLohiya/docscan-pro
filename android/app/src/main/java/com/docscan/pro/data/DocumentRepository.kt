package com.docscan.pro.data

import android.content.Context
import android.net.Uri
import com.docscan.pro.data.local.DocumentDao
import com.docscan.pro.data.local.DocumentEntity
import com.docscan.pro.data.local.PageEntity
import com.docscan.pro.domain.Document
import com.docscan.pro.feature.scan.ScannedPages
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-first document store (Phase 1). Files (PDF + page images) are copied
 * into app-private storage; metadata lives in Room. Cross-device sync with the
 * Cloudflare API arrives in Phase 4 (see PHASED_PLAN.md).
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val dao: DocumentDao,
    @ApplicationContext private val context: Context,
) {
    fun observeDocuments(): Flow<List<Document>> =
        dao.observeDocuments().map { list -> list.map(::toDomain) }

    /** Persists a scan result: copies files locally and writes metadata rows. */
    suspend fun saveScannedDocument(name: String, scan: ScannedPages): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val id = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val dir = File(context.filesDir, "documents/$id").apply { mkdirs() }

                val pages = scan.pageUris.mapIndexed { index, uri ->
                    val file = File(dir, "page_$index.jpg")
                    copyUriToFile(uri, file)
                    PageEntity(
                        id = UUID.randomUUID().toString(),
                        documentId = id,
                        orderIndex = index,
                        imagePath = file.absolutePath,
                        updatedAt = now,
                    )
                }

                val pdfFile = File(dir, "$id.pdf")
                val sizeBytes = if (scan.pdfUri != null) {
                    copyUriToFile(scan.pdfUri, pdfFile)
                    pdfFile.length()
                } else {
                    0L
                }

                val document = DocumentEntity(
                    id = id,
                    name = name,
                    pageCount = scan.pageUris.size,
                    sizeBytes = sizeBytes,
                    format = "PDF",
                    filePath = pdfFile.absolutePath,
                    syncState = "LOCAL_ONLY",
                    folderId = null,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                )
                dao.insertDocumentWithPages(document, pages)
                id
            }
        }

    suspend fun delete(id: String) = dao.softDelete(id, System.currentTimeMillis())

    private fun copyUriToFile(uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open scan result stream: $uri")
    }

    private fun toDomain(e: DocumentEntity) = Document(
        id = e.id,
        name = e.name,
        pageCount = e.pageCount,
        sizeBytes = e.sizeBytes,
        format = e.format,
        syncState = e.syncState,
        filePath = e.filePath,
        createdAt = e.createdAt,
    )
}
