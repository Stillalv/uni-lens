package com.unilens.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val sha256Checksum: String?
)

/**
 * Handles checking GitHub Releases, downloading APKs, validating SHA-256 checksums,
 * and opening the Android Package Installer.
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_REPO = "Stillalv/uni-lens"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        private const val PREFS_NAME = "unilens_update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_timestamp"
        private const val CHECK_CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a new release is available on GitHub.
     */
    suspend fun checkForUpdate(forceCheck: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()

        if (!forceCheck && (now - lastCheck < CHECK_CACHE_DURATION_MS)) {
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "UniLens-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyString)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val releaseNotes = json.optString("body", "")
            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "1.0.0"

            prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()

            if (isNewerVersion(tagName, currentVersion)) {
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                var sha256Url: String? = null

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = downloadUrl
                        } else if (name.endsWith(".sha256") || name.endsWith(".sha256.txt")) {
                            sha256Url = downloadUrl
                        }
                    }
                }

                // If sha256 asset exists, fetch it, otherwise extract from release notes
                var sha256Checksum = sha256Url?.let { fetchRemoteChecksum(it) }
                if (sha256Checksum.isNullOrBlank()) {
                    sha256Checksum = extractSha256FromText(releaseNotes)
                }

                if (!apkUrl.isNullOrBlank()) {
                    return@withContext UpdateInfo(
                        versionName = tagName,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = apkUrl,
                        sha256Checksum = sha256Checksum
                    )
                }
            }
        } catch (_: Exception) {
            // Fail silently without disrupting user
        }
        null
    }

    /**
     * Downloads APK, calculates SHA-256 hash, verifies integrity, and launches installer.
     */
    suspend fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "UniLens-${updateInfo.versionName}.apk")

            val request = Request.Builder()
                .url(updateInfo.apkDownloadUrl)
                .header("User-Agent", "UniLens-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download APK: ${response.code}"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("Empty body"))
            val totalBytes = responseBody.contentLength()
            var downloadedBytes = 0L

            responseBody.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            // Verify SHA-256 Checksum
            val calculatedHash = calculateFileSha256(apkFile)
            if (!updateInfo.sha256Checksum.isNullOrBlank()) {
                val expected = updateInfo.sha256Checksum.trim().lowercase()
                if (!calculatedHash.equals(expected, ignoreCase = true)) {
                    apkFile.delete()
                    return@withContext Result.failure(
                        SecurityException("SHA-256 mismatch! Expected $expected but got $calculatedHash")
                    )
                }
            }

            // Launch standard Package Installer via FileProvider
            withContext(Dispatchers.Main) {
                launchInstaller(apkFile)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun launchInstaller(apkFile: File) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    private fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun fetchRemoteChecksum(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            val content = response.body?.string()?.trim() ?: return null
            // Often .sha256 contains "hash filename" or just the hash
            content.split("\\s+".toRegex()).firstOrNull()?.trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSha256FromText(text: String): String? {
        val regex = Regex("""\b([a-fA-F0-9]{64})\b""")
        return regex.find(text)?.value
    }

    private fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val rParts = remoteVersion.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(rParts.size, cParts.size)
        for (i in 0 until length) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
