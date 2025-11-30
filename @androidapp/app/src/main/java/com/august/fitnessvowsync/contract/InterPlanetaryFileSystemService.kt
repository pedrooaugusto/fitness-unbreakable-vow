package com.august.fitnessvowsync.contract

import com.august.fitnessvowsync.helpers.SettingsService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class InterPlanetaryFileSystemService @Inject constructor(private val settings: SettingsService) {
    private val PINATA_UPLOAD_ENDPOINT: String = "https://uploads.pinata.cloud/v3/files"

    fun upload(file: ByteArray): UploadResponse {
        val cid = uploadToPinata(file)
        val sha256 = file.sha256().toHex()

        return UploadResponse(cid, sha256)
    }

    private fun uploadToPinata(file: ByteArray): String {
        val pinataApiToken = settings.getPinataApiToken() ?: error("Pinata API Token Not Set.")

        if (pinataApiToken == "test") return "cid"

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val fileName = "FitVow-attestation-${System.currentTimeMillis()}.pem"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.Companion.FORM)
            .addFormDataPart("network", "public")
            .addFormDataPart("name", fileName)
            .addFormDataPart(
                "file",
                fileName,
                file.toRequestBody("application/plain-text".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(PINATA_UPLOAD_ENDPOINT)
            .header("Authorization", "Bearer $pinataApiToken")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Upload failed: ${response.code} - ${response.message}")

            val bodyString = response.body?.string() ?: error("Unable to upload attestation to ipfs.")

            val json = JSONObject(bodyString)
            val data = json.optJSONObject("data") ?: error("Unable to upload attestation to ipfs. Data cannot be found.")
            val cid = data.optString("cid") ?: error("Unable to upload attestation to ipfs. CID cannot be found.")

            return cid
        }
    }

    private fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    data class UploadResponse(
        val cid: String,
        val sha256: String
    )
}