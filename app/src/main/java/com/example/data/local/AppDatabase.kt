package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        NotificationEntity::class,
        LocalFileEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        RecentPlayedEntity::class,
        ImportedMediaEntity::class,
        CachedChannelMediaEntity::class,
        ReelCommentEntity::class,
        ReelLikeEntity::class,
        FeaturedPreferenceEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE imported_media ADD COLUMN channelName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE imported_media ADD COLUMN channelHandle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_channel_media ADD COLUMN channelName TEXT NOT NULL DEFAULT 'Rahma Riad | رحمة رياض'")
                db.execSQL("ALTER TABLE cached_channel_media ADD COLUMN channelHandle TEXT NOT NULL DEFAULT '@RahmaRiad'")
                db.execSQL("ALTER TABLE cached_channel_media ADD COLUMN isOfficialChannel INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_files ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'AUDIO'")
                db.execSQL("ALTER TABLE local_files ADD COLUMN folderName TEXT NOT NULL DEFAULT 'موسيقى جهازي'")
                db.execSQL("ALTER TABLE local_files ADD COLUMN sizeBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_files ADD COLUMN dateModified INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifications ADD COLUMN thumbnailUrl TEXT")
                db.execSQL("ALTER TABLE notifications ADD COLUMN linkedUrl TEXT")
                db.execSQL("ALTER TABLE imported_media ADD COLUMN categoryName TEXT NOT NULL DEFAULT 'IMPORTED'")
                db.execSQL("ALTER TABLE imported_media ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'ALL'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reel_likes` (`reelId` TEXT NOT NULL, `likedAt` INTEGER NOT NULL, PRIMARY KEY(`reelId`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rahma_riad_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
