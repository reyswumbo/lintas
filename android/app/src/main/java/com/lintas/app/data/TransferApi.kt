package com.lintas.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import com.lintas.app.util.Constants

class TransferApi(private var baseUrl: String = Constants.DEFAULT_BASE_URL) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun updateBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    suspend fun uploadFile(file: Uri, context: Context): Result<TransferInfo> =
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val fileName = getFileName(file, context)
                val mimeType = contentResolver.getType(file) ?: "application/octet-stream"

                val inputStream: InputStream = contentResolver.openInputStream(file)
                    ?: return@withContext Result.failure(Exception("Cannot open file"))

                val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
                tempFile.outputStream().use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }

                val requestBody = tempFile.asRequestBody(mimeType.toMediaType())

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, requestBody)
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/api/upload")
                    .post(multipartBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                tempFile.delete()

                if (!response.isSuccessful) {
                    val error = extractErrorMessage(responseBody, "Upload failed: HTTP ${response.code}")
                    return@withContext Result.failure(Exception(error))
                }

                val transferInfo = json.decodeFromString<TransferInfo>(responseBody)
                Result.success(transferInfo)
            } catch (e: Exception) {
                Result.failure(Exception("Upload error: ${e.message}"))
            }
        }

    suspend fun getTransferInfo(code: String): Result<TransferInfo> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(code.trim(), StandardCharsets.UTF_8.toString())
                val request = Request.Builder()
                    .url("$baseUrl/api/transfer/$encoded")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val error = extractErrorMessage(responseBody, "Transfer not found")
                    return@withContext Result.failure(Exception(error))
                }

                val transferInfo = json.decodeFromString<TransferInfo>(responseBody)
                Result.success(transferInfo)
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }

    suspend fun downloadFile(
        code: String,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(code.trim(), StandardCharsets.UTF_8.toString())
                val request = Request.Builder()
                    .url("$baseUrl/api/download/$encoded")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val error = extractErrorMessage(errorBody, "Download failed: HTTP ${response.code}")
                    return@withContext Result.failure(Exception(error))
                }

                val body = response.body
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                val contentLength = body.contentLength().takeIf { it > 0 } ?: -1L
                val inputStream = body.byteStream()

                outputStream.use { output ->
                    inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                onProgress(totalBytesRead.toFloat() / contentLength.toFloat())
                            }
                        }
                    }
                }

                onProgress(1f)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Download error: ${e.message}"))
            }
        }

    private fun extractErrorMessage(body: String, fallback: String): String {
        return try {
            val obj = json.parseToJsonElement(body).jsonObject
            val detail = obj["detail"]?.jsonPrimitive?.content
            if (!detail.isNullOrEmpty()) detail else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun getFileName(uri: Uri, context: Context): String {
        var name = "unknown_file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
