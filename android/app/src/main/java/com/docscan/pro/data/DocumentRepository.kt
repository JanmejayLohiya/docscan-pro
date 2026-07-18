package com.docscan.pro.data

import android.content.Context
import android.net.Uri
import com.docscan.pro.data.local.DocumentDao
import com.docscan.pro.data.local.DocumentEntity
import com.docscan.pro.data.local.PageEntity
import com.docscan.pro.domain.Document
import com.docscan.pro.domain.Page
import com.docscan.pro.feature.scan.ScannedPages
import com.docscan.pro.util.buildPdf
import com.docscan.pro.util.rotateImageFile
import com.docscan.pro.util.scaleImageFile
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
 * Local-first document store. Files (PDF + page images) live in app-private
 * storage; metadata lives in Room. Every edit rebuilds the PDF from the current
 * page images and updates the document's page count/size. Cross-device sync with
 * the Cloudflare API arrives in Phase 4 (see PHASED_PLAN.md).
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val dao: DocumentDao,
    @ApplicationContext private val context: Context,
) {
    // ---- Reads ----
    fun observeDocuments(): Flow<List<Document>> =
        dao.observeDocuments().map { list -> list.map(::toDomain) }

    fun observeDocument(id: String): Flow<Document?> =
        dao.observeDocument(id).map { it?.let(::toDomain) }

    fun observePages(documentId: String): Flow<List<Page>> =
        dao.observePages(documentId).map { list -> list.map(::toPage) }

    // ---- Create ----
    suspend fun saveScannedDocument(name: String, scan: ScannedPages): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val id = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val dir = File(context.filesDir, "documents/$id").apply { mkdirs() }

                val pages = scan.pageUris.mapIndexed { index, uri ->
                    val pageId = UUID.randomUUID().toString()
                    val file = File(dir, "page_$pageId.jpg")
                    copyUriToFile(uri, file)
                    PageEntity(pageId, id, index, file.absolutePath, now)
                }

                val pdfFile = File(dir, "$id.pdf")
                val sizeBytes = if (scan.pdfUri != null) {
                    copyUriToFile(scan.pdfUri, pdfFile); pdfFile.length()
                } else {
                    0L
                }

                val document = DocumentEntity(
                    id = id, name = name, pageCount = pages.size, sizeBytes = sizeBytes,
                    format = "PDF", filePath = pdfFile.absolutePath, syncState = "LOCAL_ONLY",
                    folderId = null, createdAt = now, updatedAt = now, deletedAt = null,
                )
                dao.insertDocumentWithPages(document, pages)
                id
            }
        }

    // ---- Edit operations (Phase 2) ----
    /** Appends newly scanned/imported pages to an existing document. FR-E.3 */
    suspend fun addPages(documentId: String, scan: ScannedPages): Result<Unit> = edit(documentId) {
        val dir = File(context.filesDir, "documents/$documentId").apply { mkdirs() }
        val now = System.currentTimeMillis()
        var index = dao.getPages(documentId).size
        val newPages = scan.pageUris.map { uri ->
            val pageId = UUID.randomUUID().toString()
            val file = File(dir, "page_$pageId.jpg")
            copyUriToFile(uri, file)
            PageEntity(pageId, documentId, index++, file.absolutePath, now)
        }
        dao.insertPages(newPages)
    }

    /** Reorders pages to match [orderedPageIds]. FR-E.1 */
    suspend fun reorderPages(documentId: String, orderedPageIds: List<String>): Result<Unit> =
        edit(documentId) {
            val now = System.currentTimeMillis()
            val byId = dao.getPages(documentId).associateBy { it.id }
            val reindexed = orderedPageIds.mapIndexedNotNull { i, id ->
                byId[id]?.copy(orderIndex = i, updatedAt = now)
            }
            dao.updatePages(reindexed)
        }

    /** Removes a page and re-indexes the rest. FR-E.2 */
    suspend fun removePage(documentId: String, pageId: String): Result<Unit> = edit(documentId) {
        val page = dao.getPages(documentId).firstOrNull { it.id == pageId }
        dao.deletePageById(pageId)
        page?.let { runCatching { File(it.imagePath).delete() } }
        val now = System.currentTimeMillis()
        val reindexed = dao.getPages(documentId).mapIndexed { i, p -> p.copy(orderIndex = i, updatedAt = now) }
        dao.updatePages(reindexed)
    }

    /** Rotates a page 90° clockwise (destructive in Phase 2A). FR-E.4 */
    suspend fun rotatePage(documentId: String, pageId: String): Result<Unit> = edit(documentId) {
        val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
        rotateImageFile(page.imagePath, 90)
        dao.updatePages(listOf(page.copy(updatedAt = System.currentTimeMillis())))
    }

    /** Replaces a page's image with the given (already-processed) source, e.g. a crop result. FR-E.4 */
    suspend fun replacePageImage(documentId: String, pageId: String, source: Uri): Result<Unit> =
        edit(documentId) {
            val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
            copyUriToFile(source, File(page.imagePath))
            dao.updatePages(listOf(page.copy(updatedAt = System.currentTimeMillis())))
        }

    /** Scales a page's image down to [maxEdge] px on its longest side. FR-E.9 */
    suspend fun resizePage(documentId: String, pageId: String, maxEdge: Int = 1600): Result<Unit> =
        edit(documentId) {
            val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
            scaleImageFile(page.imagePath, maxEdge)
            dao.updatePages(listOf(page.copy(updatedAt = System.currentTimeMillis())))
        }

    /** Inserts external images (from the gallery/files) as new pages. FR-E.10 */
    suspend fun insertImages(documentId: String, uris: List<Uri>): Result<Unit> =
        addPages(documentId, ScannedPages(pdfUri = null, pageUris = uris))

    suspend fun delete(id: String) = dao.softDelete(id, System.currentTimeMillis())

    /**
     * Runs [mutate] on the IO dispatcher, then rebuilds the PDF from the current
     * ordered page images and refreshes the document's metadata.
     */
    private suspend fun edit(documentId: String, mutate: suspend () -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                mutate()
                val doc = dao.getDocument(documentId) ?: error("Document not found: $documentId")
                val pages = dao.getPages(documentId)
                val pdf = File(doc.filePath)
                buildPdf(pages.map { it.imagePath }, pdf)
                dao.updateDocument(
                    doc.copy(
                        pageCount = pages.size,
                        sizeBytes = pdf.length(),
                        syncState = "LOCAL_ONLY",
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

    private fun copyUriToFile(uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open stream: $uri")
    }

    private fun toDomain(e: DocumentEntity) = Document(
        id = e.id, name = e.name, pageCount = e.pageCount, sizeBytes = e.sizeBytes,
        format = e.format, syncState = e.syncState, filePath = e.filePath, createdAt = e.createdAt,
    )

    private fun toPage(e: PageEntity) = Page(id = e.id, orderIndex = e.orderIndex, imagePath = e.imagePath)
}
