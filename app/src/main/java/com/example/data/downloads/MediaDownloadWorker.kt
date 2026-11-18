package com.example.data.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.CineVaultDatabase
import com.example.data.local.DownloadStatus
import com.example.data.local.OfflineDownloadEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MediaDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val db = CineVaultDatabase.getInstance(appContext)
    private val dao = db.downloadDao()
    private val client: OkHttpClient by lazy {
        (appContext.applicationContext as? com.example.CineVaultApplication)?.sharedOkHttpClient
            ?: OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }

    override suspend fun doWork(): Result {
        val downloadKey = inputData.getString(KEY_DOWNLOAD_KEY) ?: return Result.failure()
        val entry = dao.getByKey(downloadKey) ?: return Result.failure()

        ensureChannel()
        runCatching {
            setForeground(createForegroundInfo(entry.title, 0, downloadKey.hashCode()))
        }

        val offlineDir = File(applicationContext.filesDir, "offline_media").apply { mkdirs() }
        val ext = guessExtension(entry.streamUrl)
        val outFile = File(offlineDir, "${downloadKey}.$ext")
        val subFile = File(offlineDir, "${downloadKey}.srt")

        return try {
            dao.updateProgress(
                key = downloadKey,
                status = DownloadStatus.DOWNLOADING.name,
                progress = 0,
                bytes = 0L,
                total = entry.totalBytes,
                localPath = outFile.absolutePath,
                subPath = ""
            )

            val existingBytes = if (outFile.exists() && entry.bytesDownloaded > 0L) outFile.length() else 0L
            val requestBuilder = Request.Builder()
                .url(entry.streamUrl)
                .header("User-Agent", "CineVault-Android/1.0")

            if (existingBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }
            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    error("HTTP ${response.code} downloading media")
                }
                val isPartial = response.code == 206
                val actualStartingBytes = if (isPartial) existingBytes else 0L
                val body = response.body ?: error("Empty response stream")
                val streamContentLength = body.contentLength()
                val totalBytes = when {
                    streamContentLength > 0L && isPartial -> actualStartingBytes + streamContentLength
                    streamContentLength > 0L -> streamContentLength
                    entry.totalBytes > 0L -> entry.totalBytes
                    else -> 0L
                }
                var downloaded = actualStartingBytes
                var lastPct = -1
                var lastUpdateMs = System.currentTimeMillis()
                var lastSpeedBytes = actualStartingBytes
                var lastSpeedTimeMs = System.currentTimeMillis()
                var currentSpeedEta = ""

                body.byteStream().use { input ->
                    FileOutputStream(outFile, isPartial).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (isStopped) {
                                val currentPct = if (totalBytes > 0L) ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                                dao.updateProgress(
                                    key = downloadKey,
                                    status = DownloadStatus.PAUSED.name,
                                    progress = currentPct,
                                    bytes = downloaded,
                                    total = totalBytes,
                                    localPath = outFile.absolutePath,
                                    subPath = "",
                                    error = "Paused"
                                )
                                return Result.failure()
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val pct = if (totalBytes > 0L) ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                            val now = System.currentTimeMillis()

                            // Calculate rolling speed & ETA
                            val speedElapsed = now - lastSpeedTimeMs
                            if (speedElapsed >= 1000L) {
                                val bytesDelta = downloaded - lastSpeedBytes
                                val bytesPerSec = (bytesDelta * 1000L) / speedElapsed
                                val speedStr = formatSpeed(bytesPerSec)
                                val etaStr = if (totalBytes > downloaded && bytesPerSec > 0L) {
                                    val etaSec = (totalBytes - downloaded) / bytesPerSec
                                    formatEta(etaSec)
                                } else ""
                                currentSpeedEta = if (etaStr.isNotBlank()) "$speedStr • $etaStr" else speedStr
                                lastSpeedBytes = downloaded
                                lastSpeedTimeMs = now
                            }

                            val shouldUpdateDb = (now - lastUpdateMs >= 800L) || (pct != lastPct && pct % 2 == 0)
                            if (shouldUpdateDb) {
                                lastUpdateMs = now
                                lastPct = pct
                                dao.updateProgress(
                                    key = downloadKey,
                                    status = DownloadStatus.DOWNLOADING.name,
                                    progress = pct,
                                    bytes = downloaded,
                                    total = totalBytes,
                                    localPath = outFile.absolutePath,
                                    subPath = "",
                                    error = currentSpeedEta
                                )
                                runCatching {
                                    setForeground(createForegroundInfo(entry.title, pct, downloadKey.hashCode(), currentSpeedEta))
                                }
                            }
                        }
                    }
                }

                // Download sidecar subtitle if provided
                var savedSubPath = ""
                if (entry.subtitleUrl.isNotBlank()) {
                    runCatching {
                        val subReq = Request.Builder().url(entry.subtitleUrl).build()
                        client.newCall(subReq).execute().use { subResp ->
                            if (subResp.isSuccessful) {
                                subResp.body?.bytes()?.let { bytes ->
                                    subFile.writeBytes(bytes)
                                    savedSubPath = subFile.absolutePath
                                }
                            }
                        }
                    }
                }

                val finalSize = outFile.length()
                dao.updateProgress(
                    key = downloadKey,
                    status = DownloadStatus.COMPLETED.name,
                    progress = 100,
                    bytes = finalSize,
                    total = if (totalBytes > 0L) totalBytes else finalSize,
                    localPath = outFile.absolutePath,
                    subPath = savedSubPath
                )
            }
            Result.success()
        } catch (e: Exception) {
            dao.updateProgress(
                key = downloadKey,
                status = DownloadStatus.FAILED.name,
                progress = 0,
                bytes = outFile.takeIf { it.exists() }?.length() ?: 0L,
                total = entry.totalBytes,
                localPath = outFile.absolutePath,
                subPath = "",
                error = e.localizedMessage ?: "Download failed"
            )
            Result.failure()
        }
    }

    private fun guessExtension(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".mkv") -> "mkv"
            clean.endsWith(".webm") -> "webm"
            clean.endsWith(".m3u8") -> "m3u8"
            else -> "mp4"
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Offline Media Downloads",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun createForegroundInfo(title: String, progress: Int, notifId: Int, speedInfo: String = ""): ForegroundInfo {
        val contentText = if (speedInfo.isNotBlank()) "$progress% • $speedInfo" else "$progress% complete"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $title")
            .setContentText(contentText)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB/s", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB/s", kb)
            else -> "${bytesPerSec} B/s"
        }
    }

    private fun formatEta(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins >= 60 -> "${mins / 60}h ${mins % 60}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    companion object {
        const val KEY_DOWNLOAD_KEY = "download_key"
        const val CHANNEL_ID = "cinevault_downloads"
    }
}

class OfflineDownloadManager(private val context: Context) {
    private val db = CineVaultDatabase.getInstance(context)
    private val workManager = WorkManager.getInstance(context)

    suspend fun enqueueDownload(
        mediaKey: String,
        title: String,
        mediaType: String,
        posterUrl: String,
        streamUrl: String,
        subtitleUrl: String,
        qualityLabel: String,
        wifiOnly: Boolean,
        initialTotalBytes: Long = 0L
    ): String {
        val downloadKey = "${mediaKey}_${streamUrl.hashCode().toUInt()}"
        val entity = OfflineDownloadEntity(
            downloadKey = downloadKey,
            mediaKey = mediaKey,
            title = title,
            mediaType = mediaType,
            posterUrl = posterUrl,
            streamUrl = streamUrl,
            subtitleUrl = subtitleUrl,
            localFilePath = "",
            localSubtitlePath = "",
            qualityLabel = qualityLabel,
            status = DownloadStatus.QUEUED.name,
            totalBytes = initialTotalBytes
        )
        db.downloadDao().upsert(entity)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setConstraints(constraints)
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                Data.Builder()
                    .putString(MediaDownloadWorker.KEY_DOWNLOAD_KEY, downloadKey)
                    .build()
            )
            .addTag(downloadKey)
            .build()

        workManager.enqueueUniqueWork(downloadKey, ExistingWorkPolicy.REPLACE, request)
        return downloadKey
    }

    suspend fun pauseDownload(downloadKey: String) {
        workManager.cancelUniqueWork(downloadKey)
        val item = db.downloadDao().getByKey(downloadKey) ?: return
        db.downloadDao().updateProgress(
            key = downloadKey,
            status = DownloadStatus.PAUSED.name,
            progress = item.progressPercent,
            bytes = item.bytesDownloaded,
            total = item.totalBytes,
            localPath = item.localFilePath,
            subPath = item.localSubtitlePath,
            error = "Paused"
        )
    }

    suspend fun resumeDownload(downloadKey: String, wifiOnly: Boolean) {
        val item = db.downloadDao().getByKey(downloadKey) ?: return
        db.downloadDao().updateProgress(
            key = downloadKey,
            status = DownloadStatus.QUEUED.name,
            progress = item.progressPercent,
            bytes = item.bytesDownloaded,
            total = item.totalBytes,
            localPath = item.localFilePath,
            subPath = item.localSubtitlePath,
            error = ""
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setConstraints(constraints)
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                Data.Builder()
                    .putString(MediaDownloadWorker.KEY_DOWNLOAD_KEY, downloadKey)
                    .build()
            )
            .addTag(downloadKey)
            .build()

        workManager.enqueueUniqueWork(downloadKey, ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun cancelAndDelete(downloadKey: String) {
        workManager.cancelUniqueWork(downloadKey)
        db.downloadDao().getByKey(downloadKey)?.let { item ->
            if (item.localFilePath.isNotBlank()) runCatching { File(item.localFilePath).delete() }
            if (item.localSubtitlePath.isNotBlank()) runCatching { File(item.localSubtitlePath).delete() }
        }
        db.downloadDao().deleteByKey(downloadKey)
    }
}
