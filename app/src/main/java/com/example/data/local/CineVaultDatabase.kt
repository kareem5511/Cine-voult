package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM offline_downloads ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<OfflineDownloadEntity>>

    @Query("SELECT * FROM offline_downloads WHERE downloadKey = :downloadKey LIMIT 1")
    suspend fun getByKey(downloadKey: String): OfflineDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineDownloadEntity)

    @Query(
        "UPDATE offline_downloads SET status = :status, progressPercent = :progress, " +
            "bytesDownloaded = :bytes, totalBytes = :total, localFilePath = :localPath, " +
            "localSubtitlePath = :subPath, errorMessage = :error WHERE downloadKey = :key"
    )
    suspend fun updateProgress(
        key: String,
        status: String,
        progress: Int,
        bytes: Long,
        total: Long,
        localPath: String,
        subPath: String,
        error: String = ""
    )

    @Query("DELETE FROM offline_downloads WHERE downloadKey = :downloadKey")
    suspend fun deleteByKey(downloadKey: String)
}

@Dao
interface LocalMediaDao {
    @Query("SELECT * FROM local_media_files ORDER BY addedAtMs DESC")
    fun observeAll(): Flow<List<LocalMediaEntity>>

    @Query("SELECT * FROM local_media_files WHERE uriString = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): LocalMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalMediaEntity)

    @Query("DELETE FROM local_media_files WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)
}

@Dao
interface LocalAddonDao {
    @Query("SELECT * FROM local_addons ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<LocalAddonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalAddonEntity)

    @Query("UPDATE local_addons SET enabled = :enabled, updatedAtMs = :updatedAtMs WHERE addonKey = :addonKey")
    suspend fun setEnabled(addonKey: String, enabled: Boolean, updatedAtMs: Long)

    @Query("DELETE FROM local_addons WHERE addonKey = :addonKey")
    suspend fun deleteByKey(addonKey: String)
}

@Dao
interface LocalLibraryDao {
    @Query("SELECT * FROM local_library_items ORDER BY lastWatchedAtMs DESC")
    fun observeAll(): Flow<List<LocalLibraryEntity>>

    @Query("SELECT * FROM local_library_items WHERE mediaKey = :mediaKey LIMIT 1")
    suspend fun getByKey(mediaKey: String): LocalLibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalLibraryEntity)

    @Query("DELETE FROM local_library_items WHERE mediaKey = :mediaKey")
    suspend fun deleteByKey(mediaKey: String)
}

@Database(
    entities = [
        OfflineDownloadEntity::class,
        LocalMediaEntity::class,
        LocalAddonEntity::class,
        LocalLibraryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CineVaultDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun localMediaDao(): LocalMediaDao
    abstract fun localAddonDao(): LocalAddonDao
    abstract fun localLibraryDao(): LocalLibraryDao

    companion object {
        @Volatile
        private var INSTANCE: CineVaultDatabase? = null

        fun getInstance(context: Context): CineVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CineVaultDatabase::class.java,
                    "cinevault_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
