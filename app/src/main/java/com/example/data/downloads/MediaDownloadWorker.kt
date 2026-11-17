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
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun doWork(): Result {
        val downloadKey = inputData.getString(KEY_DOWNLOAD_KEY) ?: return Result.failure()
        val entry = dao.getByKey(downloadKey) ?: return Result.failure()

        ensureChannel()
        setForeground(createForegroundInfo(entry.title, 0, downloadKey.hashCode()))

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

            val request = Request.Builder()
                .url(entry.streamUrl)
                .header("User-Agent", "CineVault-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code} downloading media")
                }
                val body = response.body ?: error("Empty response stream")
                val totalBytes = body.contentLength().coerceAtLeast(0L)
                var downloaded = 0L
                var lastPct = -1

                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            if (isStopped) {
                                dao.updateProgress(
                                    key = downloadKey,
                                    status = DownloadStatus.PAUSED.name,
                                    progress = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0,
                                    bytes = downloaded,
                                    total = totalBytes,
                                    localPath = outFile.absolutePath,
                                    subPath = ""
                                )
                                return Result.failure()
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val pct = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                            if (pct != lastPct && (pct % 5 == 0 || pct == 100)) {
                                lastPct = pct
                                dao.updateProgress(
                                    key = downloadKey,
                                    status = DownloadStatus.DOWNLOADING.name,
                                    progress = pct,
                                    bytes = downloaded,
                                    total = totalBytes,
                                    localPath = outFile.absolutePath,
                                    subPath = ""
                                )
                                setForeground(createForegroundInfo(entry.title, pct, downloadKey.hashCode()))
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
                    total = if (totalBytes > 0) totalBytes else finalSize,
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

    private fun createForegroundInfo(title: String, progress: Int, notifId: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $title")
            .setContentText("$progress% complete")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notifId, notification)
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
        wifiOnly: Boolean
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
            status = DownloadStatus.QUEUED.name
        )
        db.downloadDao().upsert(entity)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setConstraints(constraints)
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

    suspend fun cancelAndDelete(downloadKey: String) {
        workManager.cancelUniqueWork(downloadKey)
        db.downloadDao().getByKey(downloadKey)?.let { item ->
            if (item.localFilePath.isNotBlank()) runCatching { File(item.localFilePath).delete() }
            if (item.localSubtitlePath.isNotBlank()) runCatching { File(item.localSubtitlePath).delete() }
        }
        db.downloadDao().deleteByKey(downloadKey)
    }
}
