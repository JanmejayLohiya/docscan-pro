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
import com.docscan.pro.util.eraseImage
import com.docscan.pro.util.rotateImage
import com.docscan.pro.util.scaleImage
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
 * storage; metadata lives in Room. Image edits are NON-DESTRUCTIVE: each writes
 * a new versioned image file and repoints the page at it, leaving the old file
 * on disk so undo/redo can restore it. Every edit rebuilds the PDF and refreshes
 * the document's metadata. Cross-device sync arrives in Phase 4 (see PHASED_PLAN.md).
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
                    val file = File(dir, "page_${pageId}.jpg")
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

    // ---- Edit operations ----
    /** Appends newly scanned/imported pages. FR-E.3 */
    suspend fun addPages(documentId: String, scan: ScannedPages): Result<Unit> = edit(documentId) {
        val dir = File(context.filesDir, "documents/$documentId").apply { mkdirs() }
        val now = System.currentTimeMillis()
        var index = dao.getPages(documentId).size
        val newPages = scan.pageUris.map { uri ->
            val pageId = UUID.randomUUID().toString()
            val file = File(dir, "page_${pageId}.jpg")
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

    /** Removes a page and re-indexes the rest. Keeps the image file for undo. FR-E.2 */
    suspend fun removePage(documentId: String, pageId: String): Result<Unit> = edit(documentId) {
        dao.deletePageById(pageId)
        val now = System.currentTimeMillis()
        val reindexed = dao.getPages(documentId).mapIndexed { i, p -> p.copy(orderIndex = i, updatedAt = now) }
        dao.updatePages(reindexed)
    }

    /** Rotates a page 90° clockwise into a new image version. FR-E.4 */
    suspend fun rotatePage(documentId: String, pageId: String): Result<Unit> = edit(documentId) {
        val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
        val dst = versionedFile(documentId, pageId)
        rotateImage(page.imagePath, dst.absolutePath, 90)
        dao.updatePages(listOf(page.copy(imagePath = dst.absolutePath, updatedAt = System.currentTimeMillis())))
    }

    /** Replaces a page's image with a processed source (e.g. a crop result). FR-E.4 */
    suspend fun replacePageImage(documentId: String, pageId: String, source: Uri): Result<Unit> =
        edit(documentId) {
            val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
            val dst = versionedFile(documentId, pageId)
            copyUriToFile(source, dst)
            dao.updatePages(listOf(page.copy(imagePath = dst.absolutePath, updatedAt = System.currentTimeMillis())))
        }

    /** Scales a page's image down to [maxEdge] px on its longest side. FR-E.9 */
    suspend fun resizePage(documentId: String, pageId: String, maxEdge: Int = 1600): Result<Unit> =
        edit(documentId) {
            val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
            val dst = versionedFile(documentId, pageId)
            scaleImage(page.imagePath, dst.absolutePath, maxEdge)
            dao.updatePages(listOf(page.copy(imagePath = dst.absolutePath, updatedAt = System.currentTimeMillis())))
        }

    /** Inserts external images as new pages. FR-E.10 */
    suspend fun insertImages(documentId: String, uris: List<Uri>): Result<Unit> =
        addPages(documentId, ScannedPages(pdfUri = null, pageUris = uris))

    /** Paints erase strokes (paper-white) into a new image version. FR-E.5 */
    suspend fun erasePage(
        documentId: String,
        pageId: String,
        strokes: List<FloatArray>,
        displayW: Float,
        displayH: Float,
        brushPx: Float,
    ): Result<Unit> = edit(documentId) {
        val page = dao.getPages(documentId).firstOrNull { it.id == pageId } ?: return@edit
        val dst = versionedFile(documentId, pageId)
        eraseImage(page.imagePath, dst.absolutePath, strokes, displayW, displayH, brushPx)
        dao.updatePages(listOf(page.copy(imagePath = dst.absolutePath, updatedAt = System.currentTimeMillis())))
    }

    /** Restores the page set to a previous snapshot (undo/redo). FR-E.7 */
    suspend fun restorePages(documentId: String, pages: List<Page>): Result<Unit> = edit(documentId) {
        val now = System.currentTimeMillis()
        dao.deletePagesForDocument(documentId)
        val entities = pages.mapIndexed { i, p -> PageEntity(p.id, documentId, i, p.imagePath, now) }
        dao.insertPages(entities)
    }

    /** Renames a stored PDF document. */
    suspend fun rename(id: String, name: String) =
        dao.renameDocument(id, name.trim().ifBlank { "Untitled" }, System.currentTimeMillis())

    suspend fun delete(id: String) = dao.softDelete(id, System.currentTimeMillis())

    /** Runs [mutate], then rebuilds the PDF from current pages and refreshes metadata. */
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

    private fun versionedFile(documentId: String, pageId: String): File {
        val dir = File(context.filesDir, "documents/$documentId").apply { mkdirs() }
        return File(dir, "page_${pageId}_${UUID.randomUUID()}.jpg")
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
