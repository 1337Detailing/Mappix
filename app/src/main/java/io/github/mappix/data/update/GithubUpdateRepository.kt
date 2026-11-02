package io.github.mappix.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mappix.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ReleaseAsset(val name: String, val size: Long = 0, val digest: String? = null,
    @kotlinx.serialization.SerialName("browser_download_url") val downloadUrl: String)

@Serializable
data class GithubRelease(val tag_name: String, val name: String = "", val body: String? = null,
    val draft: Boolean = false, val prerelease: Boolean = false, val assets: List<ReleaseAsset> = emptyList())

data class UpdateInfo(val version: String, val notes: String, val apk: ReleaseAsset,
    val checksum: String)

/** Téléchargements GitHub Releases HTTPS avec contrôle SHA-256 obligatoire. */
@Singleton
class GithubUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    val configured: Boolean get() = BuildConfig.UPDATE_REPOSITORY.isNotBlank()

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        require(configured) { "update_repository_missing" }
        val url = "https://api.github.com/repos/${BuildConfig.UPDATE_REPOSITORY}/releases/latest"
        val release = json.decodeFromString(GithubRelease.serializer(), fetchText(url, 512_000))
        if (release.draft || release.prerelease || !isNewer(release.tag_name, BuildConfig.VERSION_NAME)) return@withContext null
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) &&
            it.size in 1..MAX_APK_BYTES && "mappix" in it.name.lowercase() }
            ?: error("update_apk_missing")
        val checksum = when {
            apk.digest?.startsWith("sha256:", ignoreCase = true) == true -> apk.digest.substringAfter(':')
            else -> {
                val sha = release.assets.firstOrNull { it.name == "${apk.name}.sha256" }
                    ?: error("update_checksum_missing")
                fetchText(sha.downloadUrl, 1024).trim().substringBefore(' ').trim()
            }
        }.lowercase()
        require(checksum.matches(Regex("[a-f0-9]{64}"))) { "update_checksum_invalid" }
        UpdateInfo(release.tag_name, release.body.orEmpty().take(4000), apk, checksum)
    }

    suspend fun download(update: UpdateInfo, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(outputDir, "mappix-update.apk")
        val partial = File(outputDir, "mappix-update.partial.apk")
        try {
            val connection = connect(update.apk.downloadUrl)
            try {
                val length = connection.contentLengthLong
                val expected = update.apk.size
                require(expected in 1..MAX_APK_BYTES) { "update_size_invalid" }
                require(length == -1L || length == expected) { "update_length_mismatch" }
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_APK_BYTES) { "update_too_large" }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            onProgress((total * 100L / expected).toInt().coerceIn(0, 100))
                        }
                        require(total == expected) { "update_incomplete" }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual.equals(update.checksum, true)) { "update_checksum_mismatch" }
                // Contrôle du paquet avant de proposer l'installation.
                val archive = context.packageManager.getPackageArchiveInfo(partial.absolutePath, 0)
                require(archive?.packageName == context.packageName) { "update_wrong_package" }
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "update_file_failure" }
                target
            } finally { connection.disconnect() }
        } catch (e: Exception) {
            partial.delete()
            throw e
        }
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(): Intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installIntent(file: File): Intent {
        require(file.isFile && file.parentFile?.canonicalFile == File(context.cacheDir, "updates").canonicalFile)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun fetchText(url: String, max: Int): String {
        val conn = connect(url)
        return try {
            conn.inputStream.use { input ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(out.size() + count <= max) { "update_response_too_large" }
                    out.write(buffer, 0, count)
                }
                out.toString("UTF-8")
            }
        } finally { conn.disconnect() }
    }

    private fun connect(url: String): HttpURLConnection {
        val initial = URL(url)
        require(initial.protocol == "https") { "update_https_required" }
        var next = initial
        repeat(6) {
            require(allowedHost(next.host)) { "update_untrusted_host" }
            val c = (next.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 12_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Mappix/${BuildConfig.VERSION_NAME}")
            }
            when (c.responseCode) {
                in 200..299 -> return c
                in 300..399 -> {
                    val location = c.getHeaderField("Location") ?: error("update_redirect_missing")
                    next = URL(next, location)
                    c.disconnect()
                }
                else -> {
                    val code = c.responseCode
                    c.disconnect()
                    error("update_http_$code")
                }
            }
        }
        error("update_redirect_loop")
    }

    private fun allowedHost(host: String): Boolean = host == "api.github.com" || host == "github.com" ||
        host == "objects.githubusercontent.com" || host == "release-assets.githubusercontent.com" ||
        host == "github-releases.githubusercontent.com"

    companion object {
        private const val MAX_APK_BYTES = 300L * 1024 * 1024
        fun isNewer(candidate: String, installed: String): Boolean {
            val regex = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$")
            val a = regex.matchEntire(candidate.trim()) ?: return false
            val b = regex.matchEntire(installed.trim()) ?: return false
            for (i in 1..3) {
                val x = a.groupValues[i].toLongOrNull() ?: return false
                val y = b.groupValues[i].toLongOrNull() ?: return false
                if (x != y) return x > y
            }
            return false
        }
    }
}
