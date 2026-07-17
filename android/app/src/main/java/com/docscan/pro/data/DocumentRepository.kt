package com.docscan.pro.data

import com.docscan.pro.network.DocumentDto
import com.docscan.pro.network.ScanProApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin repository over the API. In the full app this also reads/writes the local
 * Room cache and reconciles via the /v1/sync delta endpoint (see ENGINEERING_SPEC.md §5).
 * The scaffold fetches straight from the network.
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val api: ScanProApi,
) {
    suspend fun getDocuments(folderId: String? = null): Result<List<DocumentDto>> =
        runCatching { api.getDocuments(folderId).documents }
}
