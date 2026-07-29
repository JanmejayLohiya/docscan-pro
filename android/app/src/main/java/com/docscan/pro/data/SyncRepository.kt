package com.docscan.pro.data

import com.docscan.pro.data.local.DocumentDao
import com.docscan.pro.network.ScanProApi
import com.docscan.pro.network.SyncDocument
import com.docscan.pro.network.SyncFolder
import com.docscan.pro.network.SyncPush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up document + folder metadata to the Cloudflare backend (requires a
 * signed-in user; the Firebase token is attached by AuthInterceptor). Files
 * themselves stay on the device / user's drive — this syncs metadata only.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val dao: DocumentDao,
    private val api: ScanProApi,
) {
    /** Pushes all local metadata; returns the number of records the server applied. */
    suspend fun backUpMetadata(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val documents = dao.getAllDocuments().map {
                SyncDocument(
                    id = it.id, folderId = it.folderId, name = it.name, pageCount = it.pageCount,
                    sizeBytes = it.sizeBytes, format = it.format, syncState = it.syncState,
                    createdAt = it.createdAt, updatedAt = it.updatedAt, deletedAt = it.deletedAt,
                )
            }
            val folders = dao.getAllFolders().map {
                SyncFolder(id = it.id, name = it.name, createdAt = it.createdAt, updatedAt = it.updatedAt, deletedAt = it.deletedAt)
            }
            api.push(SyncPush(documents, folders)).applied
        }
    }
}
