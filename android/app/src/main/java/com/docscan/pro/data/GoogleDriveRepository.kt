package com.docscan.pro.data

import android.content.Context
import android.content.Intent
import com.docscan.pro.data.local.DocumentEntity
import com.docscan.pro.data.local.DocumentDao
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive backup, as an alternative to the built-in Cloudflare (KV) backup.
 *
 * Uses Google Sign-In with the least-privilege `drive.file` scope (the app can
 * only ever see files it created). PDFs are uploaded to the user's own Drive via
 * the REST API using a plain OkHttp client — deliberately NOT the app's shared
 * client, so the Firebase token is never sent to Google.
 *
 * Requires one-time Google Cloud setup: enable the Drive API, configure the OAuth
 * consent screen with the drive.file scope, and register the app's SHA-1.
 */
@Singleton
class GoogleDriveRepository @Inject constructor(
    private val dao: DocumentDao,
    @ApplicationContext private val context: Context,
) {
    private val http = OkHttpClient()

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DRIVE_FILE_SCOPE))
        .build()

    private fun client() = GoogleSignIn.getClient(context, signInOptions)

    /** Intent to launch the Google account chooser / consent flow. */
    fun signInIntent(): Intent = client().signInIntent

    /** The connected account's email, or null if Drive isn't connected. */
    fun connectedEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    /** Completes sign-in from the account-chooser result; returns the email. */
    fun handleSignInResult(data: Intent?): Result<String> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        account.email ?: "Google account"
    }

    fun disconnect() {
        runCatching { client().signOut() }
    }

    /** Uploads every local PDF to Drive; returns the number backed up. */
    suspend fun backUp(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = accessToken()
            var uploaded = 0
            for (doc in dao.getAllDocuments()) {
                if (doc.deletedAt != null) continue
                val file = File(doc.filePath)
                if (!file.exists() || file.length() == 0L) continue
                val bytes = file.readBytes()
                val driveId = if (doc.driveFileId != null) {
                    updateFile(token, doc.driveFileId, bytes)
                } else {
                    createFile(token, "${doc.name}.pdf", bytes).also { dao.setDriveFileId(doc.id, it) }
                }
                dao.setSyncState(doc.id, "SYNCED")
                if (driveId.isNotBlank()) uploaded++
            }
            uploaded
        }
    }

    /** Downloads any app-created Drive PDFs not present locally; returns the number restored. */
    suspend fun restore(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = accessToken()
            val localDriveIds = dao.getAllDocuments().mapNotNull { it.driveFileId }.toSet()
            var restored = 0
            for ((driveId, name) in listFiles(token)) {
                if (driveId in localDriveIds) continue
                val bytes = downloadFile(token, driveId)
                if (bytes.isEmpty()) continue
                val localId = UUID.randomUUID().toString()
                val dir = File(context.filesDir, "documents/$localId").apply { mkdirs() }
                val pdf = File(dir, "$localId.pdf")
                pdf.writeBytes(bytes)
                val now = System.currentTimeMillis()
                dao.insertDocument(
                    DocumentEntity(
                        id = localId, name = name.removeSuffix(".pdf"), pageCount = 0,
                        sizeBytes = pdf.length(), format = "PDF", filePath = pdf.absolutePath,
                        syncState = "SYNCED", folderId = null, createdAt = now, updatedAt = now,
                        deletedAt = null, ocrText = null, driveFileId = driveId,
                    ),
                )
                restored++
            }
            restored
        }
    }

    // ---- Drive REST helpers (all run on an IO dispatcher via the callers above) ----

    /** OAuth access token for the drive.file scope. Blocking — call off the main thread. */
    private fun accessToken(): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: error("Google Drive isn't connected")
        return GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_FILE_SCOPE")
    }

    private fun createFile(token: String, name: String, bytes: ByteArray): String {
        val metadata = JSONObject().put("name", name).put("mimeType", PDF_MIME).toString()
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(bytes.toRequestBody(PDF_MIME.toMediaType()))
            .build()
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        return execForId(req)
    }

    private fun updateFile(token: String, driveId: String, bytes: ByteArray): String {
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$driveId?uploadType=media&fields=id")
            .addHeader("Authorization", "Bearer $token")
            .patch(bytes.toRequestBody(PDF_MIME.toMediaType()))
            .build()
        return execForId(req)
    }

    private fun downloadFile(token: String, driveId: String): ByteArray {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$driveId?alt=media")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return ByteArray(0)
            return resp.body?.bytes() ?: ByteArray(0)
        }
    }

    /** Lists app-created PDFs as (driveId, name) pairs. */
    private fun listFiles(token: String): List<Pair<String, String>> {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?spaces=drive&q=mimeType%3D%27application%2Fpdf%27&fields=files(id%2Cname)&pageSize=1000")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful || text.isBlank()) return emptyList()
            val files = JSONObject(text).optJSONArray("files") ?: return emptyList()
            return (0 until files.length()).map { i ->
                val o = files.getJSONObject(i)
                o.getString("id") to o.optString("name", "Document")
            }
        }
    }

    private fun execForId(req: Request): String {
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Drive error ${resp.code}: ${text.take(200)}")
            return JSONObject(text).optString("id")
        }
    }

    companion object {
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val PDF_MIME = "application/pdf"
    }
}
