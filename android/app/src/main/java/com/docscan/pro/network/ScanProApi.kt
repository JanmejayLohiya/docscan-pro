package com.docscan.pro.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit contract for the ScanPro Cloudflare API. Metadata + sync only. */
interface ScanProApi {

    @GET("v1/documents")
    suspend fun getDocuments(@Query("folderId") folderId: String? = null): DocumentsResponse

    @GET("v1/folders")
    suspend fun getFolders(): FoldersResponse

    @GET("v1/entitlement")
    suspend fun getEntitlement(): EntitlementResponse
}

@Serializable
data class DocumentsResponse(val documents: List<DocumentDto>)

@Serializable
data class FoldersResponse(val folders: List<FolderDto>)

@Serializable
data class EntitlementResponse(val entitlement: EntitlementDto)

@Serializable
data class DocumentDto(
    val id: String,
    val name: String,
    @SerialName("page_count") val pageCount: Int = 0,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val format: String = "PDF",
    @SerialName("sync_state") val syncState: String = "LOCAL_ONLY",
    @SerialName("folder_id") val folderId: String? = null,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class FolderDto(
    val id: String,
    val name: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class EntitlementDto(
    @SerialName("user_id") val userId: String,
    val tier: String = "FREE",
    val status: String = "ACTIVE",
)
