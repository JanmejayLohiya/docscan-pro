package com.docscan.pro.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Retrofit contract for the ScanPro Cloudflare API. Metadata + sync only. */
interface ScanProApi {

    @GET("v1/documents")
    suspend fun getDocuments(@Query("folderId") folderId: String? = null): DocumentsResponse

    @GET("v1/folders")
    suspend fun getFolders(): FoldersResponse

    @GET("v1/entitlement")
    suspend fun getEntitlement(): EntitlementResponse

    /** Pushes document + folder metadata for backup / cross-device sync. */
    @POST("v1/sync")
    suspend fun push(@Body body: SyncPush): SyncResult

    /** Pulls everything changed on the server since [since] (0 = full snapshot). */
    @GET("v1/sync")
    suspend fun pull(@Query("since") since: Long = 0): SyncPull

    /** Uploads a document's PDF bytes to R2 backup (raw body). */
    @PUT("v1/files/{id}")
    suspend fun uploadFile(@Path("id") id: String, @Body body: RequestBody): FileResult

    /** Downloads a backed-up PDF as a stream. */
    @Streaming
    @GET("v1/files/{id}")
    suspend fun downloadFile(@Path("id") id: String): ResponseBody
}

@Serializable
data class FileResult(val ok: Boolean = true, val id: String = "", val bytes: Long = 0)

/** Server GET /v1/sync response. Rows come straight from D1, so keys are snake_case. */
@Serializable
data class SyncPull(
    val folders: List<PulledFolder> = emptyList(),
    val documents: List<PulledDocument> = emptyList(),
    val now: Long = 0,
)

@Serializable
data class PulledFolder(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class PulledDocument(
    val id: String,
    val name: String,
    @SerialName("folder_id") val folderId: String? = null,
    @SerialName("page_count") val pageCount: Int = 0,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val format: String = "PDF",
    @SerialName("sync_state") val syncState: String = "SYNCED",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class SyncPush(
    val documents: List<SyncDocument> = emptyList(),
    val folders: List<SyncFolder> = emptyList(),
)

@Serializable
data class SyncResult(val ok: Boolean = true, val applied: Int = 0, val now: Long = 0)

@Serializable
data class SyncDocument(
    val id: String,
    val folderId: String? = null,
    val name: String,
    val pageCount: Int = 0,
    val sizeBytes: Long = 0,
    val format: String = "PDF",
    val syncState: String = "LOCAL_ONLY",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class SyncFolder(
    val id: String,
    val name: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

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
